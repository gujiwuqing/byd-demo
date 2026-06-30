# 迪UI 车辆 API 增强设计文档

**日期**: 2026-06-30
**范围**: P0 + P1 + P2（全部优先级）
**目标车型**: DiLink 3.0 硬件 / 2406 固件（4.0 UI），架构预留多车型扩展
**核心目标**: 准确获取真实车辆数据（速度/挡位/胎压/车窗/车门/续航/电池温度/电芯电压等），完善 ADB 授权机制

---

## 总体架构：渐进增强（方案 A）

保留现有反射架构，逐层叠加能力：

```
层级 1: 反射调用 BYDAutoXxxDevice（现有，保留）
层级 2: Listener 回调 + 自适应轮询（新增，优化性能）
层级 3: service call autoservice 补充通道（新增，获取高级数据）
层级 4: 轻量 Socket 守护进程代理（新增，特权操作）
```

数据通道最终优先级：

```
优先级 1: Listener 回调（车门/车窗/锁/空调状态变化）
优先级 2: Socket 守护进程批量读取（高频轮询数据）
优先级 3: 反射调用 BYDAutoXxxDevice（守护进程不可用时）
优先级 4: ADB shell service call（反射也失败时的最终降级）
```

---

## 模块一：自动检测车机环境 + 智能模式切换

### 问题

`BydVehicleManager.forceSimMode` 默认 `true`，即使在真车上也默认模拟模式，需要用户手动切换。

### 设计

启动时自动判断环境：

```
App 启动
  ↓
Step 1: Class.forName("android.hardware.bydauto.ac.BYDAutoAcDevice")
  → 成功 → 可能在 BYD 车机
  → 失败 → 确定不在车机 → 模拟模式，结束
  ↓
Step 2: 尝试 getInstance() + 调用一个只读方法（如 getAcStartState）
  → 返回有效值（非 65535、非 -10011）→ 真车模式，API 可用
  → 返回哨兵值或异常 → 在车机但权限不足 → 触发 ADB 授权流程
```

检测结果枚举：`REAL_DEVICE` / `PERMISSION_NEEDED` / `SIMULATOR`

### 改动文件

| 文件 | 改动 |
|------|------|
| 新增 `api/BydEnvironmentDetector.java` | 封装检测逻辑，返回三种状态 |
| `api/BydVehicleManager.java` | 去掉 `forceSimMode` 默认 `true`，启动时调用 `BydEnvironmentDetector.detect()` |
| `MainActivity.java` | 根据检测结果决定是否弹出 ADB 授权对话框 |
| `ui/SettingsPage.java` | 保留手动切换开关，默认值由自动检测决定 |

---

## 模块二：ADB 授权增强

### 问题

- 授权成功后没有验证权限是否真的生效
- 开机自启动场景 ADB 就绪等待不足（仅 3×2s = 6s）
- 授权失败没有分类反馈

### 设计

#### 2.1 授权后验证闭环

```
ADB pm grant 执行完毕
  ↓
等待 500ms（让系统刷新权限缓存）
  ↓
调用 BydPermissionHelper.hasAllPermissions() 验证
  → 全部通过 → 重建 VehicleManager → 调用一个只读 API 确认数据可读 → 成功
  → 部分失败 → 显示具体失败权限列表 + 原因（signature 级别无法 pm grant）
  → 全部失败 → 提示用户检查 ADB 是否可用
```

#### 2.2 开机 ADB 就绪等待增强

```
现有: 3 次重试，间隔 2 秒（共 6 秒）
改为: 5 次重试，间隔递增（2s → 3s → 5s → 8s → 10s），共 28 秒
```

#### 2.3 授权结果分类

- **成功** — 正常授权
- **signature 级别** — `pm grant` 报 "not a changeable permission type"，提示用户该权限需要平台签名，非致命
- **其他错误** — 网络/ADB 断开等，提示重试

### 改动文件

| 文件 | 改动 |
|------|------|
| `api/AdbHelper.java` | `grantPermissions` 回调增加失败分类信息 |
| `MainActivity.java` | 授权后增加验证步骤，开机重试参数调整（5次递增间隔） |
| `api/BydPermissionHelper.java` | 新增 `getGrantablePermissions()` 方法，区分 signature 和 runtime 权限 |

---

## 模块三：Listener 回调模式

### 问题

所有数据都靠 2 秒轮询，车门/车窗/锁车这类低频状态变化也在轮询，浪费资源。

### 设计

#### 3.1 数据分类

| 数据类型 | 方式 | 原因 |
|----------|------|------|
| 车门开关 | **Listener** | 状态变化不频繁，事件更及时 |
| 车窗状态 | **Listener** | 同上 |
| 锁车状态 | **Listener** | 同上 |
| 空调开关/温度 | **Listener** | 用户操作触发 |
| 速度/挡位 | **保持轮询** | 高频连续数据 |
| 胎压/电量/里程 | **保持轮询** | 变化缓慢 |

#### 3.2 实现方式

通过反射注册 BYD 原生 Listener：

```
反射加载 AbsBYDAutoBodyworkListener
  ↓
动态代理/匿名子类注册到 BYDAutoBodyworkDevice.registerListener()
  ↓
状态变化 → 回调触发 → 直接更新 VehicleStatus 对应字段
  ↓
通知 UI 刷新
```

Listener 注册失败时静默降级回轮询。

### 改动文件

| 文件 | 改动 |
|------|------|
| 新增 `api/BydListenerManager.java` | 统一管理所有 Listener 的注册/注销/降级 |
| `api/BydBodyworkApi.java` | 增加 `registerStateListener()` |
| `api/BydAcApi.java` | 增加 `registerAcListener()` |
| `api/BydVehicleManager.java` | Listener 回调直接更新 VehicleStatus，轮询只负责剩余数据 |

---

## 模块四：自适应轮询

### 问题

固定 2 秒轮询，行驶时不够快，停车/熄火时浪费资源。

### 设计

#### 4.1 状态机

| 状态 | 判定条件 | 轮询间隔 |
|------|----------|----------|
| DRIVING | speed > 0 | 1 秒 |
| PARKED | speed = 0 且 gear = P 或 N | 5 秒 |
| CHARGING | 充电枪已连接 | 5 秒 |
| IDLE | 无法读取数据或 ACC OFF | 30 秒 |

#### 4.2 状态判定逻辑

```
每次轮询读取数据后:
  if (充电枪状态 != 0) → CHARGING
  else if (读取失败 / API 不可用) → IDLE
  else if (speed > 0) → DRIVING
  else → PARKED
```

#### 4.3 退避机制

连续 N 次读取返回无效数据（65535 / -10011）时，间隔按 1.5 倍递增，上限 60 秒。恢复有效数据后立即回到正常间隔。

### 改动文件

| 文件 | 改动 |
|------|------|
| 新增 `api/PollState.java` | 枚举 DRIVING/PARKED/CHARGING/IDLE + 各状态间隔常量 |
| `api/BydVehicleManager.java` | pollRunnable 中每次读取后调用 classifyState()，动态设置 postDelayed 间隔 |

---

## 模块五：service call autoservice 备用数据通道

### 问题

反射调用只能获取 BYDAutoXxxDevice 暴露的方法，高级数据（电池温度、电芯电压等）拿不到。

### 设计

#### 5.1 双通道架构

```
BydVehicleManager.readCurrentStatus()
  ↓
通道 1（主）: 反射调用 BYDAutoXxxDevice
  → 成功 → 使用反射数据
  → 失败/权限阻断 → 标记该 API 为 fallback
  ↓
通道 2（补充/降级）: service call autoservice
  → 补充反射拿不到的数据
  → 替代被权限阻断的反射 API
```

#### 5.2 执行方式

复用 AdbHelper，新增批量执行：

```
批量: adb shell "service call autoservice 5 i32 1014 i32 1246777400; ..."
```

一次连接读多个参数，减少 ADB 协议开销。

#### 5.3 新增数据项（BYDMate FID 映射表）

> **注意**：以下 FID 值来自 BYDMate 项目（DiLink 5.0 / 豹3），你的车是 DiLink 3.0 硬件 / 2406 固件，部分 FID 可能不同。模块七的 API 探测工具可用于在真车上验证和发现正确的 FID。首次接入时应逐项验证，确认返回值有效（非哨兵值）后再纳入正式轮询。

| 数据 | Device | FID | 类型 |
|------|--------|-----|------|
| 电池最高温度 | 1014 | 1148190752 | int (-40偏移) |
| 电池最低温度 | 1014 | 1148190736 | int (-40偏移) |
| 电芯最大电压 | 1014 | 1147142192 | int (mV) |
| 电芯最小电压 | 1014 | 1147142160 | int (mV) |
| 电机牵引功率 | 1012 | 339738656 | int (kW) |
| 12V 蓄电池电压 | 1001 | 1128267816 | float (V) |
| 充电枪状态 | 1009 | 876609586 | int (enum) |
| 电源状态(ACC) | 1023 | 315621408 | int (enum) |
| 驾驶模式 | 1006 | 555745294 | int (enum) |
| SoH 健康度 | 1014 | 1145045032 | int (%) |

#### 5.4 Parcel 解析

`service call` 返回格式：`Result: Parcel(00000000 0000005b '....[...')`

哨兵值过滤：
- `0x0000FFFF` (65535) = CAN 链路未建立
- `0x000FFFFF` (1048575) = 未初始化
- `0xFFFFD8E5` (-10011) = FID 不可写
- `-1.0f` = float 未初始化

### 改动文件

| 文件 | 改动 |
|------|------|
| 新增 `api/AutoserviceClient.java` | service call 执行 + Parcel 解析 + 哨兵值过滤 |
| 新增 `api/FidRegistry.java` | Device + FID 常量映射表 |
| `api/AdbHelper.java` | 新增 `execShellBatch()` 批量执行 |
| `model/VehicleStatus.java` | 新增字段：batteryTempMax/Min, cellVoltageMax/Min, voltage12v, chargeGunState, powerState, driveMode, soh |
| `api/BydVehicleManager.java` | readCurrentStatus() 中调用 AutoserviceClient 补充数据 |
| `ui/StatusPage.java` | 展示新增数据项 |

---

## 模块六：轻量 Socket 守护进程

### 问题

每次通过 AdbHelper 执行 service call 都要走完整 ADB 协议，频繁调用开销大。

### 设计

#### 6.1 架构

```
迪UI App (普通 uid)
    ↕ Unix Domain Socket (/data/local/tmp/bydui_helper.sock)
守护进程 (shell uid, 通过 ADB 启动)
    ↕ 直接执行 service call
autoservice Binder → CAN 总线
```

#### 6.2 守护进程实现

用 `app_process` 启动极简 Java 进程（复用 APK 自身代码）：

```
启动命令:
CLASSPATH=/data/app/com.bydlauncher-xxx/base.apk \
  nohup app_process /system/bin \
  com.bydlauncher.helper.HelperDaemon &
```

职责：
1. 监听 Unix Domain Socket
2. 接收 JSON 请求：`{"cmd": "getInt", "dev": 1014, "fid": 1246777400}`
3. 执行 `service call autoservice` 并返回结果
4. 文件锁 `/data/local/tmp/bydui_helper.lock` 保证单实例
5. 空闲 10 分钟无请求自动退出

#### 6.3 安全控制

写操作白名单：

| deviceType | 设备 | 允许写入 |
|------------|------|----------|
| 1000 | 空调 | ✅ |
| 1001 | 车身（车窗/灯光） | ✅ |
| 1041 | 门锁 | ✅ |

禁止写入（安全相关，只读）：

| deviceType | 设备 |
|------------|------|
| 1004, 1006, 1007 | 引擎/驾驶模式 |
| 1009, 1011, 1012, 1013 | 充电/变速箱/电机/速度 |
| 1014, 1016, 1023, 1032 | 电池/胎压/电源 |

请求来源校验：通过 `SO_PEERCRED` 获取调用方 UID，只接受迪UI App 的 UID。

#### 6.4 App 侧

- 自动管理守护进程生命周期：检测 Socket 不可用 → 通过 AdbHelper 启动 → 重新连接
- 失败时降级回直接 ADB shell

### 改动文件

| 文件 | 改动 |
|------|------|
| 新增 `helper/HelperDaemon.java` | 守护进程主类（Socket 服务端 + service call 执行 + 安全控制） |
| 新增 `helper/HelperClient.java` | App 侧 Socket 客户端 |
| 新增 `helper/WriteAllowlist.java` | 写操作白名单常量 |
| `api/BydVehicleManager.java` | 整合四级数据通道优先级 |
| `api/AdbHelper.java` | 新增 `startHelperDaemon()` |

---

## 模块七：API 探测工具增强

### 问题

现有 `BydApiExplorer` 只输出可用类名到 logcat，缺少 featureId 扫描和交互式探测。

### 设计

#### 7.1 增强能力

**a) featureId 范围扫描**

对每个已知 deviceType，通过守护进程或 ADB shell 扫描 featureId 范围，过滤掉返回 -10011 的无效 ID，记录有效结果。

**b) 方法签名枚举**

反射枚举每个 BYDAutoXxxDevice 的所有公开方法，记录名称、参数类型、返回类型。

**c) 无参 getter 自动调用**

自动调用所有无参 getter 并记录返回值。

#### 7.2 结果输出

- Logcat（`BydApiExplorer` tag）
- 本地文件（`/sdcard/bydui_probe_<timestamp>.txt`）

#### 7.3 触发方式

`SettingsPage` 中新增"API 探测"按钮，显示扫描进度，结束后 Toast 提示文件路径。

### 改动文件

| 文件 | 改动 |
|------|------|
| `api/BydApiExplorer.java` | 增加 scanFeatureIds()、enumerateMethods()、callGetters()、文件输出 |
| `ui/SettingsPage.java` | 新增"API 探测"按钮和进度显示 |

---

## 新增文件汇总

| 文件路径 | 职责 |
|----------|------|
| `api/BydEnvironmentDetector.java` | 车机环境自动检测 |
| `api/PollState.java` | 轮询状态机枚举 |
| `api/AutoserviceClient.java` | service call 执行 + Parcel 解析 |
| `api/FidRegistry.java` | Device + FID 常量映射表 |
| `api/BydListenerManager.java` | Listener 统一管理 |
| `helper/HelperDaemon.java` | Socket 守护进程 |
| `helper/HelperClient.java` | App 侧 Socket 客户端 |
| `helper/WriteAllowlist.java` | 写操作安全白名单 |

## 修改文件汇总

| 文件路径 | 改动概要 |
|----------|----------|
| `api/BydVehicleManager.java` | 环境检测集成、自适应轮询、Listener 集成、四级数据通道 |
| `api/AdbHelper.java` | 批量 shell 执行、守护进程启动、授权分类 |
| `api/BydPermissionHelper.java` | 权限分类（signature vs runtime） |
| `api/BydBodyworkApi.java` | Listener 注册 |
| `api/BydAcApi.java` | Listener 注册 |
| `api/BydApiExplorer.java` | featureId 扫描、方法枚举、文件输出 |
| `model/VehicleStatus.java` | 新增字段（电池温度/电芯电压/12V电压/充电枪/电源状态/驾驶模式/SoH） |
| `MainActivity.java` | 环境检测集成、授权验证闭环、开机等待增强 |
| `ui/StatusPage.java` | 展示新增数据项 |
| `ui/SettingsPage.java` | API 探测按钮、默认值由检测决定 |
