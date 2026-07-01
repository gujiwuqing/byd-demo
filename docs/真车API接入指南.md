# 迪UI 真车 API 接入指南

**文档日期**: 2026-07-01
**目标车型**: BYD 宋PLUS DM-i（DiLink 3.0）
**目标**: 将迪UI从模拟模式升级为真实车辆数据接入

---

## 一、整体架构

迪UI 获取 BYD 真实车况数据有两条路径，都需要 **shell uid**（普通 app 签名拿不到）：

| 路径 | 机制 | 覆盖数据 |
|------|------|----------|
| **反射 BYD API** | `pm grant` 授 `BYDAUTO_*` 权限 → 反射 `BYDAutoAcDevice` 等读数据 | 空调/车窗/车门/胎压/速度/挡位/里程 |
| **autoservice 系统服务** | `service call autoservice <tx> i32 <dev> i32 <fid>` | 电池温度/电压/SOH/充电枪/驱动模式/电机功率 |

获取 shell uid 的方式有两种：

- **app 自动授权**（推荐）：app 内自连 `127.0.0.1:5555`，走 ADB RSA 认证，首次会弹"允许 USB 调试？"
- **甲壳虫手动兜底**：用外部 ADB 工具直接执行 shell 命令，绕过 app 内协议实现

### 关键点：app 自连用的是 `127.0.0.1:5555`

app 跑在车机上，自连的是**本机回环地址** `127.0.0.1:5555`，**不依赖车机的实际 IP**——只要车机 adbd 监听 5555 端口即可。车机 IP 是 192.168.x.x 还是别的，对 app 无影响。

---

## 二、连接真车 ADB

### 前提条件
1. 已在车机上开启 ADB 调试（见下方）
2. adbd 监听 5555 端口（开「无线调试」或 `adb tcpip 5555`）

### 开启车机 ADB（2407+ 固件）
```
1. 手机蓝牙连接车机
2. 车机拨号界面输入：*#91532547#*
3. 记录显示的 IMEI 号
4. 访问 https://ahmada3mar.github.io/BYD/ 生成密码
5. 输入密码启用 ADB
```

### 外部连接（甲壳虫 / PC adb）

外部工具从车机外部连接，需填车机真实 IP（以实际为准，端口 5555）：

```bash
# 替换 <车机IP> 为车机实际 IP
adb connect <车机IP>:5555
adb devices
# 预期：<车机IP>:5555    device
```

### USB 安装 APK（备用）
```
1. U盘创建文件夹：Third Party Apps 55
2. 将 app-debug.apk 放入该文件夹
3. 插入车机 USB 口安装
4. 密码：BYD6125F
```

---

## 三、安装迪UI到真车

```bash
# 构建 Debug APK（本地，不要在车机执行）
./gradlew assembleDebug

# 安装（外部 adb，替换 <车机IP>）
adb -s <车机IP>:5555 install -r app/build/outputs/apk/debug/app-debug.apk

# 设为默认桌面（可选）
adb -s <车机IP>:5555 shell cmd package set-home-activity com.diui.launcher/.MainActivity
```

> 包名：`com.diui.launcher`

---

## 四、app 自动授权（推荐）

安装后打开迪UI，启动时会自动检测：**ADB 可用（5555 监听）且 BYD 权限未全授予** → 自动触发授权流程：

```
启动 → checkPermissions → ADB可用 && 权限缺失
     → startAdbGrant → 临时显示系统 UI（让弹窗可见）
     → AdbHelper: VERSION=0x01000001 + host:: + NONEwithRSA 签名
     → 首次: adbd 弹"允许 USB 调试？" → 用户点允许（勾选始终允许）
     → pm grant 授全部 BYDAUTO_* 权限
     → startHelperDaemon(setsid + poll-loop, binder + uid 鉴权)
     → 切真车模式 → 读真实数据
```

首次授权后，RSA 公钥存入车机 `/data/misc/adb/adb_keys`，后续启动不再弹窗。

### 设置页手动入口

设置 → 车辆 → **ADB 权限授权**：与启动自动流程等价的「再试一次」入口。

### 环境检测说明

`BydEnvironmentDetector.detect()` 判断运行环境：
- `Class.forName("android.hardware.bydauto.ac.BYDAutoAcDevice")` 失败 → SIMULATOR（真模拟器）
- 类存在但调用异常 → PERMISSION_NEEDED（真车，权限/状态问题）
- 调用返回正常值 → REAL_DEVICE

授权触发条件**不依赖**此判断，改为「ADB 可用 + 权限缺失」即触发，避免误判。

---

## 五、甲壳虫手动授权（兜底，最可靠）

当 app 自动授权怎么都不弹窗时，用甲壳虫等外部 ADB 工具手动执行——直接以 shell uid 把活干了，绕过 app 内 ADB 协议实现的所有问题。

### 5.1 一键授权 BYD 权限（核心）

甲壳虫连上车机后，在 shell 里粘贴执行：

```sh
PKG=com.diui.launcher
for p in \
  android.permission.BYDAUTO_AC_GET \
  android.permission.BYDAUTO_AC_SET \
  android.permission.BYDAUTO_AC_COMMON \
  android.permission.BYDAUTO_BODYWORK_GET \
  android.permission.BYDAUTO_BODYWORK_COMMON \
  android.permission.BYDAUTO_DOOR_LOCK_GET \
  android.permission.BYDAUTO_DOOR_LOCK_COMMON \
  android.permission.BYDAUTO_POWER_GET \
  android.permission.BYDAUTO_ENERGY_GET \
  android.permission.BYDAUTO_PM2P5_GET \
  android.permission.BYDAUTO_STATISTIC_GET \
  android.permission.BYDAUTO_GEARBOX_GET \
  android.permission.BYDAUTO_SPEED_GET \
  android.permission.BYDAUTO_CHARGING_GET \
  android.permission.BYDAUTO_TYRE_GET \
  android.permission.BYDAUTO_LIGHT_GET \
  android.permission.BYDAUTO_LIGHT_SET \
  android.permission.BYDAUTO_SETTING_GET; do
  pm grant $PKG $p 2>&1
done
```

> 此脚本也已内置于 app：**设置 → 车辆 → 手动授权脚本**，点击弹窗可一键复制到剪贴板。

执行后**杀掉 app 重新打开**即可进真车模式读 BYD API 数据。

> `pm grant` 报 `not a changeable permission type` 是正常的——那是 signature 级权限，BYD 车机上大部分能授成功，报错的几条不影响其他数据。

### 5.2 手动启动 HelperDaemon（读 autoservice 数据）

pm grant 只解锁 BYD API 反射路径。若还要读 autoservice 系统服务的数据（电池温度/电压/SOH/充电枪/驱动模式），手动启动常驻 daemon：

```sh
PKG=com.diui.launcher
APK=$(pm path $PKG | grep -o '/[^:]*base.apk' | head -1)
APPUID=$(stat -c %u /data/data/$PKG)
CLASSPATH=$APK setsid app_process /system/bin \
  --nice-name=diui_helper com.diui.launcher.helper.HelperDaemon $APPUID \
  </dev/null >/dev/null 2>&1 &
for i in 1 2 3; do service list 2>/dev/null | grep -q diui_helper && break; sleep 1; done
echo "HelperDaemon started, uid=$APPUID"
```

daemon 常驻后，app 通过 binder 自动连接它读 autoservice 数据。

### 5.3 验证

```sh
# 查看授权结果
dumpsys package com.diui.launcher | grep -A 50 "requested permissions:" | grep BYDAUTO

# 查看 HelperDaemon 是否在跑
ps -A | grep diui_helper
```

---

## 六、验证 API 是否生效

安装并授权后，打开迪UI，同时在电脑运行：

```bash
# 替换 <车机IP>
adb -s <车机IP>:5555 logcat -s BydVehicleManager:I BydAcApi:I BydBodyworkApi:I BydReflection:W BydPermissionHelper:I

# 预期（真车模式）：
# I BydVehicleManager: Initialized - AC:true Body:true Stat:true Lock:true Tire:true Drive:true
# 出现 simulation mode 说明 API 未加载成功
```

权限诊断（过滤 `BydPermissionHelper`）会列出每个 BYD 权限的授权状态。

---

## 七、数据来源对照

### 7.1 反射 BYD API（pm grant 解锁后）

| API 类 | 数据 |
|--------|------|
| `BYDAutoAcDevice` | 空调开关/温度/风量/出风/循环/车外温度 |
| `BYDAutoBodyworkDevice` | 电量/车门/车窗/天窗/锁车 |
| `BYDAutoStatisticDevice` | EV 里程/总里程/电耗百分比 |
| `BYDAutoTyreDevice` | 四轮胎压(kPa)/胎温(°C) |
| `BYDAutoGearboxDevice` 等 | 速度/挡位/功率/油耗/续航/行程 |
| `BYDAutoDoorLockDevice` | 门锁状态 |

### 7.2 autoservice 系统服务（HelperDaemon 读取）

通过 `service call autoservice <tx> i32 <dev> i32 <fid>`，device/fid 定义见 `FidRegistry.java`：

| 数据 | dev | fid |
|------|-----|-----|
| 电池温度 max/min | DEV_BATTERY(1014) | FID_BATT_TEMP_MAX/MIN |
| 电芯电压 max/min | 1014 | FID_CELL_VOLT_MAX/MIN |
| SOH | 1014 | FID_SOH |
| 12V 电压 | DEV_BODYWORK(1001) | FID_12V_VOLTAGE |
| 充电枪状态 | DEV_CHARGE(1009) | FID_CHARGE_GUN |
| 电源状态 | DEV_POWER(1023) | FID_POWER_STATE |
| 驱动模式 | DEV_DRIVE_MODE(1006) | FID_DRIVE_MODE |
| 电机功率 | DEV_MOTOR(1012) | FID_MOTOR_POWER |

返回值哨兵：`65535` = 无 CAN 信号，`-10011` = 未注册，`0xFFFFD8E3` = 错误事务码。

---

## 八、HelperDaemon 架构（安全模型）

为避免任意 app 通过本地端口控制车辆，HelperDaemon 采用 **binder + uid 鉴权**：

- daemon 以 shell uid 运行（`app_process` + `CLASSPATH=app.apk`）
- 通过 `ServiceManager.addService("diui_helper")` 注册 binder service
- `onTransact` 检查 `Binder.getCallingUid() == appUid`，拒绝其他 app
- 写操作额外经 `WriteAllowlist` 限制（仅空调/车身/门锁 dev 可写）
- app 端 `HelperClient` 反射 `ServiceManager.getService("diui_helper")` 调用
- 版本管理：记录 `spawned_version_code`，app 更新后自动 kill 旧 daemon 再 spawn

### 相关文件

| 文件 | 职责 |
|------|------|
| `api/AdbHelper.java` | 本地 ADB 客户端：协议、RSA 认证、shell 执行、daemon spawn/kill |
| `api/AdbKeyManager.java` | RSA 密钥管理：生成/存储/签名（NONEwithRSA+DigestInfo）/524 字节公钥编码 |
| `api/BydPermissionHelper.java` | 权限诊断，检查授权状态 |
| `api/BydEnvironmentDetector.java` | 运行环境检测（真车/需权限/模拟器） |
| `helper/HelperDaemon.java` | shell-uid binder 守护进程 |
| `helper/HelperClient.java` | app 端 binder 客户端 |
| `helper/HelperBinderProtocol.java` | binder 线协议常量 |
| `helper/WriteAllowlist.java` | 写操作 dev 白名单 |

---

## 九、验证清单

真车测试时逐项确认：

- [ ] adb 成功连接 `<车机IP>:5555`
- [ ] 安装迪UI后 logcat 显示 `AC:true Body:true`（非模拟模式）
- [ ] 空调开关在车机上实际生效
- [ ] 温度调节实际生效（车内温度变化）
- [ ] 锁车按钮实际生效
- [ ] 电量/续航显示与仪表盘一致
- [ ] 速度在行驶时实时更新
- [ ] 胎压与仪表盘数值一致（kPa）
- [ ] 车窗控制实际控制车窗升降
- [ ] `ps -A | grep diui_helper` 显示 daemon 在跑

---

## 十、常见问题

**Q: 启动时不弹"允许 USB 调试？"**
A: 检查 `logcat -s AdbHelper:I MainActivity:I BydEnvDetector:I`。可能原因：
- adbd 未监听 5555（开无线调试或 `adb tcpip 5555`）
- 签名/版本号问题（已修复：NONEwithRSA + 0x01000001）
- 改用甲壳虫手动授权（第五节）兜底

**Q: logcat 显示 simulation mode，真车上也这样？**
A: `Class.forName("android.hardware.bydauto.ac.BYDAutoAcDevice")` 失败，或调用异常被误判。检查权限是否已 `pm grant`，`BydPermissionContext` 是否正确包裹 context。

**Q: `pm grant` 报 "not a changeable permission type"**
A: 该权限为 signature 级，BYD 车机上大部分能授成功，报错的几条不影响其他数据。全部失败才需平台签名。

**Q: API 返回 -10011？**
A: 未注册到 BYD 服务，车机重启后重试。

**Q: API 返回 65535？**
A: 该功能在当前车型不支持。

**Q: 约 30 天后功能失效？**
A: BYD 车机定期清除第三方应用权限，需重新执行 `pm grant`（ADB 密钥不丢，daemon 需重启）。app 内会自动重新授权。

---

## 十一、参考资料

- [AndyShaman/BYDMate](https://github.com/AndyShaman/BYDMate) — ADB 协议/HelperDaemon 架构参考
- [wheregoes/byd-apps](https://github.com/wheregoes/byd-apps) — 社区 API 文档（featureId 参考）
- [ahmada3mar/BYD](https://github.com/ahmada3mar/BYD) — ADB 开启工具
- BYD DiLink 3.0 SDK 文档（车厂内部，社区整理版见 wheregoes）
