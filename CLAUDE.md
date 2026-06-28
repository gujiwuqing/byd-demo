# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Install

```bash
./gradlew assembleDebug                          # 构建 debug APK
# APK 输出: app/build/outputs/apk/debug/app-debug.apk

adb install -r app/build/outputs/apk/debug/app-debug.apk  # 安装到设备
adb connect 192.168.10.10:5555                   # WiFi 连接车机
adb logcat -s AndroidRuntime:E                   # 查看崩溃日志
adb shell cmd package set-home-activity com.bydlauncher/.MainActivity  # 设为默认桌面
```

无测试套件。无 lint 配置。验证改动靠构建 + 安装到设备/模拟器手动测试。

## Architecture

单 Activity（`MainActivity`）+ 多 View 页面切换架构，目标设备是 BYD 12-13 英寸横屏车机。参考 Kinex Launcher 设计。

### UI 层（三层屏幕结构）

```
┌─ TopBar (32dp) ─────────────────────────────────┐
├─ Content FrameLayout ───────────────────────────┤
│  5 个 View 页面通过 NavBar 标签切换（显隐控制）：  │
│  StatusPage / MapPage / ControlsPage /           │
│  AppsPage / SettingsPage                         │
├─ NavBar (56dp, 三段式) ─────────────────────────┤
│  [App shortcuts] | [5 tab buttons] | [AC temp±] │
└──────────────────────────────────────────────────┘
```

每个页面是独立的 Java 类（在 `ui/` 包下），持有自己的 View 引用并处理自身逻辑。MainActivity 只负责协调：初始化、标签切换、转发 VehicleStatus 更新到各页面。

### API 层（全部通过反射访问 BYD 系统类）

```
BydVehicleManager (单例, 2秒轮询)
 ├── BydAcApi         → android.hardware.bydauto.ac.BYDAutoAcDevice
 ├── BydBodyworkApi   → android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice
 ├── BydStatisticApi  → android.hardware.bydauto.statistic.BYDAutoStatisticDevice
 └── BydDoorLockApi   → android.hardware.bydauto.doorlock.BYDAutoDoorLockDevice
```

- `ReflectionHelper` 统一处理 `Class.forName` + `Method.invoke`，所有调用都有 try-catch
- `BydPermissionContext`（ContextWrapper）拦截 `BYDAUTO_*` 权限检查，使非系统签名 APK 也能访问 API
- **模拟模式**：非 BYD 设备上 `Class.forName` 失败时，各 API 类自动进入模拟模式（`simulation = true`），返回硬编码数据并维护可交互的内存状态

### 主题系统

`ThemeManager` 管理浅色/深色切换，使用 Android 原生 `values/colors.xml` + `values-night/colors.xml` 双套颜色资源，通过 `AppCompatDelegate.setDefaultNightMode()` 切换。主题名 `AppTheme` 继承自 `Theme.MaterialComponents.DayNight.NoActionBar`。

## Key Conventions

- **颜色命名**：使用语义化名称（`accent`、`bg_surface`、`text_primary`、`border`），不使用具体色值名（如 ~~`accent_blue`~~、~~`bg_card`~~）。两套主题中同名颜色不同值。
- **布局文件命名**：`page_*.xml`（标签页）、`card_*.xml`（卡片组件）、`view_*.xml`（固定栏）、`item_*.xml`（列表项）
- **ProGuard**：保留 `android.hardware.bydauto.**` 和 `com.bydlauncher.api.**`，反射调用的类不能被混淆
- **targetSdk 28**：刻意不升级到 29+，避免 BYD 车机上的权限和存储兼容问题
- `VehicleDiagramView` 是自定义 View，用 Canvas 在 `onDraw` 中绘制车辆俯视图，不依赖外部图片资源

## BYD API Notes

- 车机约 30 天自动清除第三方应用权限
- 返回值 65535 = 功能不可用，-10011 = 未注册
- 全景摄像头 API 权限检查在服务端，`BydPermissionContext` 无法绕过
- USB 安装需放入 `Third Party Apps 55` 文件夹，密码 `BYD6125F`
