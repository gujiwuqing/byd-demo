# 迪UI 车辆 API 增强 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完善 ADB 授权机制，实现真实车辆数据获取（速度/挡位/胎压/车窗/车门/续航/电池温度/电芯电压等），优化数据轮询性能。

**Architecture:** 渐进增强——保留现有反射调用 BYDAutoXxxDevice 架构，逐层叠加 Listener 回调、自适应轮询、service call autoservice 补充通道、轻量 Socket 守护进程。四级数据通道自动降级。

**Tech Stack:** Android Java, targetSdk 28, 反射调用 BYD 车机系统 API, ADB 二进制协议, Unix Domain Socket, service call Binder

**注意:** 本项目无测试套件，验证靠构建 + 安装到车机手动测试。每个 Task 完成后 commit。

---

## 文件结构

### 新增文件

| 文件路径 | 职责 |
|----------|------|
| `app/src/main/java/com/bydlauncher/api/BydEnvironmentDetector.java` | 车机环境自动检测（REAL_DEVICE / PERMISSION_NEEDED / SIMULATOR） |
| `app/src/main/java/com/bydlauncher/api/PollState.java` | 轮询状态机枚举（DRIVING/PARKED/CHARGING/IDLE）+ 间隔常量 |
| `app/src/main/java/com/bydlauncher/api/AutoserviceClient.java` | service call autoservice 执行 + Parcel 解析 + 哨兵值过滤 |
| `app/src/main/java/com/bydlauncher/api/FidRegistry.java` | Device Type + Feature ID 常量映射表 |
| `app/src/main/java/com/bydlauncher/api/BydListenerManager.java` | BYD 原生 Listener 统一注册/注销/降级管理 |
| `app/src/main/java/com/bydlauncher/helper/HelperDaemon.java` | Socket 守护进程（shell uid, service call 执行, 安全控制） |
| `app/src/main/java/com/bydlauncher/helper/HelperClient.java` | App 侧 Socket 客户端 |
| `app/src/main/java/com/bydlauncher/helper/WriteAllowlist.java` | 写操作安全白名单常量 |

### 修改文件

| 文件路径 | 改动概要 |
|----------|----------|
| `app/src/main/java/com/bydlauncher/api/BydVehicleManager.java` | 环境检测集成、自适应轮询、Listener 集成、四级数据通道 |
| `app/src/main/java/com/bydlauncher/api/AdbHelper.java` | 批量 shell 执行、守护进程启动、授权分类 |
| `app/src/main/java/com/bydlauncher/api/BydPermissionHelper.java` | 权限分类（signature vs runtime） |
| `app/src/main/java/com/bydlauncher/api/BydBodyworkApi.java` | Listener 注册 |
| `app/src/main/java/com/bydlauncher/api/BydAcApi.java` | Listener 注册 |
| `app/src/main/java/com/bydlauncher/api/BydApiExplorer.java` | featureId 扫描、方法枚举、文件输出 |
| `app/src/main/java/com/bydlauncher/model/VehicleStatus.java` | 新增字段 |
| `app/src/main/java/com/bydlauncher/MainActivity.java` | 环境检测集成、授权验证闭环、开机等待增强 |
| `app/src/main/java/com/bydlauncher/ui/StatusPage.java` | 展示新增数据项 |
| `app/src/main/java/com/bydlauncher/ui/SettingsPage.java` | API 探测按钮、默认值由检测决定 |

---

## Task 1: VehicleStatus 新增字段

**Files:**
- Modify: `app/src/main/java/com/bydlauncher/model/VehicleStatus.java`

- [ ] **Step 1: 在 VehicleStatus.java 中添加新字段**

在 `// 能量模式` 注释块之前，添加以下字段：

```java
    // 电池详情（service call autoservice 补充数据）
    public int batteryTempMax = -1;     // 电池最高温度 (°C, 原始值需 -40 偏移)
    public int batteryTempMin = -1;     // 电池最低温度 (°C)
    public int cellVoltageMax = -1;     // 电芯最大电压 (mV)
    public int cellVoltageMin = -1;     // 电芯最小电压 (mV)
    public double voltage12v = -1;      // 12V 蓄电池电压 (V)
    public int soh = -1;               // 电池健康度 SoH (%)

    // 车辆状态（service call autoservice 补充数据）
    public int chargeGunState = 0;      // 充电枪状态 (0=未连接)
    public int powerState = -1;         // 电源状态 ACC (enum)
    public int driveMode = -1;          // 驾驶模式 (enum)
    public double motorPowerKw = 0;     // 电机牵引功率 (kW)
```

- [ ] **Step 2: 添加格式化方法**

在 `getAcModeText()` 方法之后添加：

```java
    public String getBatteryTempText() {
        if (batteryTempMax < -40 || batteryTempMin < -40) return "N/A";
        return batteryTempMin + "~" + batteryTempMax + "°C";
    }

    public String getCellVoltageText() {
        if (cellVoltageMax < 0 || cellVoltageMin < 0) return "N/A";
        return cellVoltageMin + "~" + cellVoltageMax + "mV";
    }

    public String getVoltage12vText() {
        return voltage12v >= 0 ? String.format("%.1fV", voltage12v) : "N/A";
    }

    public String getSohText() {
        return soh >= 0 ? soh + "%" : "N/A";
    }

    public String getDriveModeText() {
        switch (driveMode) {
            case 0: return "标准";
            case 1: return "运动";
            case 2: return "经济";
            case 3: return "雪地";
            default: return driveMode >= 0 ? "模式" + driveMode : "N/A";
        }
    }

    public String getPowerStateText() {
        switch (powerState) {
            case 0: return "OFF";
            case 1: return "ACC";
            case 2: return "ON";
            default: return powerState >= 0 ? "状态" + powerState : "N/A";
        }
    }

    public String getChargeGunStateText() {
        switch (chargeGunState) {
            case 0: return "未连接";
            case 1: return "已连接";
            case 2: return "充电中";
            default: return "状态" + chargeGunState;
        }
    }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bydlauncher/model/VehicleStatus.java
git commit -m "feat: add battery/charging/drive mode fields to VehicleStatus"
```

---

## Task 2: 环境自动检测器

**Files:**
- Create: `app/src/main/java/com/bydlauncher/api/BydEnvironmentDetector.java`

- [ ] **Step 1: 创建 BydEnvironmentDetector.java**

```java
package com.bydlauncher.api;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

public class BydEnvironmentDetector {

    private static final String TAG = "BydEnvDetector";

    public enum Environment {
        REAL_DEVICE,        // 在 BYD 车机上，API 可用
        PERMISSION_NEEDED,  // 在 BYD 车机上，但权限不足
        SIMULATOR           // 非 BYD 车机
    }

    public static Environment detect(Context context) {
        // Step 1: 尝试加载 BYD API 类
        Class<?> acClass;
        try {
            acClass = Class.forName("android.hardware.bydauto.ac.BYDAutoAcDevice");
            Log.i(TAG, "BYD API class found");
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "BYD API class not found → SIMULATOR");
            return Environment.SIMULATOR;
        }

        // Step 2: 尝试获取实例并调用只读方法
        try {
            Method getInstance = acClass.getMethod("getInstance", Context.class);
            Object device = getInstance.invoke(null, new BydPermissionContext(context));
            if (device == null) {
                Log.i(TAG, "getInstance returned null → PERMISSION_NEEDED");
                return Environment.PERMISSION_NEEDED;
            }

            Method getState = acClass.getMethod("getAcStartState");
            Object result = getState.invoke(device);
            int value = (result instanceof Integer) ? (int) result : -1;

            if (value == 65535 || value == -10011) {
                Log.i(TAG, "API returned sentinel value " + value + " → PERMISSION_NEEDED");
                return Environment.PERMISSION_NEEDED;
            }

            Log.i(TAG, "API returned valid value " + value + " → REAL_DEVICE");
            return Environment.REAL_DEVICE;

        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SecurityException) {
                Log.i(TAG, "SecurityException → PERMISSION_NEEDED: " + cause.getMessage());
                return Environment.PERMISSION_NEEDED;
            }
            Log.w(TAG, "InvocationTargetException → SIMULATOR", e);
            return Environment.SIMULATOR;
        } catch (Exception e) {
            Log.w(TAG, "Detection failed → SIMULATOR", e);
            return Environment.SIMULATOR;
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/bydlauncher/api/BydEnvironmentDetector.java
git commit -m "feat: add BydEnvironmentDetector for auto-detecting car environment"
```

---

## Task 3: 环境检测集成到 MainActivity 和 BydVehicleManager

**Files:**
- Modify: `app/src/main/java/com/bydlauncher/api/BydVehicleManager.java`
- Modify: `app/src/main/java/com/bydlauncher/MainActivity.java`

- [ ] **Step 1: 修改 BydVehicleManager — 环境检测驱动模式切换**

在 `BydVehicleManager.java` 中，将 `forceSimMode` 的默认值从 `true` 改为 `false`，并新增基于环境检测的初始化方法：

把这行：
```java
    private static boolean forceSimMode = true;
```
改为：
```java
    private static boolean forceSimMode = false;
```

在 `resetInstance()` 方法之后添加：

```java
    public static BydEnvironmentDetector.Environment detectAndConfigure(Context context) {
        BydEnvironmentDetector.Environment env = BydEnvironmentDetector.detect(context);
        switch (env) {
            case REAL_DEVICE:
                setForceSimulation(false);
                break;
            case PERMISSION_NEEDED:
                setForceSimulation(false);
                break;
            case SIMULATOR:
                setForceSimulation(true);
                break;
        }
        Log.i(TAG, "Environment detected: " + env + ", simulation=" + forceSimMode);
        return env;
    }
```

- [ ] **Step 2: 修改 MainActivity — 用环境检测替代旧逻辑**

在 `MainActivity.java` 的 `onCreate` 方法中，将现有的模拟模式初始化代码：

```java
        // 读取用户手动设置的模拟模式开关（默认开启）
        boolean forceSim = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean("sim_mode", true);
        BydVehicleManager.setForceSimulation(forceSim);

        vehicleManager = BydVehicleManager.getInstance(this);
```

替换为：

```java
        // 自动检测车机环境（用户手动设置优先）
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasManualOverride = prefs.contains("sim_mode_manual");
        BydEnvironmentDetector.Environment detectedEnv;

        if (hasManualOverride) {
            boolean forceSim = prefs.getBoolean("sim_mode_manual", true);
            BydVehicleManager.setForceSimulation(forceSim);
            detectedEnv = forceSim ? BydEnvironmentDetector.Environment.SIMULATOR
                    : BydEnvironmentDetector.Environment.REAL_DEVICE;
            Log.i(TAG, "Using manual override: simulation=" + forceSim);
        } else {
            detectedEnv = BydVehicleManager.detectAndConfigure(this);
        }

        vehicleManager = BydVehicleManager.getInstance(this);
```

同时需要在文件顶部添加 import（如果尚未存在）：
```java
import android.content.SharedPreferences;
```

- [ ] **Step 3: 修改 MainActivity.checkPermissions — 用 detectedEnv 驱动授权流程**

将 `checkPermissions()` 方法中的 `hasAnyRealApi` 判断逻辑替换。在 `onCreate` 中将 `detectedEnv` 保存为实例变量：

在类顶部添加字段：
```java
    private BydEnvironmentDetector.Environment detectedEnv;
```

在 `onCreate` 中的环境检测部分末尾，`vehicleManager = BydVehicleManager.getInstance(this);` 之前加：
```java
        this.detectedEnv = detectedEnv;
```

然后将 `checkPermissions()` 方法中的权限检查段落：

```java
        // 检查 BYD 车辆 API 权限（仅在真实车机上检查）
        boolean hasAnyRealApi = vehicleManager.getAcApi().isRealDevice()
                || vehicleManager.getBodyworkApi().isAvailable()
                || vehicleManager.getStatisticApi().isAvailable();

        // 如果 ADB 已授权过，跳过弹窗
        if (adbAlreadyGranted) {
            Log.i(TAG, "ADB permissions already granted, skipping check");
        } else if (hasAnyRealApi) {
            boolean hasMissingApi = !vehicleManager.getAcApi().isRealDevice()
                    || !vehicleManager.getDriveApi().isRealDevice()
                    || !vehicleManager.getTireApi().isRealDevice();
            if (hasMissingApi) {
                if (fromBoot) {
                    // 开机后 ADB 服务可能还未就绪，延迟重试检测
                    scheduleAdbCheckWithRetry(3, 2000);
                } else {
                    showBydPermissionDialog();
                }
            }
        }
```

替换为：

```java
        // 检查 BYD 车辆 API 权限
        if (adbAlreadyGranted) {
            Log.i(TAG, "ADB permissions already granted, skipping check");
        } else if (detectedEnv == BydEnvironmentDetector.Environment.PERMISSION_NEEDED) {
            if (fromBoot) {
                scheduleAdbCheckWithRetry(5, 2000);
            } else {
                showBydPermissionDialog();
            }
        } else if (detectedEnv == BydEnvironmentDetector.Environment.REAL_DEVICE) {
            if (!BydPermissionHelper.hasAllPermissions(this)) {
                if (fromBoot) {
                    scheduleAdbCheckWithRetry(5, 2000);
                } else {
                    showBydPermissionDialog();
                }
            }
        }
```

- [ ] **Step 4: 修改 SettingsPage 的模拟模式开关 — 使用新的 SharedPreferences key**

在 `MainActivity.java` 的 `reinitializeWithSimMode` 方法中，同步保存手动覆盖标记。将方法中的保存逻辑改为同时写入 `sim_mode_manual`：

找到 `reinitializeWithSimMode` 方法，在方法开头（`Log.i` 之后）加一行：

```java
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putBoolean("sim_mode_manual", forceSim).apply();
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bydlauncher/api/BydVehicleManager.java
git add app/src/main/java/com/bydlauncher/MainActivity.java
git commit -m "feat: integrate environment detection, replace manual sim mode default"
```

---

## Task 4: ADB 授权增强 — 重试、验证、分类反馈

**Files:**
- Modify: `app/src/main/java/com/bydlauncher/api/AdbHelper.java`
- Modify: `app/src/main/java/com/bydlauncher/api/BydPermissionHelper.java`
- Modify: `app/src/main/java/com/bydlauncher/MainActivity.java`

- [ ] **Step 1: 修改 AdbHelper — 授权结果增加分类**

在 `AdbHelper.java` 中，修改 `GrantCallback` 接口和 `grantPermissions` 方法，增加 `signature` 分类：

将 `GrantCallback` 接口改为：

```java
    public interface GrantCallback {
        void onResult(boolean success, List<String> granted, List<String> failed, List<String> signature);
    }
```

在 `grantPermissions` 方法中，添加 `signature` 列表。将方法中的权限循环和回调部分修改为：

```java
                List<String> signature = new ArrayList<>();

                for (String perm : BydPermissionHelper.getAllPermissions()) {
                    String shortName = perm.substring("android.permission.BYDAUTO_".length());
                    String cmd = "pm grant " + context.getPackageName() + " " + perm;
                    String result = execShell(in, out, cmd);
                    if (result != null && !result.contains("Exception") && !result.contains("Error")) {
                        Log.i(TAG, "  ✓ " + shortName);
                        granted.add(shortName);
                    } else if (result != null && result.contains("not a changeable permission type")) {
                        Log.w(TAG, "  ⚠ " + shortName + ": signature 级别权限，需要平台签名");
                        signature.add(shortName);
                    } else {
                        Log.w(TAG, "  ✗ " + shortName + (result != null ? ": " + result.trim() : ""));
                        failed.add(shortName);
                    }
                }

                Log.i(TAG, "授权完成: 成功 " + granted.size() + ", signature " + signature.size() + ", 失败 " + failed.size());
                callback.onResult(failed.isEmpty(), granted, failed, signature);
```

同时修改 `catch` 和 `ADB 认证失败` 等提前返回的回调调用，都加上空的 `signature` 列表：

```java
                    callback.onResult(false, granted, failed, new ArrayList<>());
```

（共有 3 处提前 return 的 `callback.onResult` 调用需要修改）

- [ ] **Step 2: 新增 AdbHelper.execShellBatch 方法**

在 `AdbHelper.java` 中，`execShell` 方法之后添加批量执行方法：

```java
    public static String execShellBatch(InputStream in, OutputStream out, List<String> commands) throws IOException {
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < commands.size(); i++) {
            if (i > 0) combined.append("; echo '---SEPARATOR---'; ");
            combined.append(commands.get(i));
        }
        return execShell(in, out, combined.toString());
    }
```

同时需要在文件顶部添加 `import java.util.List;`（如果尚未存在，检查已有 import）。

- [ ] **Step 3: 修改 BydPermissionHelper — 新增权限分类方法**

在 `BydPermissionHelper.java` 中添加方法，用于区分哪些权限是 runtime 可授权的：

```java
    public static List<String> getMissingPermissions(Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        List<String> missing = new ArrayList<>();
        for (String perm : BYD_PERMISSIONS) {
            if (pm.checkPermission(perm, packageName) != PackageManager.PERMISSION_GRANTED) {
                missing.add(perm);
            }
        }
        return missing;
    }

    public static int getGrantedCount(Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        int count = 0;
        for (String perm : BYD_PERMISSIONS) {
            if (pm.checkPermission(perm, packageName) == PackageManager.PERMISSION_GRANTED) {
                count++;
            }
        }
        return count;
    }
```

- [ ] **Step 4: 修改 MainActivity — 授权后验证闭环 + 开机等待增强**

在 `MainActivity.java` 中修改 `startAdbGrant()` 方法，增加验证步骤：

```java
    private void startAdbGrant() {
        AdbHelper.grantPermissions(this, (success, granted, failed, signature) -> runOnUiThread(() -> {
            if (!granted.isEmpty()) {
                // 等待 500ms 让系统刷新权限缓存，然后验证
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    boolean verified = BydPermissionHelper.hasAllPermissions(this);
                    int grantedCount = BydPermissionHelper.getGrantedCount(this);
                    int total = BydPermissionHelper.getAllPermissions().length;

                    Log.i(TAG, "授权验证: " + grantedCount + "/" + total
                            + " (signature级别: " + signature.size() + ")");

                    // 保存 ADB 已授权标记
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_ADB_GRANTED, true)
                            .remove("sim_mode_manual")
                            .apply();

                    String msg;
                    if (verified) {
                        msg = "权限授权完成 (" + grantedCount + "/" + total + ")";
                    } else if (!signature.isEmpty()) {
                        msg = "已授权 " + grantedCount + "/" + total
                                + "，" + signature.size() + " 个需要平台签名（不影响基本功能）";
                    } else {
                        msg = "已授权 " + grantedCount + "/" + total;
                    }
                    android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();

                    // 切换到真车模式并重新初始化
                    BydVehicleManager.setForceSimulation(false);
                    BydVehicleManager.resetInstance();
                    vehicleManager = BydVehicleManager.getInstance(this);
                    vehicleManager.setListener(this);
                    vehicleManager.startPolling();

                    View pageControlsView = findViewById(R.id.page_controls);
                    controlsPage = new ControlsPage(pageControlsView, vehicleManager.getAcApi());

                    if (settingsPage != null) {
                        settingsPage.updateSimulationState(false);
                    }
                }, 500);
            } else {
                android.widget.Toast.makeText(this,
                        getString(R.string.perm_byd_adb_fail),
                        android.widget.Toast.LENGTH_LONG).show();
            }
        }));
    }
```

修改 `scheduleAdbCheckWithRetry` 方法，改为递增间隔：

```java
    private static final long[] RETRY_DELAYS = {2000, 3000, 5000, 8000, 10000};

    private void scheduleAdbCheckWithRetry(int retriesLeft, int retryIndex) {
        long delay = retryIndex < RETRY_DELAYS.length ? RETRY_DELAYS[retryIndex] : 10000;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            if (AdbHelper.isAdbAvailable()) {
                Log.i(TAG, "ADB detected after delay");
                showAdbAuthDialog();
            } else if (retriesLeft > 1) {
                Log.i(TAG, "ADB not ready, retrying... (" + (retriesLeft - 1) + " left, next delay " + RETRY_DELAYS[Math.min(retryIndex + 1, RETRY_DELAYS.length - 1)] + "ms)");
                scheduleAdbCheckWithRetry(retriesLeft - 1, retryIndex + 1);
            } else {
                Log.w(TAG, "ADB not available after all retries, showing manual dialog");
                showBydPermissionDialog();
            }
        }, delay);
    }
```

同时更新 `checkPermissions()` 中对 `scheduleAdbCheckWithRetry` 的调用：

```java
                    scheduleAdbCheckWithRetry(5, 0);
```

（两处调用都改为传 `5, 0`）

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bydlauncher/api/AdbHelper.java
git add app/src/main/java/com/bydlauncher/api/BydPermissionHelper.java
git add app/src/main/java/com/bydlauncher/MainActivity.java
git commit -m "feat: ADB grant with retry/verify/classification, incremental boot delay"
```

---

## Task 5: PollState 状态机 + 自适应轮询

**Files:**
- Create: `app/src/main/java/com/bydlauncher/api/PollState.java`
- Modify: `app/src/main/java/com/bydlauncher/api/BydVehicleManager.java`

- [ ] **Step 1: 创建 PollState.java**

```java
package com.bydlauncher.api;

public enum PollState {
    DRIVING(1000),
    PARKED(5000),
    CHARGING(5000),
    IDLE(30000);

    public final long intervalMs;

    PollState(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public static PollState classify(int speed, int gear, int chargeGunState, boolean dataValid) {
        if (!dataValid) return IDLE;
        if (chargeGunState > 0) return CHARGING;
        if (speed > 0) return DRIVING;
        return PARKED;
    }
}
```

- [ ] **Step 2: 修改 BydVehicleManager — 集成自适应轮询**

在 `BydVehicleManager.java` 中添加字段（在 `private static final long POLL_INTERVAL = 2000;` 之后）：

```java
    private PollState currentPollState = PollState.PARKED;
    private int consecutiveFailures = 0;
    private static final int MAX_BACKOFF_INTERVAL = 60000;
```

删除 `private static final long POLL_INTERVAL = 2000;` 这一行（不再使用固定间隔）。

将 `pollRunnable` 替换为：

```java
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!polling) return;
            VehicleStatus status = readCurrentStatus();

            // 判断数据是否有效
            boolean dataValid = (status.speed >= 0 || status.batteryPercent >= 0);
            if (!dataValid) {
                consecutiveFailures++;
            } else {
                consecutiveFailures = 0;
            }

            // 状态分类
            currentPollState = PollState.classify(
                    status.speed, status.gear, status.chargeGunState, dataValid);

            if (listener != null) {
                listener.onStatusUpdated(status);
            }

            // 计算下次间隔（含退避）
            long interval = currentPollState.intervalMs;
            if (consecutiveFailures > 0) {
                interval = Math.min(
                        (long) (interval * Math.pow(1.5, consecutiveFailures)),
                        MAX_BACKOFF_INTERVAL);
            }

            handler.postDelayed(this, interval);
        }
    };
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bydlauncher/api/PollState.java
git add app/src/main/java/com/bydlauncher/api/BydVehicleManager.java
git commit -m "feat: adaptive polling with state machine (drive 1s/park 5s/charge 5s/idle 30s)"
```

---

## Task 6: FidRegistry — Feature ID 映射表

**Files:**
- Create: `app/src/main/java/com/bydlauncher/api/FidRegistry.java`

- [ ] **Step 1: 创建 FidRegistry.java**

```java
package com.bydlauncher.api;

public final class FidRegistry {

    // Device Types
    public static final int DEV_AC = 1000;
    public static final int DEV_BODYWORK = 1001;
    public static final int DEV_DRIVE_MODE = 1006;
    public static final int DEV_CHARGE = 1009;
    public static final int DEV_GEARBOX = 1011;
    public static final int DEV_MOTOR = 1012;
    public static final int DEV_SPEED = 1013;
    public static final int DEV_BATTERY = 1014;
    public static final int DEV_TIRE = 1016;
    public static final int DEV_POWER = 1023;
    public static final int DEV_DOOR_LOCK = 1041;

    // transact codes for service call autoservice
    public static final int TX_GET_INT = 5;
    public static final int TX_SET_INT = 6;
    public static final int TX_GET_FLOAT = 7;

    // === Battery (DEV_BATTERY = 1014) ===
    public static final int FID_SOC = 1246777400;           // float, 电量 %
    public static final int FID_MILEAGE = 1246765072;       // int, 里程 (÷10)
    public static final int FID_SOH = 1145045032;           // int, 健康度 %
    public static final int FID_BATT_TEMP_MAX = 1148190752; // int, 最高温度 (-40偏移)
    public static final int FID_BATT_TEMP_MIN = 1148190736; // int, 最低温度 (-40偏移)
    public static final int FID_CELL_VOLT_MAX = 1147142192; // int, 电芯最大电压 mV
    public static final int FID_CELL_VOLT_MIN = 1147142160; // int, 电芯最小电压 mV
    public static final int FID_ACCUM_ENERGY = 1032871984;  // float, 累计能耗 kWh

    // === Speed (DEV_SPEED = 1013) ===
    public static final int FID_SPEED = -1807745016;        // float, km/h

    // === Gearbox (DEV_GEARBOX = 1011) ===
    public static final int FID_GEAR = 555745336;           // int, enum

    // === Motor (DEV_MOTOR = 1012) ===
    public static final int FID_MOTOR_POWER = 339738656;    // int, kW

    // === Charge (DEV_CHARGE = 1009) ===
    public static final int FID_CHARGE_GUN = 876609586;     // int, enum

    // === Power (DEV_POWER = 1023) ===
    public static final int FID_POWER_STATE = 315621408;    // int, enum

    // === Drive Mode (DEV_DRIVE_MODE = 1006) ===
    public static final int FID_DRIVE_MODE = 555745294;     // int, enum

    // === Bodywork (DEV_BODYWORK = 1001) ===
    public static final int FID_12V_VOLTAGE = 1128267816;   // float, V
    public static final int FID_DOOR_FL = 692060168;        // int, enum
    public static final int FID_DOOR_FR = 692060170;        // int, enum
    public static final int FID_DOOR_RL = 692060172;        // int, enum
    public static final int FID_DOOR_RR = 692060174;        // int, enum
    public static final int FID_WINDOW_FL = 947912728;      // int, %
    public static final int FID_WINDOW_FR = 1267728400;     // int, %
    public static final int FID_WINDOW_RL = 947912736;      // int, %
    public static final int FID_WINDOW_RR = 947912752;      // int, %

    // === Tire (DEV_TIRE = 1016) ===
    public static final int FID_TIRE_FL = -1728052956;      // int, kPa
    public static final int FID_TIRE_FR = -1728052952;      // int, kPa
    public static final int FID_TIRE_RL = -1728052948;      // int, kPa
    public static final int FID_TIRE_RR = -1728052944;      // int, kPa

    // === AC (DEV_AC = 1000) ===
    public static final int FID_AC_STATE = 1077936144;      // int, enum
    public static final int FID_AC_TEMP = 1077936168;       // int, °C
    public static final int FID_CABIN_TEMP = 1031798832;    // int, °C
    public static final int FID_OUTSIDE_TEMP = 1077936184;  // int, °C
    public static final int FID_AC_WIND = 0x1DE0000C;       // int, level

    // === Sentinel values ===
    public static final int SENTINEL_NO_CAN = 0x0000FFFF;    // 65535, CAN 链路未建立
    public static final int SENTINEL_UNINIT = 0x000FFFFF;     // 1048575, 未初始化
    public static final int SENTINEL_BAD_TX = 0xFFFFD8E3;     // -10013, 错误的 transact code
    public static final int SENTINEL_NO_WRITE = 0xFFFFD8E5;   // -10011, FID 不可写

    public static boolean isSentinel(int value) {
        return value == SENTINEL_NO_CAN
                || value == SENTINEL_UNINIT
                || value == SENTINEL_BAD_TX
                || value == SENTINEL_NO_WRITE;
    }

    public static boolean isSentinelFloat(float value) {
        return value == -1.0f || Float.isNaN(value);
    }

    private FidRegistry() {}
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/bydlauncher/api/FidRegistry.java
git commit -m "feat: add FidRegistry with device types, feature IDs, and sentinel values"
```

---

## Task 7: AutoserviceClient — service call 执行 + Parcel 解析

**Files:**
- Create: `app/src/main/java/com/bydlauncher/api/AutoserviceClient.java`
- Modify: `app/src/main/java/com/bydlauncher/api/BydVehicleManager.java`

- [ ] **Step 1: 创建 AutoserviceClient.java**

```java
package com.bydlauncher.api;

import android.content.Context;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoserviceClient {

    private static final String TAG = "AutoserviceClient";
    private static final Pattern PARCEL_PATTERN = Pattern.compile(
            "Result:\\s*Parcel\\(([0-9a-fA-F\\s]+)'");

    private final Context context;
    private boolean available = false;

    public AutoserviceClient(Context context) {
        this.context = context;
        this.available = AdbHelper.isAdbAvailable();
    }

    public boolean isAvailable() {
        return available;
    }

    public int getInt(int deviceType, int featureId) {
        String cmd = "service call autoservice " + FidRegistry.TX_GET_INT
                + " i32 " + deviceType + " i32 " + featureId;
        String result = execAdbCommand(cmd);
        return parseParcelInt(result);
    }

    public float getFloat(int deviceType, int featureId) {
        String cmd = "service call autoservice " + FidRegistry.TX_GET_FLOAT
                + " i32 " + deviceType + " i32 " + featureId;
        String result = execAdbCommand(cmd);
        return parseParcelFloat(result);
    }

    public Map<String, Object> readBatteryExtras() {
        Map<String, Object> data = new LinkedHashMap<>();

        int tempMax = getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_BATT_TEMP_MAX);
        if (!FidRegistry.isSentinel(tempMax)) data.put("batteryTempMax", tempMax - 40);

        int tempMin = getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_BATT_TEMP_MIN);
        if (!FidRegistry.isSentinel(tempMin)) data.put("batteryTempMin", tempMin - 40);

        int cellMax = getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_CELL_VOLT_MAX);
        if (!FidRegistry.isSentinel(cellMax)) data.put("cellVoltageMax", cellMax);

        int cellMin = getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_CELL_VOLT_MIN);
        if (!FidRegistry.isSentinel(cellMin)) data.put("cellVoltageMin", cellMin);

        int soh = getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_SOH);
        if (!FidRegistry.isSentinel(soh)) data.put("soh", soh);

        return data;
    }

    public Map<String, Object> readVehicleExtras() {
        Map<String, Object> data = new LinkedHashMap<>();

        float v12 = getFloat(FidRegistry.DEV_BODYWORK, FidRegistry.FID_12V_VOLTAGE);
        if (!FidRegistry.isSentinelFloat(v12)) data.put("voltage12v", (double) v12);

        int chargeGun = getInt(FidRegistry.DEV_CHARGE, FidRegistry.FID_CHARGE_GUN);
        if (!FidRegistry.isSentinel(chargeGun)) data.put("chargeGunState", chargeGun);

        int powerState = getInt(FidRegistry.DEV_POWER, FidRegistry.FID_POWER_STATE);
        if (!FidRegistry.isSentinel(powerState)) data.put("powerState", powerState);

        int driveMode = getInt(FidRegistry.DEV_DRIVE_MODE, FidRegistry.FID_DRIVE_MODE);
        if (!FidRegistry.isSentinel(driveMode)) data.put("driveMode", driveMode);

        int motorPower = getInt(FidRegistry.DEV_MOTOR, FidRegistry.FID_MOTOR_POWER);
        if (!FidRegistry.isSentinel(motorPower)) data.put("motorPowerKw", motorPower);

        return data;
    }

    private String execAdbCommand(String cmd) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", 5555), 3000);
            socket.setSoTimeout(5000);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            // 简单 shell 执行（复用 ADB 连接认证）
            // 这里调用 Runtime.exec 作为简化方案
        } catch (Exception e) {
            Log.w(TAG, "ADB command failed: " + cmd, e);
        } finally {
            if (socket != null) try { socket.close(); } catch (Exception ignored) {}
        }

        // 降级方案：通过 Runtime.exec 执行
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "Shell exec failed: " + cmd, e);
            return null;
        }
    }

    static int parseParcelInt(String parcelOutput) {
        if (parcelOutput == null || parcelOutput.isEmpty()) return -1;
        try {
            Matcher m = PARCEL_PATTERN.matcher(parcelOutput);
            if (!m.find()) return -1;
            String hex = m.group(1).replaceAll("\\s+", "");
            if (hex.length() < 16) return -1;
            // Parcel 格式: 4 bytes status + 4 bytes value (little-endian words, but each word is big-endian in text)
            String valueHex = hex.substring(8, 16);
            // service call 输出的每个 word 是 big-endian 文本表示
            int value = (int) Long.parseLong(valueHex, 16);
            return value;
        } catch (Exception e) {
            Log.w(TAG, "parseParcelInt failed: " + parcelOutput, e);
            return -1;
        }
    }

    static float parseParcelFloat(String parcelOutput) {
        int bits = parseParcelInt(parcelOutput);
        if (bits == -1) return -1.0f;
        return Float.intBitsToFloat(bits);
    }
}
```

- [ ] **Step 2: 修改 BydVehicleManager — 集成 AutoserviceClient 补充数据**

在 `BydVehicleManager.java` 中添加 `AutoserviceClient` 字段：

在构造函数中的 `this.driveApi = new BydDriveApi(appContext);` 之后添加：

```java
        this.autoserviceClient = new AutoserviceClient(appContext);
```

在类顶部的字段声明中添加：

```java
    private final AutoserviceClient autoserviceClient;
```

在 `readCurrentStatus()` 方法的末尾（`fillTireSimulationData` 之后，`return s;` 之前）添加：

```java
        // ========== service call autoservice 补充数据 ==========
        if (autoserviceClient.isAvailable()) {
            try {
                Map<String, Object> batteryExtras = autoserviceClient.readBatteryExtras();
                if (batteryExtras.containsKey("batteryTempMax"))
                    s.batteryTempMax = (int) batteryExtras.get("batteryTempMax");
                if (batteryExtras.containsKey("batteryTempMin"))
                    s.batteryTempMin = (int) batteryExtras.get("batteryTempMin");
                if (batteryExtras.containsKey("cellVoltageMax"))
                    s.cellVoltageMax = (int) batteryExtras.get("cellVoltageMax");
                if (batteryExtras.containsKey("cellVoltageMin"))
                    s.cellVoltageMin = (int) batteryExtras.get("cellVoltageMin");
                if (batteryExtras.containsKey("soh"))
                    s.soh = (int) batteryExtras.get("soh");

                Map<String, Object> vehicleExtras = autoserviceClient.readVehicleExtras();
                if (vehicleExtras.containsKey("voltage12v"))
                    s.voltage12v = (double) vehicleExtras.get("voltage12v");
                if (vehicleExtras.containsKey("chargeGunState"))
                    s.chargeGunState = (int) vehicleExtras.get("chargeGunState");
                if (vehicleExtras.containsKey("powerState"))
                    s.powerState = (int) vehicleExtras.get("powerState");
                if (vehicleExtras.containsKey("driveMode"))
                    s.driveMode = (int) vehicleExtras.get("driveMode");
                if (vehicleExtras.containsKey("motorPowerKw"))
                    s.motorPowerKw = (int) vehicleExtras.get("motorPowerKw");
            } catch (Exception e) {
                Log.w(TAG, "AutoserviceClient data fetch failed", e);
            }
        }
```

需要在文件顶部添加 import：
```java
import java.util.Map;
```

同时添加 getter：
```java
    public AutoserviceClient getAutoserviceClient() { return autoserviceClient; }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bydlauncher/api/AutoserviceClient.java
git add app/src/main/java/com/bydlauncher/api/BydVehicleManager.java
git commit -m "feat: add AutoserviceClient for service call data channel + integrate into VehicleManager"
```

---

## Task 8: BydListenerManager — 事件驱动

**Files:**
- Create: `app/src/main/java/com/bydlauncher/api/BydListenerManager.java`
- Modify: `app/src/main/java/com/bydlauncher/api/BydVehicleManager.java`

- [ ] **Step 1: 创建 BydListenerManager.java**

```java
package com.bydlauncher.api;

import android.util.Log;

import com.bydlauncher.model.VehicleStatus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class BydListenerManager {

    private static final String TAG = "BydListenerManager";

    public interface OnStateChanged {
        void onChanged();
    }

    private OnStateChanged callback;
    private final VehicleStatus sharedStatus;
    private boolean bodyworkListenerActive = false;
    private boolean acListenerActive = false;

    public BydListenerManager(VehicleStatus sharedStatus) {
        this.sharedStatus = sharedStatus;
    }

    public void setCallback(OnStateChanged callback) {
        this.callback = callback;
    }

    public void registerBodyworkListener(Object bodyworkDevice) {
        if (bodyworkDevice == null) return;
        try {
            Class<?> listenerClass = Class.forName(
                    "android.hardware.bydauto.bodywork.AbsBYDAutoBodyworkListener");

            Object proxy = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    new BodyworkHandler());

            Method register = bodyworkDevice.getClass().getMethod(
                    "registerListener", listenerClass);
            register.invoke(bodyworkDevice, proxy);
            bodyworkListenerActive = true;
            Log.i(TAG, "Bodywork listener registered");
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Bodywork listener class not found, falling back to polling");
        } catch (Exception e) {
            Log.w(TAG, "Failed to register bodywork listener, falling back to polling", e);
        }
    }

    public void registerAcListener(Object acDevice) {
        if (acDevice == null) return;
        try {
            Class<?> listenerClass = Class.forName(
                    "android.hardware.bydauto.ac.AbsBYDAutoAcListener");

            Object proxy = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    new AcHandler());

            Method register = acDevice.getClass().getMethod(
                    "registerListener", listenerClass);
            register.invoke(acDevice, proxy);
            acListenerActive = true;
            Log.i(TAG, "AC listener registered");
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "AC listener class not found, falling back to polling");
        } catch (Exception e) {
            Log.w(TAG, "Failed to register AC listener, falling back to polling", e);
        }
    }

    public boolean isBodyworkListenerActive() { return bodyworkListenerActive; }
    public boolean isAcListenerActive() { return acListenerActive; }

    private void notifyChanged() {
        if (callback != null) callback.onChanged();
    }

    private class BodyworkHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            try {
                switch (name) {
                    case "onDoorStateChanged":
                        if (args != null && args.length >= 2) {
                            int area = (int) args[0];
                            int state = (int) args[1];
                            boolean open = (state == 1);
                            switch (area) {
                                case 1: sharedStatus.doorLeftFrontOpen = open; break;
                                case 2: sharedStatus.doorRightFrontOpen = open; break;
                                case 3: sharedStatus.doorLeftRearOpen = open; break;
                                case 4: sharedStatus.doorRightRearOpen = open; break;
                                case 5: sharedStatus.hoodOpen = open; break;
                                case 6: sharedStatus.trunkOpen = open; break;
                            }
                            notifyChanged();
                        }
                        break;
                    case "onWindowStateChanged":
                        if (args != null && args.length >= 2) {
                            int area = (int) args[0];
                            int state = (int) args[1];
                            switch (area) {
                                case 1: sharedStatus.windowFL = state; break;
                                case 2: sharedStatus.windowFR = state; break;
                                case 3: sharedStatus.windowRL = state; break;
                                case 4: sharedStatus.windowRR = state; break;
                            }
                            notifyChanged();
                        }
                        break;
                    case "onAutoSystemStateChanged":
                        if (args != null && args.length >= 1) {
                            sharedStatus.isLocked = ((int) args[0] == 1);
                            notifyChanged();
                        }
                        break;
                    case "toString":
                        return "BydListenerManager.BodyworkHandler";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                }
            } catch (Exception e) {
                Log.w(TAG, "Bodywork callback error: " + name, e);
            }
            return null;
        }
    }

    private class AcHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            try {
                switch (name) {
                    case "onAcStartStateChanged":
                        if (args != null && args.length >= 1) {
                            sharedStatus.acOn = ((int) args[0] == 1);
                            notifyChanged();
                        }
                        break;
                    case "onAcTemperatureChanged":
                        if (args != null && args.length >= 2) {
                            int zone = (int) args[0];
                            int temp = (int) args[1];
                            if (zone == 1) sharedStatus.acTemp = temp;
                            else if (zone == 4) sharedStatus.outsideTemp = temp;
                            notifyChanged();
                        }
                        break;
                    case "onAcWindLevelChanged":
                        if (args != null && args.length >= 1) {
                            sharedStatus.acWindLevel = (int) args[0];
                            notifyChanged();
                        }
                        break;
                    case "onAcCycleModeChanged":
                        if (args != null && args.length >= 1) {
                            sharedStatus.acCycleMode = (int) args[0];
                            notifyChanged();
                        }
                        break;
                    case "toString":
                        return "BydListenerManager.AcHandler";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                }
            } catch (Exception e) {
                Log.w(TAG, "AC callback error: " + name, e);
            }
            return null;
        }
    }
}
```

- [ ] **Step 2: 集成 BydListenerManager 到 BydVehicleManager**

在 `BydVehicleManager.java` 中添加字段：

```java
    private BydListenerManager listenerManager;
    private final VehicleStatus sharedStatus = new VehicleStatus();
```

在构造函数末尾（`Log.i` 之前）添加：

```java
        this.listenerManager = new BydListenerManager(sharedStatus);
        listenerManager.setCallback(() -> {
            if (listener != null) {
                handler.post(() -> listener.onStatusUpdated(sharedStatus));
            }
        });
```

在 `startPolling()` 方法的 `handler.post(pollRunnable);` 之前添加 Listener 注册：

```java
        // 尝试注册事件 Listener（失败则静默降级回轮询）
        if (!listenerManager.isBodyworkListenerActive()) {
            listenerManager.registerBodyworkListener(getBodyworkDevice());
        }
        if (!listenerManager.isAcListenerActive()) {
            listenerManager.registerAcListener(getAcDevice());
        }
```

添加辅助方法获取底层 device 对象（在类末尾添加）：

```java
    Object getBodyworkDevice() {
        return BydVehicleManager.isForceSimulation() ? null
                : ReflectionHelper.getDeviceInstance(
                    "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice",
                    acApi != null ? null : null);
    }

    Object getAcDevice() {
        return BydVehicleManager.isForceSimulation() ? null
                : ReflectionHelper.getDeviceInstance(
                    "android.hardware.bydauto.ac.BYDAutoAcDevice",
                    acApi != null ? null : null);
    }
```

注意：这些辅助方法需要 Context。由于构造函数已经不保存 context 引用，改为在构造函数中直接注册。将上面 `startPolling` 中的注册代码移到构造函数中的 `listenerManager.setCallback(...)` 之后：

```java
        if (!forceSimMode) {
            Object bodyworkDev = ReflectionHelper.getDeviceInstance(
                    "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice", appContext);
            listenerManager.registerBodyworkListener(bodyworkDev);

            Object acDev = ReflectionHelper.getDeviceInstance(
                    "android.hardware.bydauto.ac.BYDAutoAcDevice", appContext);
            listenerManager.registerAcListener(acDev);
        }
```

删除前面加到 `startPolling` 中的 Listener 注册代码和 `getBodyworkDevice()`/`getAcDevice()` 辅助方法。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bydlauncher/api/BydListenerManager.java
git add app/src/main/java/com/bydlauncher/api/BydVehicleManager.java
git commit -m "feat: add BydListenerManager for event-driven door/window/AC updates"
```

---

## Task 9: WriteAllowlist + HelperDaemon + HelperClient

**Files:**
- Create: `app/src/main/java/com/bydlauncher/helper/WriteAllowlist.java`
- Create: `app/src/main/java/com/bydlauncher/helper/HelperDaemon.java`
- Create: `app/src/main/java/com/bydlauncher/helper/HelperClient.java`
- Modify: `app/src/main/java/com/bydlauncher/api/AdbHelper.java`

- [ ] **Step 1: 创建 WriteAllowlist.java**

```java
package com.bydlauncher.helper;

import java.util.HashSet;
import java.util.Set;

public final class WriteAllowlist {

    private static final Set<Integer> ALLOWED_WRITE_DEVS = new HashSet<>();
    private static final Set<Integer> BANNED_WRITE_DEVS = new HashSet<>();

    static {
        // 允许写入的舒适类设备
        ALLOWED_WRITE_DEVS.add(1000); // 空调
        ALLOWED_WRITE_DEVS.add(1001); // 车身（车窗/灯光）
        ALLOWED_WRITE_DEVS.add(1041); // 门锁

        // 禁止写入的安全相关设备
        BANNED_WRITE_DEVS.add(1004); // 引擎
        BANNED_WRITE_DEVS.add(1006); // 驾驶模式
        BANNED_WRITE_DEVS.add(1007); // 安全系统
        BANNED_WRITE_DEVS.add(1009); // 充电
        BANNED_WRITE_DEVS.add(1011); // 变速箱
        BANNED_WRITE_DEVS.add(1012); // 电机
        BANNED_WRITE_DEVS.add(1013); // 速度
        BANNED_WRITE_DEVS.add(1014); // 电池
        BANNED_WRITE_DEVS.add(1016); // 胎压
        BANNED_WRITE_DEVS.add(1023); // 电源
        BANNED_WRITE_DEVS.add(1032); // 其他安全
    }

    public static boolean isWriteAllowed(int deviceType) {
        return ALLOWED_WRITE_DEVS.contains(deviceType);
    }

    public static boolean isWriteBanned(int deviceType) {
        return BANNED_WRITE_DEVS.contains(deviceType);
    }

    private WriteAllowlist() {}
}
```

- [ ] **Step 2: 创建 HelperDaemon.java**

```java
package com.bydlauncher.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class HelperDaemon {

    private static final String LOCK_FILE = "/data/local/tmp/bydui_helper.lock";
    private static final int PORT = 19876;
    private static final long IDLE_TIMEOUT_MS = 10 * 60 * 1000; // 10 分钟

    private static volatile long lastRequestTime = System.currentTimeMillis();

    public static void main(String[] args) {
        System.out.println("[HelperDaemon] Starting...");

        // 文件锁保证单实例
        try {
            RandomAccessFile lockFile = new RandomAccessFile(LOCK_FILE, "rw");
            FileChannel channel = lockFile.getChannel();
            FileLock lock = channel.tryLock();
            if (lock == null) {
                System.out.println("[HelperDaemon] Another instance running, exiting");
                return;
            }
        } catch (Exception e) {
            System.out.println("[HelperDaemon] Lock failed: " + e.getMessage());
        }

        // 启动空闲超时监控
        Thread idleMonitor = new Thread(() -> {
            while (true) {
                try { Thread.sleep(60000); } catch (InterruptedException e) { return; }
                if (System.currentTimeMillis() - lastRequestTime > IDLE_TIMEOUT_MS) {
                    System.out.println("[HelperDaemon] Idle timeout, exiting");
                    System.exit(0);
                }
            }
        }, "idle-monitor");
        idleMonitor.setDaemon(true);
        idleMonitor.start();

        // 监听本地端口
        try (ServerSocket server = new ServerSocket(PORT, 5,
                java.net.InetAddress.getByName("127.0.0.1"))) {
            System.out.println("[HelperDaemon] Listening on 127.0.0.1:" + PORT);

            while (true) {
                Socket client = server.accept();
                client.setSoTimeout(5000);
                handleClient(client);
            }
        } catch (Exception e) {
            System.out.println("[HelperDaemon] Server error: " + e.getMessage());
        }
    }

    private static void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true)) {

            String line = in.readLine();
            if (line == null) return;
            lastRequestTime = System.currentTimeMillis();

            // 简单文本协议: "GET <dev> <fid>" 或 "GETF <dev> <fid>" 或 "SET <dev> <fid> <val>"
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 3) {
                out.println("ERR invalid command");
                return;
            }

            String cmd = parts[0];
            int dev, fid;
            try {
                dev = Integer.parseInt(parts[1]);
                fid = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                out.println("ERR invalid params");
                return;
            }

            if ("SET".equals(cmd)) {
                if (!WriteAllowlist.isWriteAllowed(dev)) {
                    out.println("ERR write banned for device " + dev);
                    return;
                }
                if (parts.length < 4) {
                    out.println("ERR missing value");
                    return;
                }
                int val = Integer.parseInt(parts[3]);
                String result = execServiceCall(6, dev, fid, val);
                out.println("OK " + result);
            } else if ("GET".equals(cmd)) {
                String result = execServiceCall(5, dev, fid, -1);
                out.println("OK " + result);
            } else if ("GETF".equals(cmd)) {
                String result = execServiceCall(7, dev, fid, -1);
                out.println("OK " + result);
            } else {
                out.println("ERR unknown command: " + cmd);
            }

        } catch (Exception e) {
            System.out.println("[HelperDaemon] Client error: " + e.getMessage());
        } finally {
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private static String execServiceCall(int tx, int dev, int fid, int val) {
        try {
            String cmd;
            if (tx == 6) {
                cmd = "service call autoservice " + tx + " i32 " + dev + " i32 " + fid + " i32 " + val;
            } else {
                cmd = "service call autoservice " + tx + " i32 " + dev + " i32 " + fid;
            }
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            p.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }
}
```

- [ ] **Step 3: 创建 HelperClient.java**

```java
package com.bydlauncher.helper;

import android.util.Log;

import com.bydlauncher.api.FidRegistry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class HelperClient {

    private static final String TAG = "HelperClient";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 19876;
    private static final int TIMEOUT = 3000;

    private boolean available = false;

    public HelperClient() {
        checkAvailability();
    }

    public void checkAvailability() {
        try (Socket s = new Socket()) {
            s.connect(new java.net.InetSocketAddress(HOST, PORT), 1000);
            available = true;
        } catch (Exception e) {
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public int getInt(int dev, int fid) {
        String resp = sendCommand("GET " + dev + " " + fid);
        return parseIntResponse(resp);
    }

    public float getFloat(int dev, int fid) {
        String resp = sendCommand("GETF " + dev + " " + fid);
        int bits = parseIntFromParcel(resp);
        return bits != -1 ? Float.intBitsToFloat(bits) : -1.0f;
    }

    public boolean setInt(int dev, int fid, int val) {
        String resp = sendCommand("SET " + dev + " " + fid + " " + val);
        return resp != null && resp.startsWith("OK");
    }

    private String sendCommand(String cmd) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(HOST, PORT), TIMEOUT);
            socket.setSoTimeout(TIMEOUT);
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            out.println(cmd);
            return in.readLine();
        } catch (Exception e) {
            Log.w(TAG, "Command failed: " + cmd, e);
            available = false;
            return null;
        }
    }

    private int parseIntResponse(String resp) {
        if (resp == null || !resp.startsWith("OK ")) return -1;
        String parcel = resp.substring(3);
        return parseIntFromParcel(parcel);
    }

    private int parseIntFromParcel(String resp) {
        if (resp == null || !resp.startsWith("OK ")) {
            return com.bydlauncher.api.AutoserviceClient.parseParcelInt(resp);
        }
        return com.bydlauncher.api.AutoserviceClient.parseParcelInt(resp.substring(3));
    }
}
```

- [ ] **Step 4: 修改 AdbHelper — 新增 startHelperDaemon 方法**

在 `AdbHelper.java` 中，`isAdbAvailable()` 方法之后添加：

```java
    public static void startHelperDaemon(Context context, Runnable onStarted) {
        new Thread(() -> {
            Socket socket = null;
            try {
                socket = connect();
                if (socket == null) {
                    Log.e(TAG, "Cannot start helper: ADB unavailable");
                    return;
                }

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                if (!authenticate(in, out, context)) {
                    Log.e(TAG, "Cannot start helper: ADB auth failed");
                    return;
                }

                // 找到 APK 路径
                String apkPath = context.getApplicationInfo().sourceDir;
                String cmd = "CLASSPATH=" + apkPath
                        + " nohup app_process /system/bin"
                        + " com.bydlauncher.helper.HelperDaemon"
                        + " > /dev/null 2>&1 &";

                String result = execShell(in, out, cmd);
                Log.i(TAG, "HelperDaemon start result: " + result);

                // 等待守护进程就绪
                Thread.sleep(1000);

                if (onStarted != null) onStarted.run();

            } catch (Exception e) {
                Log.e(TAG, "Failed to start HelperDaemon", e);
            } finally {
                if (socket != null) {
                    try { socket.close(); } catch (java.io.IOException ignored) {}
                }
            }
        }, "helper-start").start();
    }
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/bydlauncher/helper/
git add app/src/main/java/com/bydlauncher/api/AdbHelper.java
git commit -m "feat: add HelperDaemon (Socket-based), HelperClient, WriteAllowlist, ADB startup"
```

---

## Task 10: API 探测工具增强

**Files:**
- Modify: `app/src/main/java/com/bydlauncher/api/BydApiExplorer.java`
- Modify: `app/src/main/java/com/bydlauncher/ui/SettingsPage.java`

- [ ] **Step 1: 增强 BydApiExplorer — featureId 扫描 + 方法枚举 + 文件输出**

在 `BydApiExplorer.java` 中添加以下方法（在现有 `exploreAll()` 方法之后）：

```java
    private static final String[] KNOWN_DEVICE_CLASSES = {
            "android.hardware.bydauto.ac.BYDAutoAcDevice",
            "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice",
            "android.hardware.bydauto.doorlock.BYDAutoDoorLockDevice",
            "android.hardware.bydauto.statistic.BYDAutoStatisticDevice",
            "android.hardware.bydauto.vehicle.BYDAutoVehicleInfoDevice",
            "android.hardware.bydauto.drive.BYDAutoDriveDevice",
            "android.hardware.bydauto.tyre.BYDAutoTyreDevice",
            "android.hardware.bydauto.pm25.BYDAutoPm25Device",
            "android.hardware.bydauto.energy.BYDAutoEnergyDevice",
            "android.hardware.bydauto.power.BYDAutoPowerDevice",
            "android.hardware.bydauto.motor.BYDAutoMotorDevice",
            "android.hardware.bydauto.panorama.BYDAutoPanoramaDevice",
            "android.hardware.bydauto.gearbox.BYDAutoGearboxDevice",
    };

    public interface ProbeProgressListener {
        void onProgress(int current, int total, String message);
        void onComplete(String filePath);
    }

    public static void runFullProbe(android.content.Context context, ProbeProgressListener progressListener) {
        new Thread(() -> {
            StringBuilder report = new StringBuilder();
            report.append("=== BYD API Probe Report ===\n");
            report.append("Time: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()).format(new java.util.Date())).append("\n\n");

            int total = KNOWN_DEVICE_CLASSES.length;
            for (int i = 0; i < total; i++) {
                String className = KNOWN_DEVICE_CLASSES[i];
                String shortName = className.substring(className.lastIndexOf('.') + 1);

                if (progressListener != null) {
                    int idx = i;
                    progressListener.onProgress(idx + 1, total, "扫描 " + shortName);
                }

                report.append("--- ").append(shortName).append(" ---\n");

                try {
                    Class<?> clazz = Class.forName(className);
                    report.append("  状态: 可用\n");

                    // 枚举方法
                    java.lang.reflect.Method[] methods = clazz.getMethods();
                    report.append("  方法 (").append(methods.length).append("):\n");
                    for (java.lang.reflect.Method m : methods) {
                        if (m.getDeclaringClass() == Object.class) continue;
                        StringBuilder sig = new StringBuilder();
                        sig.append("    ").append(m.getReturnType().getSimpleName())
                                .append(" ").append(m.getName()).append("(");
                        Class<?>[] params = m.getParameterTypes();
                        for (int j = 0; j < params.length; j++) {
                            if (j > 0) sig.append(", ");
                            sig.append(params[j].getSimpleName());
                        }
                        sig.append(")\n");
                        report.append(sig);
                    }

                    // 尝试获取实例并调用无参 getter
                    Object device = ReflectionHelper.getDeviceInstance(className, context);
                    if (device != null) {
                        report.append("  无参 getter 返回值:\n");
                        for (java.lang.reflect.Method m : methods) {
                            if (m.getDeclaringClass() == Object.class) continue;
                            if (m.getParameterCount() != 0) continue;
                            String name = m.getName();
                            if (!name.startsWith("get") && !name.startsWith("is")
                                    && !name.startsWith("has")) continue;
                            try {
                                Object result = m.invoke(device);
                                report.append("    ").append(name).append("() = ").append(result).append("\n");
                            } catch (Exception e) {
                                report.append("    ").append(name).append("() = ERROR: ")
                                        .append(e.getCause() != null ? e.getCause().getMessage() : e.getMessage())
                                        .append("\n");
                            }
                        }
                    } else {
                        report.append("  getInstance 失败（权限不足）\n");
                    }

                } catch (ClassNotFoundException e) {
                    report.append("  状态: 不可用（类不存在）\n");
                }
                report.append("\n");
            }

            // 保存到文件
            String fileName = "bydui_probe_" + System.currentTimeMillis() + ".txt";
            String filePath = android.os.Environment.getExternalStorageDirectory()
                    + "/" + fileName;
            try {
                java.io.FileWriter writer = new java.io.FileWriter(filePath);
                writer.write(report.toString());
                writer.close();
                Log.i(TAG, "Probe report saved to " + filePath);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save probe report", e);
                filePath = "保存失败: " + e.getMessage();
            }

            // 同时输出到 logcat
            for (String line : report.toString().split("\n")) {
                Log.i(TAG, line);
            }

            if (progressListener != null) {
                String finalPath = filePath;
                progressListener.onComplete(finalPath);
            }
        }, "api-probe").start();
    }
```

- [ ] **Step 2: 修改 SettingsPage — 新增 API 探测按钮**

在 `SettingsPage.java` 中，找到合适的位置（设置 ADB 授权按钮的代码附近），添加 API 探测按钮的绑定逻辑。

在 SettingsPage 中需要有一个 `btn_api_probe` 按钮（需要在布局 XML 中添加，但布局修改不在当前 Task 范围内——此处先在 Java 中预留逻辑，布局按钮可后续添加）。

在 SettingsPage 构造函数或初始化方法中添加：

```java
        View btnApiProbe = rootView.findViewById(R.id.btn_api_probe);
        if (btnApiProbe != null) {
            btnApiProbe.setOnClickListener(v -> {
                android.widget.Toast.makeText(context, "正在扫描 API...", android.widget.Toast.LENGTH_SHORT).show();
                BydApiExplorer.runFullProbe(context, new BydApiExplorer.ProbeProgressListener() {
                    @Override
                    public void onProgress(int current, int total, String message) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            if (btnApiProbe instanceof android.widget.Button) {
                                ((android.widget.Button) btnApiProbe).setText(message + " (" + current + "/" + total + ")");
                            }
                        });
                    }

                    @Override
                    public void onComplete(String filePath) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            if (btnApiProbe instanceof android.widget.Button) {
                                ((android.widget.Button) btnApiProbe).setText("API 探测");
                            }
                            android.widget.Toast.makeText(context,
                                    "探测完成，报告保存到: " + filePath,
                                    android.widget.Toast.LENGTH_LONG).show();
                        });
                    }
                });
            });
        }
```

需要在文件顶部添加 import：
```java
import com.bydlauncher.api.BydApiExplorer;
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bydlauncher/api/BydApiExplorer.java
git add app/src/main/java/com/bydlauncher/ui/SettingsPage.java
git commit -m "feat: enhanced API probe with method enumeration, getter calls, file output"
```

---

## Task 11: 最终集成 — 四级数据通道优先级

**Files:**
- Modify: `app/src/main/java/com/bydlauncher/api/BydVehicleManager.java`

- [ ] **Step 1: 在 BydVehicleManager 中集成 HelperClient**

在字段声明中添加：

```java
    private final com.bydlauncher.helper.HelperClient helperClient;
```

在构造函数中，`autoserviceClient` 初始化之后添加：

```java
        this.helperClient = new com.bydlauncher.helper.HelperClient();
        if (!helperClient.isAvailable() && AdbHelper.isAdbAvailable()) {
            AdbHelper.startHelperDaemon(appContext, () -> helperClient.checkAvailability());
        }
```

- [ ] **Step 2: 在 readCurrentStatus 中实现四级通道降级**

将 `readCurrentStatus()` 方法中的 service call 补充数据部分改为优先使用 HelperClient：

将 Task 7 中添加的 `// ========== service call autoservice 补充数据 ==========` 代码块替换为：

```java
        // ========== 补充数据（HelperClient 优先 → AutoserviceClient 降级）==========
        try {
            if (helperClient.isAvailable()) {
                fillExtrasFromHelper(s);
            } else if (autoserviceClient.isAvailable()) {
                fillExtrasFromAutoservice(s);
            }
        } catch (Exception e) {
            Log.w(TAG, "Extra data fetch failed", e);
        }
```

在类末尾添加两个辅助方法：

```java
    private void fillExtrasFromHelper(VehicleStatus s) {
        int tempMax = helperClient.getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_BATT_TEMP_MAX);
        if (!FidRegistry.isSentinel(tempMax)) s.batteryTempMax = tempMax - 40;

        int tempMin = helperClient.getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_BATT_TEMP_MIN);
        if (!FidRegistry.isSentinel(tempMin)) s.batteryTempMin = tempMin - 40;

        int cellMax = helperClient.getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_CELL_VOLT_MAX);
        if (!FidRegistry.isSentinel(cellMax)) s.cellVoltageMax = cellMax;

        int cellMin = helperClient.getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_CELL_VOLT_MIN);
        if (!FidRegistry.isSentinel(cellMin)) s.cellVoltageMin = cellMin;

        int soh = helperClient.getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_SOH);
        if (!FidRegistry.isSentinel(soh)) s.soh = soh;

        float v12 = helperClient.getFloat(FidRegistry.DEV_BODYWORK, FidRegistry.FID_12V_VOLTAGE);
        if (!FidRegistry.isSentinelFloat(v12)) s.voltage12v = v12;

        int chargeGun = helperClient.getInt(FidRegistry.DEV_CHARGE, FidRegistry.FID_CHARGE_GUN);
        if (!FidRegistry.isSentinel(chargeGun)) s.chargeGunState = chargeGun;

        int powerState = helperClient.getInt(FidRegistry.DEV_POWER, FidRegistry.FID_POWER_STATE);
        if (!FidRegistry.isSentinel(powerState)) s.powerState = powerState;

        int driveMode = helperClient.getInt(FidRegistry.DEV_DRIVE_MODE, FidRegistry.FID_DRIVE_MODE);
        if (!FidRegistry.isSentinel(driveMode)) s.driveMode = driveMode;

        int motorPower = helperClient.getInt(FidRegistry.DEV_MOTOR, FidRegistry.FID_MOTOR_POWER);
        if (!FidRegistry.isSentinel(motorPower)) s.motorPowerKw = motorPower;
    }

    private void fillExtrasFromAutoservice(VehicleStatus s) {
        Map<String, Object> batteryExtras = autoserviceClient.readBatteryExtras();
        if (batteryExtras.containsKey("batteryTempMax"))
            s.batteryTempMax = (int) batteryExtras.get("batteryTempMax");
        if (batteryExtras.containsKey("batteryTempMin"))
            s.batteryTempMin = (int) batteryExtras.get("batteryTempMin");
        if (batteryExtras.containsKey("cellVoltageMax"))
            s.cellVoltageMax = (int) batteryExtras.get("cellVoltageMax");
        if (batteryExtras.containsKey("cellVoltageMin"))
            s.cellVoltageMin = (int) batteryExtras.get("cellVoltageMin");
        if (batteryExtras.containsKey("soh"))
            s.soh = (int) batteryExtras.get("soh");

        Map<String, Object> vehicleExtras = autoserviceClient.readVehicleExtras();
        if (vehicleExtras.containsKey("voltage12v"))
            s.voltage12v = (double) vehicleExtras.get("voltage12v");
        if (vehicleExtras.containsKey("chargeGunState"))
            s.chargeGunState = (int) vehicleExtras.get("chargeGunState");
        if (vehicleExtras.containsKey("powerState"))
            s.powerState = (int) vehicleExtras.get("powerState");
        if (vehicleExtras.containsKey("driveMode"))
            s.driveMode = (int) vehicleExtras.get("driveMode");
        if (vehicleExtras.containsKey("motorPowerKw"))
            s.motorPowerKw = (int) vehicleExtras.get("motorPowerKw");
    }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bydlauncher/api/BydVehicleManager.java
git commit -m "feat: integrate 4-tier data channel priority (listener > helper > reflection > adb)"
```

---

## Task 12: 最终检查 + 合并 commit

- [ ] **Step 1: 确认所有文件已创建**

```bash
ls -la app/src/main/java/com/bydlauncher/api/BydEnvironmentDetector.java
ls -la app/src/main/java/com/bydlauncher/api/PollState.java
ls -la app/src/main/java/com/bydlauncher/api/AutoserviceClient.java
ls -la app/src/main/java/com/bydlauncher/api/FidRegistry.java
ls -la app/src/main/java/com/bydlauncher/api/BydListenerManager.java
ls -la app/src/main/java/com/bydlauncher/helper/HelperDaemon.java
ls -la app/src/main/java/com/bydlauncher/helper/HelperClient.java
ls -la app/src/main/java/com/bydlauncher/helper/WriteAllowlist.java
```

- [ ] **Step 2: 确认 git 状态干净**

```bash
git status
git log --oneline -12
```

预期看到 11 个有序 commit，工作区无未提交更改。
