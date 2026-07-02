# autoservice Feature ID 参考手册

**维护日期**: 2026-07-02
**目标车型**: BYD 宋PLUS DM-i 21款（DiLink 3.0, msm8953, Android 10）
**数据来源**: BYDMate FidMap + wheregoes/byd-apps API Reference + 车机实测

> 本文档持续维护。每次在车机上跑「FID 全量扫描」后，根据 ✓/✗ 结果更新状态列。
> 对应代码文件：`app/src/main/java/com/diui/launcher/api/FidRegistry.java`

---

## 一、概念说明

### 什么是 FID

FID（Feature ID）是 BYD autoservice 系统服务的信号地址，对应 CAN 总线上的一个数据点。每个 FID 绑定一个设备类型（dev）和一个事务码（tx）。

### 读取方式

```bash
# tx=5 读 int
service call autoservice 5 i32 <dev> i32 <fid>

# tx=7 读 float
service call autoservice 7 i32 <dev> i32 <fid>
```

返回格式：`Result: Parcel(XXXXXXXX YYYYYYYY '....')`
- 前 8 位 hex = 异常码（`00000000` = 正常）
- 后 8 位 hex = 返回值（int 直接读，float 按 IEEE754 解析）

### 哨兵值

| 值 | hex | 含义 | 处理 |
|----|-----|------|------|
| 65535 | 0x0000FFFF | 无 CAN 信号（车型不支持） | 跳过 |
| 1048575 | 0x000FFFFF | 未初始化 | 跳过 |
| -10013 | 0xFFFFD8E3 | 错误事务码（tx 不对或 dev 不存在） | 换 tx 或 dev |
| -10011 | 0xFFFFD8E5 | FID 未注册（此车型无此信号） | 换 FID |

### 常见坑

- **int vs float**：同一个 FID 用错 tx 会返回 -10013。例如 SOC 必须用 tx=7（float），用 tx=5 会报错
- **不同车型 FID 不同**：海豚/海豹/宋Plus 的 FID 可能不同，需各自验证
- **温度偏移**：电池温度需 raw - 40 才是摄氏度（CAN 总线通用偏移）
- **里程缩放**：里程值需 × 0.1 才是公里数

---

## 二、设备类型列表

| 常量名 | dev | BYD API 类 | 说明 |
|--------|-----|------------|------|
| DEV_AC | 1000 | BYDAutoAcDevice | 空调系统 |
| DEV_BODYWORK | 1001 | BYDAutoBodyworkDevice | 车身（门/窗/电压） |
| DEV_POWER_2 | 1002 | — | 电源辅助 |
| DEV_LIGHT | 1004 | BYDAutoLightDevice | 灯光 |
| DEV_DRIVE_MODE | 1006 | BYDAutoEnergyDevice | 驱动/能量模式 |
| DEV_SAFETY | 1007 | — | 安全（安全带等） |
| DEV_CHARGE | 1009 | — | 充电系统 |
| DEV_GEARBOX | 1011 | — | 变速箱 |
| DEV_MOTOR | 1012 | — | 电机 |
| DEV_SPEED | 1013 | — | 速度 |
| DEV_STATISTIC | 1014 | BYDAutoStatisticDevice | 统计/电池/BMS |
| DEV_TIRE | 1016 | BYDAutoTyreDevice | 轮胎 |
| DEV_POWER | 1023 | BYDAutoPowerDevice | 电源管理 |
| DEV_LOCK | 1032 | — | 门锁 |
| DEV_DOOR_LOCK | 1041 | BYDAutoDoorLockDevice | 门锁（旧接口） |

---

## 三、FID 完整列表

### AC 空调（dev=1000）

| 常量名 | FID | tx | 说明 | 取值范围 | 21款宋Plus |
|--------|-----|----|------|----------|-----------|
| FID_AC_STATE | 1077936144 | 5(int) | AC 开关 | 0=关 1=开 | ✓ |
| FID_AC_CYCLE | 1077936148 | 5(int) | 循环模式 | 0=内循环 1=外循环 | ✓ 待验证 |
| FID_AC_FAN | 1077936156 | 5(int) | 风量等级 | 0~7 | ✓ 待验证 |
| FID_AC_TEMP | 1077936168 | 5(int) | 设定温度 | 17~33°C | ✓ |
| FID_OUTSIDE_TEMP | 1077936184 | 5(int) | 车外温度 | °C | ✓ |
| FID_CABIN_TEMP | 1031798832 | 5(int) | 车内温度 | °C | ✓ 待验证 |

### 车身 Bodywork（dev=1001）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_DOOR_FL | 692060168 | 5(int) | 左前门 | 0=关 1=开 | ✓ |
| FID_DOOR_FR | 692060170 | 5(int) | 右前门 | 0=关 1=开 | ✓ |
| FID_DOOR_RL | 692060172 | 5(int) | 左后门 | 0=关 1=开 | ✓ |
| FID_DOOR_RR | 692060174 | 5(int) | 右后门 | 0=关 1=开 | ✓ |
| FID_HOOD | 692060188 | 5(int) | 引擎盖 | 0=关 1=开 | 待验证 |
| FID_TRUNK | 1074790416 | 5(int) | 后备箱 | 0=关 1=开 | 待验证 |
| FID_WINDOW_FL | 947912728 | 5(int) | 左前窗 | 开度% | ✓ |
| FID_WINDOW_FR | 1267728400 | 5(int) | 右前窗 | 开度% | ✓ |
| FID_WINDOW_RL | 947912736 | 5(int) | 左后窗 | 开度% | ✓ |
| FID_WINDOW_RR | 947912752 | 5(int) | 右后窗 | 开度% | ✗ -10011 |
| FID_12V_VOLTAGE | 1128267816 | 7(float) | 12V 电压 | V | ✓ 13.7 |

### 统计/电池（dev=1014）

| 常量名 | FID | tx | 说明 | 取值/缩放 | 21款宋Plus |
|--------|-----|----|------|-----------|-----------|
| FID_SOC | 1246777400 | **7(float)** | 电量 SOC | 百分比 | ✓ 待验证 |
| FID_SOH | 1145045032 | 5(int) | 电池健康度 | 百分比 | ✓ 100 |
| FID_MILEAGE | 1246765072 | 5(int) | 总里程 | ×0.1=km | ✓ |
| FID_BATT_TEMP_MAX | 1148190752 | 5(int) | 电池最高温 | raw-40=°C | ✓ |
| FID_BATT_TEMP_MIN | 1148190736 | 5(int) | 电池最低温 | raw-40=°C | ✓ |
| FID_CELL_VOLT_MAX | 1147142192 | 5(int) | 电芯最高压 | ×0.001=V | ✓ |
| FID_CELL_VOLT_MIN | 1147142160 | 5(int) | 电芯最低压 | ×0.001=V | ✓ |
| FID_TOTAL_ELEC_CON | 1032871984 | **7(float)** | 累计能耗 | kWh | 待验证 |

### 速度（dev=1013）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_SPEED | -1807745016 | **7(float)** | 车速 | km/h | 待验证 |

### 变速箱（dev=1011）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_GEAR | 555745336 | 5(int) | 挡位 | 0=P 1=R 2=N 3=D | ✓ |

### 电机（dev=1012）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_MOTOR_POWER | 339738656 | 5(int) | 电机功率 | kW（正=耗电 负=回收） | ✓ |

### 充电（dev=1009）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_CHARGE_GUN | 876609586 | 5(int) | 充电枪状态 | 1=无 2=AC 3=DC | ✓ |
| FID_CHARGING_TYPE | 876609592 | 5(int) | 充电类型 | 1=默认 2=AC 3=VTOG | 待验证 |
| FID_CHARGING_BMS_STATE | 876609560 | 5(int) | BMS 充电状态 | 1=充电中 2=完成 13=暂停 | 待验证 |
| FID_CHARGING_CAPACITY | 666894360 | 7(float) | 本次充电量 | kWh | 待验证 |

### 电源管理（dev=1023）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_POWER_STATE | 315621408 | 5(int) | 电源状态 | 0/1/2 | ✓ |

### 驱动模式（dev=1006）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_DRIVE_MODE | 555745294 | 5(int) | 驱动模式 | | ✓ |
| FID_WORK_MODE | 874512420 | 5(int) | EV/HEV 工作模式 | | 待验证 |

### 灯光（dev=1004）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_LIGHT_LOW | 950009866 | 5(int) | 近光灯 | 0=关 1=开 | 待验证 |
| FID_DRL | 1231040528 | 5(int) | 日间行车灯 | 0=关 1=开 | 待验证 |

### 安全（dev=1007）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_SEATBELT_FL | 692060184 | 5(int) | 主驾安全带 | 0=未系 1=已系 | 待验证 |

### 门锁（dev=1032）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_LOCK_FL | 1081081864 | 5(int) | 左前门锁 | 0=未锁 1=已锁 | 待验证 |

### 门锁 CAN 特征 ID（dev=1041，byd-apps 反向工程）

> 来源：wheregoes/byd-apps manager scan，在 Dolphin DiLink 3.0 上发现
> 注意：主门锁 `getDoorLockStatus()` 对 area 1~5 返回 INVALID(0)，仅儿童锁可读

| 常量名 | FID | hex | tx | Dolphin 值 | 说明 |
|--------|-----|-----|-----|-----------|------|
| FID_DOORLOCK_ONLINE | 1101004800 | 0x41A00000 | 5(int) | 1 | 在线/已连接 |
| FID_DOORLOCK_STATUS_1 | 1101004808 | 0x41A00008 | 5(int) | 255 (0xFF) | 锁状态位掩码 |
| FID_DOORLOCK_STATUS_2 | 1101004816 | 0x41A00010 | 5(int) | 255 (0xFF) | 锁状态位掩码 |
| FID_DOORLOCK_UNK_18 | 1101004824 | 0x41A00018 | 5(int) | 65535 | N/A |
| FID_DOORLOCK_UNK_20 | 1101004832 | 0x41A00020 | 5(int) | 0 | 未知 |
| FID_DOORLOCK_COUNT | 1101004844 | 0x41A0002C | 5(int) | 5 | 可能=车门数量 |
| FID_DOORLOCK_UNK_30 | 1101004848 | 0x41A00030 | 5(int) | 65535 | N/A |

### 轮胎（dev=1016）

| 常量名 | FID | tx | 说明 | 取值 | 21款宋Plus |
|--------|-----|----|------|------|-----------|
| FID_TIRE_FL | -1728052956 | 5(int) | 左前胎压 | kPa | ✓ 235 |
| FID_TIRE_FR | -1728052952 | 5(int) | 右前胎压 | kPa | ✓ |
| FID_TIRE_RL | -1728052948 | 5(int) | 左后胎压 | kPa | ✓ |
| FID_TIRE_RR | -1728052944 | 5(int) | 右后胎压 | kPa | ✓ |

---

## 四、未知 FID（待发现）

以下数据在 `BYDAutoStatisticDevice`（dev=1014）上有对应 API 方法，但 FID 未知：

| BYD API 方法 | 数据含义 | 发现方法 |
|-------------|----------|----------|
| `getSpeedSignalVDisValue()` | 仪表盘速度（可能与 FID_SPEED 不同） | 反编译 framework |
| `getEVMileageValue()` | EV 纯电里程 | 反编译 framework |
| `getHEVMileageValue()` | HEV 混动里程 | 反编译 framework |
| `getElecDrivingRangeValue()` | 纯电续航里程 | 反编译 framework |
| `getFuelDrivingRangeValue()` | 燃油续航里程 | 反编译 framework |
| `getFuelPercentageValue()` | 油量百分比 | 反编译 framework |
| `getElecPercentageValue()` | 电量百分比 | 可能=FID_SOC |
| `getInstantFuelConValue()` | 瞬时油耗 | 反编译 framework |
| `getInstantElecConValue()` | 瞬时电耗 | 反编译 framework |
| `getAverageFuelConsumption(int)` | 平均油耗 | 反编译 framework |
| `getAverageElectricConsumption(int)` | 平均电耗 | 反编译 framework |
| `getDrivingTimeValue()` | 行驶时间 | 反编译 framework |
| `getKeyBatteryLevel()` | 钥匙电量 | 反编译 framework |
| `getWaterTemperature()` | 水温 | 反编译 framework |

### 发现方法

1. **反编译 framework.jar**：从车机 `/system/framework/` 拉取 BYD 框架 jar，用 jadx/dex2jar 反编译，从每个 getter 方法的字节码中提取 FID 常量
2. **FID 范围扫描**：围绕已知 FID 附近（±100）的值逐个探测，找到返回有效数据的
3. **社区共享**：其他宋Plus DMi 车主分享的 FID 数据

---

## 五、更新日志

### 2026-07-02

**byd-apps API Reference 整合**

- 新增 WFID_AC_FAN = 501219340（0x1DE0000C），风量写入 FID（Dolphin 实测可用，命名方法 setAcWindLevel 不可用）
- 新增 7 个门锁 CAN 特征 ID（dev=1041），来自 byd-apps manager scan
- 记录 byd-apps 发现：BYDAutoDoorLockDevice 主门锁 area 1~5 返回 INVALID(0)，仅儿童锁可读
- 记录 byd-apps 发现：BYDAutoPanoramaDevice 权限绕过在服务端被拦截，第三方无法访问全景摄像头

### 2026-07-01

**初始版本**

- 从 BYDMate FidMap.kt 提取 40+ 个 FID
- 实测验证 21 款宋Plus DMi：
  - AC 开关/温度/车外温度 ✓
  - 车门 x4 ✓，车窗 x3 ✓（右后 ✗）
  - 胎压 x4 ✓
  - SOH ✓，挡位 ✓
  - 12V 电压 ✓，充电枪 ✓，电源状态 ✓
- 修复 3 个事务码错误：
  - SOC：tx=5→tx=7（float）
  - 速度：tx=5→tx=7（float）
  - 风量：FID 0x1DE0000C → 1077936156

---

## 六、验证流程

每次更新 FID 后的验证步骤：

1. 修改 `FidRegistry.java` 中的常量
2. 如有新字段，在 `AutoserviceClient.java` 加读取方法
3. 在 `BydVehicleManager.fillFromAutoserviceShell()` 中使用新数据
4. 构建安装到车机
5. 设置页 → 「📡 FID 全量扫描」→ 确认 ✓/✗
6. 设置页 → 「🚗 真车模式诊断」→ 确认 service call 返回值
7. 更新本文档对应行的「21款宋Plus」列

---

## 七、写入 FID（控制命令）

> 来源：BYDMate LIVE_VALIDATED（Leopard 3 实车验证）
> 写入命令：`service call autoservice 6 i32 <dev> i32 <fid> i32 <value>`
> **注意：写入 FID 与读取 FID 完全不同！**

### AC 空调写入（dev=1000）

| 常量名 | FID | value 含义 | 验证状态 |
|--------|-----|-----------|---------|
| WFID_AC_ON | 501219352 | 0=开 | ⚠ 未验证 |
| WFID_AC_OFF | 501219364 | 1=关 | ✓ |
| WFID_AC_TEMP | 501219368 | 16~30°C | ✓ |
| WFID_AC_CYCLE | 501219355 | 0=外循环 1=内循环 | ✓ |
| WFID_AC_DEFROST_REAR | 501219357 | 0=关 1=开后除雾 | ✓ |
| WFID_AC_FAN | 501219340 | 0~7 风量 | ✓ Dolphin 实测（byd-apps: 命名方法 setAcWindLevel 不可用，需通过 base set() 调用） |

### 车窗写入（dev=1001）— 百分比 0~100

| 常量名 | FID | 说明 | 验证状态 |
|--------|-----|------|---------|
| WFID_WINDOW_FL | 1276219408 | 左前窗开度% | ✓ |
| WFID_WINDOW_FR | 1276219424 | 右前窗开度% | ✓ |
| WFID_WINDOW_RL | 1276219416 | 左后窗开度% | ✓ |
| WFID_WINDOW_RR | 1276219432 | 右后窗开度% | ✓ |

### 门锁写入（dev=1001）

| 常量名 | FID | value 含义 | 验证状态 |
|--------|-----|-----------|---------|
| WFID_DOOR_LOCK | 1276141590 | 1=解锁 2=上锁 | ✓ |

### 天窗写入（dev=1001）

| 常量名 | FID | value 含义 | 验证状态 |
|--------|-----|-----------|---------|
| WFID_SUNROOF | 1125122056 | 1=开 2=关 3=倾 4=停 5=翻转 6=舒适 | ✓ |
| WFID_SUNSHADE | 1125122060 | 1=开 2=关 | ✓ |

### 前备箱写入（dev=1001）

| 常量名 | FID | value 含义 | 验证状态 |
|--------|-----|-----------|---------|
| WFID_FRONT_TRUNK | 1276182560 | 1=开 3=关 | ✓ |

### 座椅加热/通风（dev=1000）

| 常量名 | FID | 说明 | 验证状态 |
|--------|-----|------|---------|
| WFID_SEAT_HEAT_DR_SW | 1276248084 | 主驾加热开关 1=开 2=关 | ✓ |
| WFID_SEAT_HEAT_DR_LV | 1276252180 | 主驾加热档位 1~5 | ✓ |
| WFID_SEAT_HEAT_PA_SW | 1276248092 | 副驾加热开关 | ✓ |
| WFID_SEAT_HEAT_PA_LV | 1276252188 | 副驾加热档位 | ✓ |
| WFID_SEAT_VENT_DR_SW | 1276248080 | 主驾通风开关 | ✓ |
| WFID_SEAT_VENT_DR_LV | 1276252176 | 主驾通风档位 | ✓ |
| WFID_SEAT_VENT_PA_SW | 1276248088 | 副驾通风开关 | ✓ |
| WFID_SEAT_VENT_PA_LV | 1276252184 | 副驾通风档位 | ✓ |

### 灯光写入（dev=1023/1004，特殊豁免）

| 常量名 | FID | value | 说明 |
|--------|-----|-------|------|
| WFID_INTERIOR_LIGHT | 1330643002 | 1=关 2=开 | 室内灯（dev=1023） |
| WFID_AMBIENT_LIGHT | 1069547536 | 1=关 2~5=亮度 | 氛围灯（dev=1023） |

---

## 八、Content Provider 数据源

部分数据通过 Android Content Provider 读取，**无需特殊权限**，ADB shell 直接访问。

### CarStatusProvider（已验证可用）

URI: `content://com.byd.carStatusProvider/car_status`

```bash
content query --uri content://com.byd.carStatusProvider/car_status
```

| key | 说明 | 示例值 |
|-----|------|--------|
| `car_status_maintenance_time` | 距下次保养天数 | 238 |
| `car_status_maintenance_mile` | 距下次保养公里数 | 4988 |
| `set_car_status_maintenance_time` | 保养周期(天) | 360 |
| `set_car_status_maintenance_mile` | 保养周期(km) | 7500 |
| `car_status_issue_num` | 当前故障数量 | 0 |
| `car_status_issue` | 故障列表 | (空) |
| `travel_points_fuel` | 油耗曲线（#分隔，单位Wh/10km每段） | 0#0#6#11#13... |
| `travel_points_elec` | 电耗曲线（#分隔，负值=回收） | 177#173#-5#-23... |
| `car_status_maintenance_hev_mile` | HEV混动里程保养 | |

### CarSettingsProvider（路径未知，待探测）

URI: `content://com.byd.providers.carsettings`
包含 TravelInfoHandler，可能有续航/油量数据，尚未找到正确路径。

### 无效的 Provider（宋Plus DMi 不存在）

- `com.gpack.service.provider.VehicleServiceProvider` ✗
- `com.byd.car.server.provider.CarServiceProvider` ✗

---

## 九、写入 FID 在 21款宋Plus DMi 上的验证状态

> BYDMate 的 LIVE_VALIDATED 在 Leopard 3（2026款豹3）上验证，宋Plus DMi 尚未逐一确认。
> 装机后按以下步骤验证：

```bash
# 验证车窗写入（应能控制左前窗）
service call autoservice 6 i32 1001 i32 1276219408 i32 50
# 读回确认
service call autoservice 5 i32 1001 i32 947912728
```

| 功能 | 验证状态（宋Plus DMi） |
|------|---------------------|
| 空调开关 | 待验证 |
| 空调温度 | 待验证 |
| 车窗开度 | 待验证 |
| 门锁 | 待验证 |
| 座椅加热/通风 | 待验证 |

