# BYD Launcher

比亚迪车机第三方桌面启动器，适配 DiLink 3.0（21款宋PLUS DM-i）。

## 功能

- **基础桌面**：时钟、日期、应用列表（4列网格）
- **车辆状态**：电池电量、纯电续航、总里程、电源状态、室外温度、车门状态、锁车状态
- **空调控制**：开关、温度调节（17-33°C）、风量（0-7级）、出风模式、循环模式、自动/手动
- **快捷开关**：车锁状态、后备箱、空调开关、循环模式切换

## 技术架构

```
┌─────────────────────────────────────────────┐
│                MainActivity                  │
│  (Launcher Activity, CATEGORY_HOME)          │
├─────────────────────────────────────────────┤
│         BydVehicleManager (单例)             │
│    ┌──────────┬──────────┬──────────┐        │
│    │ BydAcApi │BodyworkApi│StatApi  │        │
│    └────┬─────┴────┬─────┴────┬────┘        │
│         │          │          │              │
│    ReflectionHelper (反射调用)               │
│         │                                    │
│    BydPermissionContext (权限绕过)           │
├─────────────────────────────────────────────┤
│  android.hardware.bydauto.* (framework.jar)  │
│         ↓                                    │
│    DiCarServer → CAN Bus → 车辆 ECU         │
└─────────────────────────────────────────────┘
```

## API 访问方式

通过反射访问 `android.hardware.bydauto.*` 命名空间的系统类：

- `BYDAutoAcDevice` — 空调控制（设备类型 1000）
- `BYDAutoBodyworkDevice` — 车身状态
- `BYDAutoStatisticDevice` — 行驶统计
- `BYDAutoDoorLockDevice` — 门锁状态

权限绕过通过 `BydPermissionContext`（ContextWrapper）实现，拦截 `BYDAUTO_*` 权限检查。

## 编译

```bash
# 需要 Android SDK, API 25+, Build Tools
cd byd/
./gradlew assembleDebug
# APK 输出: app/build/outputs/apk/debug/app-debug.apk
```

## 安装到车机

### 方式一：ADB WiFi
```bash
adb connect 192.168.10.10:5555
adb install -r app-debug.apk
```

### 方式二：USB 安装
1. U盘中创建 `Third Party Apps 55` 文件夹
2. 将 APK 放入该文件夹
3. 插入车机 USB 口
4. 输入密码 `BYD6125F`

## 开启 ADB 调试（2407+ 固件）

1. 手机蓝牙连接车机
2. 车机拨号界面输入 `*#91532547#*`
3. 记录显示的 IMEI
4. 访问 https://ahmada3mar.github.io/BYD/ 生成密码
5. 输入密码启用 ADB

## 设为默认桌面

安装后点击 Home 键，系统会询问使用哪个桌面，选择 "BYD Launcher" 并设为默认。

也可通过 ADB 设置：
```bash
adb shell cmd package set-home-activity com.bydlauncher/.MainActivity
```

## 注意事项

- 车机约 30 天会自动清除第三方应用权限，需重新安装或授权
- 全景摄像头（Panorama）API 权限检查在服务端执行，`BydPermissionContext` 无法绕过
- 部分 API 在不同车型上返回值可能不同（65535 = 不可用, -10011 = 未注册）
- 建议先测试读取接口，确认返回有效数据后再开发控制功能

## 参考资料

- [wheregoes/byd-apps](https://github.com/wheregoes/byd-apps) — 社区 API 文档
- [ahmada3mar/BYD](https://github.com/ahmada3mar/BYD) — ADB 开启指南
- [Kinex Launcher](https://kinex.lexwah.com/) — 参考实现
