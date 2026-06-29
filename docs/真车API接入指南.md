# 迪UI 真车 API 接入指南

**文档日期**: 2026-06-28  
**目标车型**: BYD 宋PLUS DM-i（DiLink 3.0）  
**目标**: 将迪UI从模拟模式升级为真实车辆数据接入

---

## 一、连接真车 ADB

### 前提条件
1. 手机/电脑和车机连接**同一个 WiFi 热点**（用手机开热点，车机连接该热点）
2. 已在车机上开启 ADB 调试（参考下方步骤）

### 开启车机 ADB（2407+ 固件）
```
1. 手机蓝牙连接车机
2. 车机拨号界面输入：*#91532547#*
3. 记录显示的 IMEI 号
4. 访问 https://ahmada3mar.github.io/BYD/ 生成密码
5. 输入密码启用 ADB
```

### 连接命令
```bash
# WiFi 连接（推荐）
adb connect 192.168.10.10:5555

# 验证连接
adb devices

# 预期输出
# 192.168.10.10:5555    device
```

### USB 安装 APK（备用方式）
```
1. U盘创建文件夹：Third Party Apps 55
2. 将 app-debug.apk 放入该文件夹
3. 插入车机 USB 口安装
4. 密码：BYD6125F
```

---

## 二、安装迪UI到真车

```bash
# 构建 Debug APK
cd /Users/feng/work/byd
./gradlew assembleDebug

# 安装到真车
adb -s 192.168.10.10:5555 install -r app/build/outputs/apk/debug/app-debug.apk

# 设为默认桌面（可选）
adb -s 192.168.10.10:5555 shell cmd package set-home-activity com.bydlauncher/.MainActivity
```

---

## 三、验证现有 API 是否生效

安装后在车机上打开迪UI，同时在电脑运行：

```bash
# 实时查看 API 调用日志
adb -s 192.168.10.10:5555 logcat -s BydVehicleManager:I BydAcApi:I BydBodyworkApi:I BydStatisticApi:I BydReflection:W

# 预期看到（非模拟模式）：
# I BydVehicleManager: Initialized - AC:true Body:true Stat:true Lock:true
# 如果看到 simulation mode 说明 API 未加载成功
```

---

## 四、待接入的真实 API

### 4.1 速度 + 挡位（BYDAutoGearboxDevice）

**类名**: `android.hardware.bydauto.gearbox.BYDAutoGearboxDevice`

需要在代码中新增 `BydGearboxApi.java`：

```java
// 方法名（待验证）
getGearPosition()        // 返回 0=P, 1=R, 2=N, 3=D
getVehicleSpeed()        // 返回 km/h（整数或浮点）
getEngineSpeed()         // 转速 RPM
```

**验证命令**（安装后运行 API 探测 APK）：
```bash
adb -s 192.168.10.10:5555 shell dumpsys activity com.bydlauncher
```

### 4.2 胎压 + 胎温（BYDAutoTyreDevice）

**类名**: `android.hardware.bydauto.tyre.BYDAutoTyreDevice`

```java
// 方法名（待验证）
getTyrePressure(int area)   // area: 1=左前, 2=右前, 3=左后, 4=右后
getTyreTemp(int area)       // 对应胎温
```

返回值单位：`kPa * 10` 或 `PSI * 10`，需要在真车上确认换算关系。

### 4.3 空气质量（BYDAutoPm25Device）

**类名**: `android.hardware.bydauto.pm25.BYDAutoPm25Device`

```java
// 方法名（待验证）
getOutPm25Value()    // 室外 PM2.5 数值
getInPm25Value()     // 室内 PM2.5 数值
getAqiLevel()        // AQI 等级
```

### 4.4 实时功率（BYDAutoEnergyDevice）

**类名**: `android.hardware.bydauto.energy.BYDAutoEnergyDevice`

```java
// 方法名（待验证）
getMotorPower()          // 电机功率 kW
getRegenPower()          // 回收功率 kW
getBatteryVoltage()      // 电池电压
getBatteryCurrent()      // 电池电流
```

---

## 五、探测车窗/座椅控制 FeatureId

BYD 的底层控制通过 `set(deviceType, featureId, value)` 方法实现。
空调风量的 featureId 已知为 `0x1DE0000C`，车窗/座椅需要在真车上探测。

### 探测步骤

#### 方法一：抓取系统日志（推荐）

在车机上手动操作车窗，同时抓取系统日志：

```bash
# 清除旧日志
adb -s 192.168.10.10:5555 logcat -c

# 在车机上操作：手动打开/关闭车窗

# 抓取日志（过滤 BYD 相关）
adb -s 192.168.10.10:5555 logcat | grep -i "byd\|bodywork\|window\|feature\|0x1"
```

#### 方法二：运行 FeatureId 扫描工具

在 `BydBodyworkApi.java` 中临时加入以下探测代码，安装后查看 logcat：

```java
// 临时探测代码 - 找到 featureId 后删除
public void probeWindowFeatureIds() {
    if (device == null) return;
    for (int featureId = 0x1DE00001; featureId <= 0x1DE000FF; featureId++) {
        try {
            int result = ReflectionHelper.setViaBaseClass(device, DEVICE_TYPE, featureId, 0);
            if (result >= 0) {
                Log.i("BYD_PROBE", "Found featureId: 0x" + Integer.toHexString(featureId) + " result=" + result);
            }
        } catch (Exception e) {
            // 跳过无效 featureId
        }
    }
}
```

---

## 六、接入后需修改的文件

| 文件 | 改动内容 |
|------|----------|
| `api/BydGearboxApi.java` | 新建：速度+挡位 API |
| `api/BydTyreApi.java` | 新建：胎压+胎温 API |
| `api/BydPm25Api.java` | 新建：空气质量 API |
| `api/BydEnergyApi.java` | 新建：实时功率 API |
| `api/BydBodyworkApi.java` | 新增：车窗控制 setWindowState() |
| `api/BydVehicleManager.java` | 注册新 API + readCurrentStatus() 补充 |
| `model/VehicleStatus.java` | 已完善，字段已预留 |
| `ui/StatusPage.java` | 解除模拟数据，接入真实值 |
| `ui/WindowControlDialog.java` | 接入真实 setWindowState() |
| `AndroidManifest.xml` | 新增权限声明 |

### 需要新增的权限（AndroidManifest.xml）

```xml
<uses-permission android:name="android.permission.BYDAUTO_GEARBOX_GET" />
<uses-permission android:name="android.permission.BYDAUTO_TYRE_GET" />
<uses-permission android:name="android.permission.BYDAUTO_SPEED_GET" />
```

> 注：`BYDAUTO_ENERGY_GET` 和 `BYDAUTO_PM2P5_GET` 已在 Manifest 中声明。

---

## 七、验证清单

真车测试时逐项确认：

- [ ] adb 成功连接 `192.168.10.10:5555`
- [ ] 安装迪UI后 logcat 显示 `AC:true Body:true`（非模拟模式）
- [ ] 空调开关在车机上实际生效
- [ ] 温度调节实际生效（车内温度变化）
- [ ] 锁车按钮实际生效
- [ ] 电量/续航显示与仪表盘一致
- [ ] 速度在行驶时实时更新（接入 GearboxApi 后）
- [ ] 胎压与仪表盘数值一致（接入 TyreApi 后）
- [ ] 车窗控制实际控制车窗升降

---

## 八、常见问题

**Q: logcat 显示 simulation mode，真车上也这样？**  
A: 说明 `Class.forName("android.hardware.bydauto.ac.BYDAutoAcDevice")` 失败，可能是权限问题。检查 `BydPermissionContext` 是否正确包裹 context。

**Q: API 返回 -10011？**  
A: 未注册到 BYD 服务，车机重启后重试，或检查权限声明。

**Q: API 返回 65535？**  
A: 该功能在当前车型不支持。

**Q: 约 30 天后功能失效？**  
A: BYD 车机会定期清除第三方应用权限，需重新安装 APK。

---

## 九、参考资料

- [wheregoes/byd-apps](https://github.com/wheregoes/byd-apps) — 社区 API 文档（featureId 参考）
- [ahmada3mar/BYD](https://github.com/ahmada3mar/BYD) — ADB 开启工具
- [Kinex Launcher](https://kinex.lexwah.com/) — 功能参考实现
- BYD DiLink 3.0 SDK 文档（车厂内部，社区整理版见 wheregoes）
