# Diplus 集成技术方案

## Context

当前项目通过 autoservice FID 读取车辆数据，已知 FID 约 50 个，但油量百分比、续航里程、瞬时能耗、发动机/电机转速等关键数据的 FID 未知且无法通过反编译获取。车上已安装 Diplus（迪加）应用，它封装了 BYD SDK 并暴露了本地 HTTP API（端口 8988），可作为补充数据源。

**核心约束**：Diplus 必须在车机上处于运行状态才能响应 API 请求，因此不适合作为自动轮询数据源，需要用户手动触发。

---

## 一、架构设计

### 数据流

```
用户点击「Diplus 刷新」按钮
  → ADB shell: curl http://localhost:8988/api/getDiPars
  → 解析 JSON 响应
  → 将可用字段写入 VehicleStatus
  → 触发 UI 刷新（与现有 onStatusUpdated 回调一致）
```

### 与现有体系的关系

```
┌──────────── 自动轮询（现有） ────────────┐
│ BYD SDK 反射 → autoservice FID → Helper  │
│ 间隔：1~30秒，自动启停                    │
└───────────────────────────────────────────┘
             ↓ 合并到同一个 VehicleStatus ↓
┌──────────── 手动触发（新增） ────────────┐
│ Diplus HTTP API (localhost:8988)          │
│ 用户手动点击按钮触发，一次性读取           │
│ 补充 FID 无法获取的字段                   │
└───────────────────────────────────────────┘
```

Diplus 数据**不替换**现有 FID 数据，只补充 FID 读不到的字段（`VehicleStatus` 中值仍为 `-1` 的字段）。

---

## 二、Diplus API 规格

### 端点

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/getVal?name=<中文名>&status=true` | GET | 查询单个传感器，返回 `{"val": <数值>}` |
| `/api/getDiPars` | GET | 批量查询所有传感器，返回完整 JSON |

- **地址**：`http://localhost:8988`（车机内部访问）
- **通过 ADB 调用**：`adb shell "curl -s 'http://localhost:8988/api/getDiPars'"`
- **响应时间**：通常 < 100ms
- **前提**：Diplus 应用必须在车机上运行中

### 传感器名称（中文）

> 以下为社区已知列表，需在实车上通过 `getDiPars` 返回结果确认

**高价值（FID 未覆盖）：**

| 中文名 | 含义 | 缩放因子 | 映射到 VehicleStatus 字段 |
|--------|------|---------|--------------------------|
| `油量百分比` | 燃油百分比 | 1 | `fuelPercent` |
| `续航里程` | 总续航 | 1 | `totalRange` |
| `纯电续航里程` | 纯电续航 | 1 | `evMileage` |
| `瞬时电耗` | 当前电耗 | 1 | `currentElecConsumption` |
| `瞬时油耗` | 当前油耗 | 1 | `currentFuelConsumption` |
| `平均电耗` | 平均电耗 | 1 | `avgElecConsumption` |
| `平均油耗` | 平均油耗 | 1 | `avgFuelConsumption` |
| `发动机转速` | 发动机 RPM | 1 | 新增字段 `engineRpm` |
| `电机转速` | 电机 RPM | 1 | 新增字段 `motorRpm` |
| `方向角` | 方向盘角度 | 1 | 新增字段 `steeringAngle` |
| `行驶时间` | 行驶时长 | 1 | `tripTime` |
| `钥匙电量` | 遥控钥匙电量 | 1 | 新增字段 `keyBattery` |
| `水温` | 发动机水温 | 1 | 新增字段 `waterTemp` |

**验证用（FID 已覆盖，可交叉对比）：**

| 中文名 | 含义 | 缩放因子 | 映射到 VehicleStatus 字段 |
|--------|------|---------|--------------------------|
| `电量百分比` | SOC | 1 | `batteryPercent` |
| `车速` | 车速 | 1 | `speed` |
| `里程` | 总里程 | 0.1 | `totalMileage` |
| `车外温度` | 环境温度 | 1 | `outsideTemp` |
| `左前胎压` / `右前胎压` / `左后胎压` / `右后胎压` | 胎压 | 0.01 | `tirePressureFL/FR/RL/RR` |

---

## 三、实现方案

### 3.1 新增文件

**`api/DiplusClient.java`**（已创建）

核心方法：
- `testConnection()` — 连接测试（检查进程、端口、基础传感器）
- `fetchAllSensors()` — 获取完整传感器列表（诊断用）
- `readVehicleData()` — **新增**，读取 Diplus 数据并填充到 VehicleStatus

```java
/**
 * 通过 Diplus API 读取车辆数据，填充到 VehicleStatus。
 * 仅覆盖仍为默认值（-1）的字段，不覆盖 FID 已成功读取的字段。
 *
 * @param status 要填充的 VehicleStatus 对象
 * @return 成功读取的传感器数量，-1 表示连接失败
 */
public static int fillVehicleStatus(VehicleStatus status)
```

实现逻辑：
1. 通过 `AdbHelper.getSharedDadb()` 执行 `curl -s http://localhost:8988/api/getDiPars`
2. 解析 JSON 响应
3. 对每个已知传感器名称，提取值 × 缩放因子
4. 仅在 VehicleStatus 对应字段为 `-1`（未填充）时写入
5. 返回成功读取的数量

### 3.2 修改文件

**`api/BydVehicleManager.java`**

新增方法：

```java
/**
 * 手动触发一次 Diplus 数据读取，补充现有 VehicleStatus。
 * 在后台线程执行，完成后通过 listener 回调 UI。
 *
 * @param callback 结果回调（主线程），参数为成功读取的传感器数量，-1 表示失败
 */
public void refreshFromDiplus(java.util.function.IntConsumer callback)
```

实现逻辑：
1. 在 `pollExecutor` 上执行
2. 先调用 `readCurrentStatus()` 获取当前状态（含 FID 数据）
3. 调用 `DiplusClient.fillVehicleStatus(status)` 补充 Diplus 数据
4. 通过 `handler.post()` 回调 `listener.onStatusUpdated(status)` + `callback`

**`ui/StatusPage.java`**

在状态页面顶部（或合适位置）添加一个「Diplus 刷新」按钮：
- 点击 → Toast "正在读取 Diplus 数据..." → 调用 `vm.refreshFromDiplus(count -> ...)`
- 成功 → Toast "已更新 N 个传感器"
- 失败 → Toast "Diplus 未响应，请确认迪加已启动"

**`res/layout/page_status.xml`**

在适当位置添加刷新按钮的 XML 布局。

**`model/VehicleStatus.java`**（可选）

如需显示 Diplus 独有数据（发动机转速、电机转速、方向角、钥匙电量、水温），新增对应字段：

```java
public int engineRpm = -1;
public int motorRpm = -1;
public double steeringAngle = -1;
public int keyBattery = -1;
public int waterTemp = -1;
```

及对应的 display-text 方法。

---

## 四、交互流程

### 用户视角

1. 启动车辆，打开迪加(Diplus)应用
2. 打开本桌面软件，正常显示 FID 数据（自动轮询）
3. 部分字段显示 "—"（FID 未覆盖的数据）
4. 点击「Diplus 刷新」按钮
5. 显示 Toast "正在读取 Diplus 数据..."
6. 1~2 秒后，之前显示 "—" 的字段被填充真实值
7. 显示 Toast "已更新 12 个传感器"

### 异常情况

| 情况 | 表现 | 处理 |
|------|------|------|
| Diplus 未安装 | 连接失败 | Toast 提示 "Diplus 未响应" |
| Diplus 未启动 | 端口不可达 | Toast 提示 "请先启动迪加应用" |
| ADB 未连接 | dadb 为 null | Toast 提示 "请先连接 ADB" |
| 传感器名不匹配 | 返回空值 | 跳过该字段，不影响其他 |

---

## 五、数据合并策略

**原则：FID 优先，Diplus 补充**

```
if (status.fuelPercent < 0 && diplusData.contains("油量百分比")) {
    status.fuelPercent = diplusData.getInt("油量百分比");
}
```

对于 FID 和 Diplus 都能提供的字段（如电量百分比、车速），以 FID 为准（实时性更好），Diplus 仅在 FID 值为 `-1` 时作为降级。

---

## 六、验证步骤

1. 在设置页点击「🔌 Diplus 连接测试」→ 确认 API 可达
2. 在设置页点击「📋 Diplus 传感器扫描」→ 记录所有可用传感器名称
3. 根据实际返回的传感器名称，调整 `DiplusClient` 中的映射表
4. 在状态页点击「Diplus 刷新」→ 确认之前显示 "—" 的字段被正确填充
5. 对比 Diplus 和 FID 的重叠字段（电量、车速、里程），验证数据一致性

---

## 七、文件清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 已创建 | `api/DiplusClient.java` | Diplus HTTP API 客户端 |
| 已修改 | `res/layout/page_settings.xml` | 两个诊断按钮 |
| 已修改 | `ui/SettingsPage.java` | 两个诊断按钮的点击事件 |
| 待修改 | `api/DiplusClient.java` | 新增 `fillVehicleStatus()` |
| 待修改 | `api/BydVehicleManager.java` | 新增 `refreshFromDiplus()` |
| 待修改 | `ui/StatusPage.java` | 添加 Diplus 刷新按钮 |
| 待修改 | `res/layout/page_status.xml` | 刷新按钮 XML |
| 待修改 | `model/VehicleStatus.java` | 新增 Diplus 独有字段（可选） |

---

## 八、风险与限制

1. **Diplus 版本差异**：不同版本的 Diplus 可能调整 API 路径或传感器名称，需定期验证
2. **车型差异**：传感器名称可能因车型不同而有差异（如纯电车无"油量百分比"）
3. **非实时性**：手动触发模式下数据不会自动更新，适合停车/充电场景查看
4. **依赖第三方**：Diplus 是第三方应用，如果停止维护或被 BYD OTA 封锁则此方案失效
5. **curl 可用性**：车机上需要有 `curl` 或 `wget` 命令，部分精简系统可能没有，需降级为 `cat < /dev/tcp/127.0.0.1/8988` 方式
