# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Install

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

adb connect 192.168.10.10:5555                    # WiFi 连接车机（固定 IP）
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s AndroidRuntime:E                    # 查看崩溃日志
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png  # 截图调试
adb shell cmd package set-home-activity com.bydlauncher/.MainActivity
```

无测试套件。无 lint 配置。验证改动靠构建 + 安装到设备/模拟器手动测试。

## Architecture

APP 名称：**迪UI**。单 Activity（`MainActivity`）+ 多 View 页面切换架构，目标设备是 BYD 12-13 英寸横屏车机。设计参考 Kinex Launcher 和 BYD 原生 DiLink 界面。

### 屏幕三层结构

```
┌─ TopBar (28dp) ─────────────────────────────────────────────┐
├─ FrameLayout content_frame ─────────────────────────────────┤
│  4 个标签页 View（显隐切换）：                               │
│  page_status / page_controls / page_apps / page_settings    │
├─ NavBar (52dp, 三段式) ──────────────────────────────────────┤
│  [❄AC温度±🌀] | [🔄循环 🪟车窗 💺座椅] | [主页/控制/应用/设置] │
└──────────────────────────────────────────────────────────────┘

FrameLayout 根容器还叠加了：
  overlay_mask    — 全屏半透明遮罩（#80000000）
  panel_window    — 车窗控制面板（activity 内嵌，非 Dialog）
  panel_seat      — 座椅控制面板（activity 内嵌，非 Dialog）
```

**关键架构决策**：弹窗面板不使用 Android Dialog（Launcher 进程的 Dialog.FLAG_DIM_BEHIND 作用在壁纸层，无法遮罩 APP 内容），改为在 activity_main.xml 的根 FrameLayout 中直接叠加面板 View + 遮罩层，通过 `NavBar.setPanels()` 注入引用后控制显隐。

### UI 类职责（`ui/` 包）

| 类 | 职责 |
|----|------|
| `StatusPage` | Status 仪表盘：速度/挡位/电量/油量/胎压/能耗/行程 |
| `NavBar` | 底部导航栏：标签切换 + AC 快捷 + 弹窗面板管理 |
| `WindowPanelController` | 车窗面板逻辑（静态绑定，绑定 panel_window_control 的 View） |
| `SeatPanelController` | 座椅面板逻辑（静态绑定，绑定 panel_seat_control 的 View） |
| `ControlsPage` | Controls 标签页：完整空调控制 + 车身控制 |
| `VehicleDiagramView` | 自定义 View，Canvas 绘制车辆俯视图 + 车门高亮 |
| `MusicCardView` | MediaSession 读取系统正在播放的媒体 |
| `SettingsPage` | 主题/时钟格式/温度单位/胎压单位/默认桌面 |

`WindowControlDialog` 和 `SeatControlDialog` 是废弃的旧 Dialog 实现，保留但不再使用，可以删除。

### API 层

```
BydVehicleManager（单例，2 秒轮询）
 ├── BydAcApi         → BYDAutoAcDevice（空调）
 ├── BydBodyworkApi   → BYDAutoBodyworkDevice（车身/锁/车门）
 ├── BydStatisticApi  → BYDAutoStatisticDevice（里程/能耗）
 ├── BydTireApi       → BYDAutoTyreDevice（胎压/胎温，尝试多个候选类名）
 ├── BydDriveApi      → BYDAutoGearboxDevice 等（速度/挡位，尝试多个候选类名）
 └── BydDoorLockApi   → BYDAutoDoorLockDevice
```

- `BydApiExplorer`：真车调试工具类，在 logcat 中扫描并列出所有可用的 BYD 系统类，用于发现未记录的 API
- `BydTireApi` / `BydDriveApi`：尝试多个候选类名（不同车型/固件版本类名不同），成功加载哪个就用哪个
- **模拟模式**：非 BYD 设备上 `Class.forName` 失败 → `simulation = true` → 返回硬编码数据，所有控制操作只修改内存状态
- `BydPermissionContext`（ContextWrapper）：拦截 `BYDAUTO_*` 权限检查，使非系统签名 APK 可访问 API

### 主题系统

深色 OLED + Glassmorphism 风格。`ThemeManager` 管理浅/深切换，通过 `AppCompatDelegate.setDefaultNightMode()` + Android 原生 `values/colors.xml` + `values-night/colors.xml` 实现。`SettingsPage` 额外持久化时钟格式、温度单位、胎压单位到 SharedPreferences（key 见 `SettingsPage` 的静态常量），供其他页面通过静态方法读取。

## Key Conventions

- **颜色命名**：语义化（`accent`、`bg_surface`、`text_primary`），两套主题同名不同值。禁止使用旧名如 `accent_blue`、`bg_card_pressed`、`border_subtle`
- **布局命名**：`page_*.xml` 标签页、`panel_*.xml` 内嵌弹窗面板、`dialog_*.xml` 旧 Dialog（废弃）、`card_*.xml` 卡片、`view_*.xml` 固定栏
- **面板 vs Dialog**：新增弹窗功能必须用 `panel_*.xml` + Activity 内嵌方式，不用 Android Dialog
- **targetSdk 28**：刻意不升级，避免 BYD 车机权限问题
- **ProGuard**：保留 `android.hardware.bydauto.**` 和 `com.bydlauncher.api.**`

## BYD API Notes

- 车机 ADB WiFi IP：`192.168.10.10:5555`；USB 安装目录：`Third Party Apps 55`，密码 `BYD6125F`
- 约 30 天自动清除第三方应用权限，需重装
- 返回值 65535 = 功能不可用，-10011 = 未注册
- `BydApiExplorer.explore(context)` 可在真车 logcat 中列出所有可用 API 类（用于发现车窗/座椅控制的 featureId）
- 详细真车接入步骤见 `docs/真车API接入指南.md`

## Changelog

每次代码修改的摘要记录，按日期归档在 `changelog/` 目录下，便于追溯历史改动。

**规则**：
- 目录结构：`changelog/YYYYMMDD.md`，每天一个文件
- 同一天多次修改时，在文件内按时间分隔，格式为 `## HH:MM 修改标题`，新的追加在文件末尾
- 每次修改完代码后必须写入 changelog，包含：涉及文件数和行数变化、按文件分组列出每项修复/改动的简要说明
