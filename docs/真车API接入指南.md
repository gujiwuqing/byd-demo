# 迪UI 真车 API 接入指南

**文档日期**: 2026-07-01
**目标车型**: BYD 宋PLUS DM-i（DiLink 3.0, msm8953, Android 10）

---

## 一、整体架构

### 数据通道

在 21 款宋Plus DMi 上，所有 BYDAUTO 权限为 signature 级别，BYD API 服务端校验权限（返回 `permission deny!`），`app_process` 被 SELinux 阻止。因此数据获取的唯一通道是：

```
App → dadb (ADB shell, uid=2000)
    → service call autoservice <tx> i32 <dev> i32 <fid>
    → 解析 Parcel 返回值
```

### 轮询流程

```
pollExecutor (后台线程)
  → readCurrentStatus()
    ① BYD 反射 API（仅 isRealDevice()=true 的才读）
    ② fillFromAutoserviceShell() — 通过 dadb shell 读取真实数据
    ③ fillExtrasFromAutoservice() — 电池/车辆扩展数据
    ④ 仅无 ADB 时填模拟数据
  → handler.post → UI 更新 (主线程)
```

---

## 二、ADB 连接

### 自连接机制

```java
AdbHelper.findAdbHost(context)
  → 127.0.0.1 → WifiManager IP → 所有网络接口 IPv4
  → 第一个 TCP 5555 可达的地址
```

**关键修复记录**：
- `isAdbAvailable()` 原在主线程 → `NetworkOnMainThreadException` 被吞 → 改为 `checkAvailableAsync()`
- 轮询原在主线程 → `dadb.shell()` 阻塞/异常 → 改为 `pollExecutor` 后台线程

### 开启车机 ADB

**方法一**：设置 → DiLink → 版本管理 → 快速点击"恢复出厂设置"文字 10 次 → 开启无线 ADB
**方法二**：蓝牙拨号 `*#91532547#*` → 精诚助手扫码 → 开启 ADB

---

## 三、数据来源与 FID 对照

### 3.1 已验证可用的数据（✓）

| 数据 | dev | fid | tx | 实测值 |
|------|-----|-----|----|--------|
| AC 开关 | 1000 | FID_AC_STATE | 5(int) | 0/1 |
| AC 温度 | 1000 | FID_AC_TEMP | 5(int) | 25 |
| 车外温度 | 1000 | FID_OUTSIDE_TEMP | 5(int) | 24 |
| 车门 FL/FR/RL/RR | 1001 | FID_DOOR_* | 5(int) | 0/1 |
| 车窗 FL/FR/RL | 1001 | FID_WINDOW_* | 5(int) | 开度 |
| 12V 电压 | 1001 | FID_12V_VOLTAGE | 7(float) | 13.7 |
| **电量 SOC** | 1014 | FID_SOC | **7(float)** | 67.5 |
| SOH | 1014 | FID_SOH | 5(int) | 100 |
| 里程 | 1014 | FID_MILEAGE | 5(int) | km |
| 电池温度 max/min | 1014 | FID_BATT_TEMP_* | 5(int) | 需 -40 |
| 电芯电压 max/min | 1014 | FID_CELL_VOLT_* | 5(int) | mV |
| 挡位 | 1011 | FID_GEAR | 5(int) | 0/1/2 |
| 胎压 x4 | 1016 | FID_TIRE_* | 5(int) | 235 kPa |
| 充电枪 | 1009 | FID_CHARGE_GUN | 5(int) | 0/1 |
| 电源状态 | 1023 | FID_POWER_STATE | 5(int) | 0/1/2 |
| 驱动模式 | 1006 | FID_DRIVE_MODE | 5(int) | |
| 电机功率 | 1012 | FID_MOTOR_POWER | 5(int) | kW |

### 3.2 FID 不匹配的数据（✗）

| 数据 | 当前 FID | 错误 | 状态 |
|------|----------|------|------|
| 风量 | FID_AC_WIND (0x1DE0000C) | -10011 未注册 | 待发现正确 FID |
| 车窗右后 | FID_WINDOW_RR | -10011 未注册 | 待发现正确 FID |
| 速度 | FID_SPEED on DEV_SPEED(1013) | -10013 设备不存在 | 在 dev=1014 上，FID 未知 |
| 累计能耗 | FID_ACCUM_ENERGY | -10013 | 需 tx=7(float)，已修复待验证 |

### 3.3 **缺失的大量数据（FID 未知）**

`BYDAutoStatisticDevice`（dev=1014）上的这些方法我们**没有对应 FID**：

| BYD API 方法 | 数据含义 | FID 状态 |
|-------------|----------|----------|
| `getSpeedSignalVDisValue()` | 车速 | ❓ 未知 |
| `getEVMileageValue()` | EV 纯电里程 | ❓ 未知 |
| `getHEVMileageValue()` | HEV 混动里程 | ❓ 未知 |
| `getElecDrivingRangeValue()` | 纯电续航 | ❓ 未知 |
| `getFuelDrivingRangeValue()` | 燃油续航 | ❓ 未知 |
| `getFuelPercentageValue()` | 油量百分比 | ❓ 未知 |
| `getElecPercentageValue()` | 电量百分比 | ❓ 未知（可能与 FID_SOC 重复） |
| `getInstantFuelConValue()` | 瞬时油耗 | ❓ 未知 |
| `getInstantElecConValue()` | 瞬时电耗 | ❓ 未知 |
| `getAverageFuelConsumption()` | 平均油耗 | ❓ 未知 |
| `getAverageElectricConsumption()` | 平均电耗 | ❓ 未知 |
| `getDrivingTimeValue()` | 行驶时间 | ❓ 未知 |
| `getFuelADValue()` | 油量 AD 值 | ❓ 未知 |
| `getKeyBatteryLevel()` | 钥匙电量 | ❓ 未知 |
| `getWaterTemperature()` | 水温 | ❓ 未知 |

其他设备类型（`BYDAutoBodyworkDevice` dev=1001）同样有未知 FID：

| BYD API 方法 | 数据含义 | FID 状态 |
|-------------|----------|----------|
| `getBatteryCapacity()` | 电池容量（bodywork 路径） | ❓ 未知 |
| `getPowerLevel()` | 功率等级 | ❓ 未知 |
| `getAutoVIN()` | VIN 号 | ❓ 未知 |

### 3.4 为什么只有部分 FID

FID（Feature ID）是 BYD 私有的 CAN 总线信号地址，嵌入在车机 `/system/framework/` 的 BYD 框架类字节码中。当前 `FidRegistry` 的 32 个 FID 来自：
- BYDMate 项目的逆向结果（主要针对海豹/海豚）
- wheregoes/byd-apps 的探测结果
- 社区积累

**不同车型的 FID 可能不同**（21 款宋Plus DMi vs 海豚 vs 海豹），所以部分 FID 在本车上返回 -10011/-10013。

### 3.5 如何发现更多 FID

1. **从车机框架提取**（最可靠）：通过 ADB 拉取 `/system/framework/` 中的 BYD 框架 jar，反编译提取所有 FID 常量
2. **FID 范围扫描**：在已知设备类型上扫描 FID 范围，找返回有效值的
3. **社区贡献**：如果有同车型用户分享过 FID 数据

---

## 四、权限系统

所有 BYDAUTO 权限均为 signature 级别：
- `pm grant` 无效（返回 `not a changeable permission type`）
- `BydPermissionContext` 仅绕过 `getInstance()` 客户端检查
- 数据方法 IPC 到服务端 → `[getInt] permission deny!`
- 只能通过 ADB shell（uid=2000）执行 `service call autoservice`

---

## 五、app_process / HelperDaemon（不可用）

SELinux `Enforcing` + `u:r:shell:s0` 阻止 `app_process` 运行：
```
Error changing dalvik-cache ownership: permission denied → Aborted (SIGABRT)
ANDROID_DATA=/data/local/tmp → Operation not permitted
```

HelperDaemon 代码保留（其他车型可能可用），当前车型全部通过 `service call` 替代。

---

## 六、诊断工具

设置页提供以下诊断按钮：

| 按钮 | 功能 |
|------|------|
| 🔍 ADB 网络诊断 | 探测 ADB 连接：候选 IP、端口、Dadb 状态 |
| 🚗 真车模式诊断 | API 状态 + service call 实测每个 FID 的返回值 |
| ⚙️ HelperDaemon 诊断 | 逐步启动 daemon，含 SELinux/dmesg 检查 |
| 📡 FID 全量扫描 | 扫描所有已知 FID，标注 ✓/✗ |
| 🔬 API 探测 | 反射扫描所有 BYD API 类的方法和返回值 |

---

## 七、已修复 Bug 记录

| Bug | 原因 | 修复 |
|-----|------|------|
| 点击 ADB 授权始终报"不可用" | 主线程 `Socket.connect()` → `NetworkOnMainThreadException` | `checkAvailableAsync()` |
| 127.0.0.1 不通 | adbd 可能只绑定 Wi-Fi 接口 | `findAdbHost()` 自动探测所有 IP |
| ADB 授权成功但不切真车模式 | 模式切换在 `startHelperDaemon.onStarted` 回调里，daemon 失败则不执行 | 模式切换提前到授权成功后立即执行 |
| 轮询数据全是模拟值 | `readCurrentStatus()` 在主线程，`dadb.shell()` 网络操作被阻塞 | 轮询移到 `pollExecutor` 后台线程 |
| 模拟数据覆盖真实数据 | 模拟值先填入(250)，autoservice shell 后覆盖失败 | 改为 `isRealDevice()` 才读 API，autoservice shell 优先 |
| startHelperDaemon 缺少 appUid | `HelperDaemon.main()` 要求 args[0]=appUid | 命令加 `context.getApplicationInfo().uid` |
| SOC 电量返回 -10013 | FID_SOC 用 tx=5(int)，实际是 tx=7(float) | 改为 `getFloat()` |

---

## 八、参考项目

| 项目 | 数据来源 |
|------|----------|
| [BYDMate](https://github.com/AndyShaman/BYDMate) | 手写 ADB 协议 + `service call autoservice`（与我们方案一致） |
| [wheregoes/byd-apps](https://github.com/wheregoes/byd-apps) | `BydPermissionContext` 直接调用 BYD API（仅客户端校验的车型有效） |
| [ahmada3mar/BYD](https://github.com/ahmada3mar/BYD) | ADB 开启工具/密码生成器 |
