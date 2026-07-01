# 迪UI 真车 API 接入指南

**文档日期**: 2026-07-01
**目标车型**: BYD 宋PLUS DM-i（DiLink 3.0, msm8953, Android 10）
**目标**: 将迪UI从模拟模式升级为真实车辆数据接入

---

## 一、整体架构

迪UI 获取 BYD 真实车况数据有三条路径：

| 路径 | 机制 | 覆盖数据 | 适用场景 |
|------|------|----------|----------|
| **反射 BYD API** | `BydPermissionContext` 绕过客户端权限 → 反射 `BYDAutoAcDevice` 等 | 空调/车窗/车门/胎压/速度/挡位/里程 | 权限仅客户端校验的车型 |
| **ADB shell service call** | `dadb.shell("service call autoservice ...")` 以 shell UID 直接读 autoservice | AC/电量/车门/车窗/速度/胎压/电池温度/SOH | **所有 BYDAUTO 权限为 signature 级别的车型（推荐）** |
| **HelperDaemon** | `app_process` 启动 shell UID 的 binder 守护进程 | 同上 | SELinux 允许 app_process 的车型 |

### 21 款宋Plus DMi 的情况

| 特性 | 状态 |
|------|------|
| BYDAUTO 权限级别 | **全部 signature** — `pm grant` 无效 |
| BydPermissionContext | `getInstance()` 通过，但 IPC 数据读取返回 -1（服务端校验） |
| app_process | **SELinux Enforcing 阻止**（exit -122, SIGABRT） |
| service call autoservice | ✓ 通过 dadb shell（shell UID）可正常读取 |

因此本车型使用**路径 2（ADB shell service call）**作为主要数据通道。

---

## 二、ADB 连接

### 2.1 app 自连接机制

app 运行在车机上，通过 dadb 库自连本机 adbd：

```
AdbHelper.findAdbHost(context)
  → 依次探测 127.0.0.1 → WifiManager 获取的 Wi-Fi IP → 所有网络接口 IPv4
  → 第一个 TCP 5555 端口可达的地址即为 ADB host
  → 缓存到 resolvedHost，后续复用
```

**关键修复**：`isAdbAvailable()` 原本在主线程执行 Socket.connect()，Android 10 抛
`NetworkOnMainThreadException` 被 catch 吞掉后返回 false → 永远显示"本地 ADB 不可用"。
已改为 `checkAvailableAsync()` 在后台线程执行，回调到主线程。

### 2.2 开启车机 ADB

**方法一：快速点击（DiLink 3.0, 2024 年 6 月前固件）**
1. 设置 → DiLink → 版本管理
2. 快速点击"恢复出厂设置"**文字**（不是按钮！）10 次
3. 开启「连接 USB 后启用调试模式」和「无线 ADB 调试开关」

**方法二：暗码 + 扫码（2024 年 6 月后固件）**
1. 蓝牙电话拨号：`*#91532547#*`
2. 屏幕弹出二维码 → 找售后/淘宝用「精诚助手」扫码
3. 开启 ADB 开关

### 2.3 USB 安装 APK
```
1. U盘创建文件夹：Third Party Apps 55
2. 放入 APK
3. 插入车机 USB，密码：BYD6125F
```

---

## 三、数据读取流程

### 3.1 启动时序

```
onCreate
  → BydEnvironmentDetector.detect()  // 检测真车/模拟器
  → BydVehicleManager.getInstance()  // 初始化所有 API
  → checkPermissions()
    → AdbHelper.checkAvailableAsync()  // 异步探测 ADB（后台线程）
    → ADB 可用 → autoAdbGrant() → connectAndAuth()
    → 认证成功 → 切换真车模式 → 重建 VehicleManager → 开始轮询
```

### 3.2 轮询数据流

```
pollRunnable (主线程 Handler)
  → readCurrentStatus()
    ①  BYD 反射 API 尝试读取（AcApi/BodyworkApi/DriveApi...）
        → 大部分返回 -1（signature 权限 IPC 拦截）
    ②  fillFromAutoserviceShell()  ← 核心数据通道
        → 通过 dadb.shell("service call autoservice ...") 读取
        → shell UID 有权限访问 autoservice binder
        → 覆盖 ① 中返回 -1 的字段
    ③  fillExtrasFromHelper/fillExtrasFromAutoservice()
        → 电池温度/SOH/12V 电压等补充数据
  → listener.onStatusUpdated(status)  // UI 刷新
```

### 3.3 service call 命令格式

```bash
# 读 int
service call autoservice 5 i32 <deviceType> i32 <featureId>

# 读 float
service call autoservice 7 i32 <deviceType> i32 <featureId>

# 写 int（HelperDaemon 限用，app 直接调用被安全限制）
service call autoservice 6 i32 <deviceType> i32 <featureId> i32 <value>
```

返回格式：`Result: Parcel(XXXXXXXX YYYYYYYY '....')`
- 前 8 位 hex = 异常码（00000000 = 正常）
- 后 8 位 hex = 返回值

---

## 四、数据来源对照

### 4.1 通过 ADB shell service call 读取的核心数据

| 数据 | dev 常量 | fid 常量 | 说明 |
|------|----------|----------|------|
| AC 开关状态 | DEV_AC (1000) | FID_AC_STATE | 1=开 0=关 |
| AC 设定温度 | DEV_AC | FID_AC_TEMP | >30 时需 /2 |
| 车外温度 | DEV_AC | FID_OUTSIDE_TEMP | 摄氏度 |
| 风量等级 | DEV_AC | FID_AC_WIND | 0~7 |
| 电池电量 | DEV_BATTERY (1014) | FID_SOC | 百分比 |
| 车门状态 | DEV_BODYWORK (1001) | FID_DOOR_FL/FR/RL/RR | 1=开 0=关 |
| 车窗状态 | DEV_BODYWORK | FID_WINDOW_FL/FR/RL/RR | 开度 |
| 速度 | DEV_SPEED (1013) | FID_SPEED | km/h |
| 挡位 | DEV_GEARBOX (1011) | FID_GEAR | |
| 胎压 | DEV_TIRE (1016) | FID_TIRE_FL/FR/RL/RR | kPa |

### 4.2 补充数据（电池/车辆扩展）

| 数据 | dev | fid |
|------|-----|-----|
| 电池温度 max/min | DEV_BATTERY (1014) | FID_BATT_TEMP_MAX/MIN |
| 电芯电压 max/min | 1014 | FID_CELL_VOLT_MAX/MIN |
| SOH | 1014 | FID_SOH |
| 12V 电压 | DEV_BODYWORK (1001) | FID_12V_VOLTAGE (float) |
| 充电枪状态 | DEV_CHARGE (1009) | FID_CHARGE_GUN |
| 电源状态 | DEV_POWER (1023) | FID_POWER_STATE |
| 驱动模式 | DEV_DRIVE_MODE (1006) | FID_DRIVE_MODE |
| 电机功率 | DEV_MOTOR (1012) | FID_MOTOR_POWER |

### 4.3 哨兵值

| 值 | 含义 |
|----|------|
| 0x0000FFFF (65535) | 无 CAN 信号（车型不支持该功能） |
| 0x000FFFFF | 未初始化 |
| 0xFFFFD8E3 (-10013) | 错误事务码 |
| 0xFFFFD8E5 (-10011) | 未注册到 BYD 服务 |

---

## 五、权限系统

### 5.1 三层绕过策略

| 层级 | 机制 | 21款宋Plus DMi 效果 |
|------|------|---------------------|
| **客户端 getInstance** | `BydPermissionContext` 拦截 `enforcePermission` 返回 GRANTED | ✓ 成功获取设备对象 |
| **客户端数据读取** | 同上 | ✗ IPC 到服务端，服务端校验真实权限 → -1 |
| **服务端 autoservice** | `dadb.shell("service call ...")` 以 shell UID 调用 | ✓ shell 有权限 |

### 5.2 pm grant 在本车型的行为

所有 18 个 BYDAUTO 权限均为 signature 级别：
```
pm grant com.diui.launcher android.permission.BYDAUTO_AC_GET
→ Error: not a changeable permission type
```

`pm grant` 对本车型完全无效，数据获取完全依赖 ADB shell service call。

---

## 六、HelperDaemon（备注）

### 6.1 设计目的

通过 `app_process` 以 shell UID 启动 binder 守护进程，注册为 `diui_helper` 系统服务，
app 通过 binder IPC 调用它读写 autoservice。

### 6.2 在 21 款宋Plus DMi 上的状态

**不可用**。`app_process` 被 SELinux 阻止：

```
SELinux: Enforcing
shell context: u:r:shell:s0
app_process 报错: Error changing dalvik-cache ownership: permission denied → Aborted (SIGABRT)
设置 ANDROID_DATA=/data/local/tmp 后: Operation not permitted
```

已改用 `dadb.shell("service call autoservice ...")` 替代，无需 `app_process`。

### 6.3 相关文件（保留，其他车型可能可用）

| 文件 | 职责 |
|------|------|
| `helper/HelperDaemon.java` | shell-uid binder 守护进程 |
| `helper/HelperClient.java` | app 端 binder 客户端 |
| `helper/HelperBinderProtocol.java` | binder 协议常量 |
| `helper/WriteAllowlist.java` | 写操作 dev 白名单（AC/车身/门锁） |

---

## 七、诊断工具

设置页提供三个诊断按钮，便于排查问题：

### 🔍 ADB 网络诊断

检查 ADB 连接状态：
- 缓存 host / Wi-Fi IP
- 所有网络接口枚举
- 127.0.0.1 和各 IP 的 TCP 5555 端口探测
- Dadb 连接状态和 shell 验证

### 🚗 真车模式诊断

检查各 API 初始化状态：
- `forceSimMode` 开关状态
- 每个 API 的 `isRealDevice()` / `isAvailable()`
- BYDAutoAcDevice 类和 getInstance 是否存在
- 实际数据读取结果（AC/Bodywork/Drive）
- Polling 状态和连续失败次数

### ⚙️ HelperDaemon 诊断

逐步启动 daemon 并显示每步结果：
- Dadb 连接验证
- SELinux 模式和 shell context
- app_process 基础测试
- 前台启动 daemon 捕获输出
- dmesg SELinux denied 日志
- binder 服务注册状态

---

## 八、关键代码文件

| 文件 | 职责 |
|------|------|
| `api/AdbHelper.java` | ADB 连接：Wi-Fi IP 自动探测、异步可用性检查、RSA 认证、shell 执行、诊断 |
| `api/AutoserviceClient.java` | 通过 `dadb.shell()` 执行 `service call autoservice` 读取车辆数据 |
| `api/BydVehicleManager.java` | 车辆数据管理：轮询、API 调用、autoservice shell 降级、诊断报告 |
| `api/FidRegistry.java` | autoservice 设备类型和 feature ID 常量表 |
| `api/BydPermissionContext.java` | ContextWrapper，拦截 BYDAUTO 权限检查 |
| `api/BydPermissionHelper.java` | 权限诊断，检查授权状态 |
| `api/BydEnvironmentDetector.java` | 运行环境检测（真车/需权限/模拟器） |
| `api/ReflectionHelper.java` | 反射调用 BYD API |
| `api/BydAcApi.java` | 空调 API（反射路径） |
| `api/BydBodyworkApi.java` | 车身 API（反射路径） |
| `ui/SettingsPage.java` | 设置页：ADB 授权、手动授权脚本、三个诊断按钮 |

---

## 九、已知问题与修复记录

### 主线程 NetworkOnMainThreadException（已修复）

**现象**：点击"ADB 权限授权"始终报"本地 ADB 不可用"
**原因**：`isAdbAvailable()` 在主线程执行 `Socket.connect()`，Android 10 抛异常被 catch 吞掉
**修复**：改为 `checkAvailableAsync(context, callback)` 异步执行

### 连接地址写死 127.0.0.1（已修复）

**现象**：adbd 可能只绑定 Wi-Fi 接口，127.0.0.1 不通
**修复**：`findAdbHost(context)` 自动探测 127.0.0.1 → WifiManager IP → 所有网络接口

### startHelperDaemon 缺少 appUid 参数（已修复）

**现象**：daemon 启动后立即 `System.exit(2)`（args 为空）
**修复**：命令加上 `context.getApplicationInfo().uid`

### onAuthGranted 回调被 HelperDaemon 阻塞（已修复）

**现象**：ADB 授权成功但永远不切换到真车模式
**原因**：模式切换代码放在 `startHelperDaemon` 的 `onStarted` 回调里，daemon 启动失败时回调不执行
**修复**：模式切换提前到授权成功后立即执行，daemon 在后台异步启动

### app_process 被 SELinux 阻止（已确认，已绕过）

**现象**：exit=-122 (SIGABRT)，输出为空
**原因**：SELinux Enforcing + shell context 不允许运行 app_process
**绕过**：改用 `dadb.shell("service call autoservice ...")` 直接读取数据

---

## 十、验证清单

真车测试时逐项确认：

- [ ] 设置页 ADB 网络诊断显示某个 IP:5555 可达
- [ ] ADB 授权弹窗出现并点击"允许"
- [ ] 真车模式诊断显示 `forceSimMode: false`
- [ ] AC startState 返回 0 或 1（非 -1）
- [ ] 电量百分比与仪表盘一致
- [ ] 车门开关状态正确
- [ ] 速度在行驶时实时更新
- [ ] 胎压与仪表盘数值一致（kPa）

---

## 十一、参考资料

- [wheregoes/byd-apps](https://github.com/wheregoes/byd-apps) — BYD API 逆向文档、feature ID 参考
- [AndyShaman/BYDMate](https://github.com/AndyShaman/BYDMate) — autoservice binder 通信参考
- [ahmada3mar/BYD](https://github.com/ahmada3mar/BYD) — ADB 开启工具/密码生成器
