package com.diui.launcher.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.diui.launcher.model.VehicleStatus;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BydVehicleManager {

    private static final String TAG = "BydVehicleManager";
    private static BydVehicleManager instance;

    // 用户手动设置的模拟模式开关（true=模拟模式，false=真车模式）
    private static boolean forceSimMode = false;

    private final BydAcApi acApi;
    private final BydBodyworkApi bodyworkApi;
    private final BydStatisticApi statisticApi;
    private final BydDoorLockApi doorLockApi;
    private final BydTireApi tireApi;
    private final BydDriveApi driveApi;
    private final AutoserviceClient autoserviceClient;
    private final com.diui.launcher.helper.HelperClient helperClient;
    private BydListenerManager listenerManager;
    private PollState currentPollState = PollState.PARKED;
    private int consecutiveFailures = 0;
    private static final int MAX_BACKOFF_INTERVAL = 60000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private VehicleStatusListener listener;
    private boolean polling = false;

    public interface VehicleStatusListener {
        void onStatusUpdated(VehicleStatus status);
    }

    private BydVehicleManager(Context context) {
        Context appContext = context.getApplicationContext();
        this.acApi = new BydAcApi(appContext);
        this.bodyworkApi = new BydBodyworkApi(appContext);
        this.statisticApi = new BydStatisticApi(appContext);
        this.doorLockApi = new BydDoorLockApi(appContext);
        this.tireApi = new BydTireApi(appContext);
        this.driveApi = new BydDriveApi(appContext);
        this.autoserviceClient = new AutoserviceClient(appContext);
        this.helperClient = new com.diui.launcher.helper.HelperClient();
        ensureHelperRunning(appContext);

        this.listenerManager = new BydListenerManager(new VehicleStatus());
        listenerManager.setCallback(() -> {
            if (listener != null) {
                handler.post(() -> listener.onStatusUpdated(listenerManager.getSharedStatus()));
            }
        });

        if (!forceSimMode) {
            Object bodyworkDev = ReflectionHelper.getDeviceInstance(
                    "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice", appContext);
            listenerManager.registerBodyworkListener(bodyworkDev);

            Object acDev = ReflectionHelper.getDeviceInstance(
                    "android.hardware.bydauto.ac.BYDAutoAcDevice", appContext);
            listenerManager.registerAcListener(acDev);
        }

        Log.i(TAG, "Initialized - AC:" + acApi.isRealDevice()
                + " Body:" + bodyworkApi.isAvailable()
                + " Stat:" + statisticApi.isAvailable()
                + " Lock:" + doorLockApi.isAvailable()
                + " Tire:" + tireApi.isRealDevice()
                + " Drive:" + driveApi.isRealDevice());
    }

    public static synchronized BydVehicleManager getInstance(Context context) {
        if (instance == null) {
            instance = new BydVehicleManager(context);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.stopPolling();
        }
        instance = null;
    }

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

    public static void setForceSimulation(boolean force) {
        forceSimMode = force;
    }

    public static boolean isForceSimulation() {
        return forceSimMode;
    }

    public BydAcApi getAcApi() { return acApi; }
    public BydBodyworkApi getBodyworkApi() { return bodyworkApi; }
    public BydStatisticApi getStatisticApi() { return statisticApi; }
    public BydDoorLockApi getDoorLockApi() { return doorLockApi; }
    public BydTireApi getTireApi() { return tireApi; }
    public BydDriveApi getDriveApi() { return driveApi; }
    public AutoserviceClient getAutoserviceClient() { return autoserviceClient; }

    public void setListener(VehicleStatusListener listener) {
        this.listener = listener;
    }

    private static final String HELPER_PREFS = "diui_helper";
    private static final String KEY_SPAWNED_VERSION = "spawned_version_code";
    private static final long NO_STORED_VERSION = -1L;

    /**
     * 确保 HelperDaemon 运行且与当前 app 版本一致：
     *   1. 若 daemon 已存活且版本匹配 → 复用。
     *   2. 若版本不匹配（app 更新过）→ kill 旧 daemon 再 spawn 新的。
     *   3. 同版本但未运行（重启后）→ 直接 spawn。
     * ADB 不可用时静默跳过（模拟器/未开 ADB）。
     */
    private void ensureHelperRunning(Context appContext) {
        AdbHelper.checkAvailableAsync(appContext, available -> {
            if (!available) {
                Log.i(TAG, "ADB unavailable, skip helper daemon");
                return;
            }
            ensureHelperRunningInternal(appContext);
        });
    }

    private void ensureHelperRunningInternal(Context appContext) {        long wantVersion = installedVersionCode(appContext);
        long spawnedVersion = appContext
                .getSharedPreferences(HELPER_PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_SPAWNED_VERSION, NO_STORED_VERSION);

        if (helperClient.isAvailable() && spawnedVersion == wantVersion) {
            Log.i(TAG, "HelperDaemon alive (v" + spawnedVersion + "), reuse");
            return;
        }

        if (spawnedVersion != NO_STORED_VERSION && spawnedVersion != wantVersion) {
            Log.i(TAG, "Stale helper (spawned=" + spawnedVersion + " want=" + wantVersion + "), killing");
            AdbHelper.killHelper(appContext, () -> {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                spawnAndRecord(appContext, wantVersion);
            });
        } else {
            spawnAndRecord(appContext, wantVersion);
        }
    }

    private void spawnAndRecord(Context appContext, long wantVersion) {
        AdbHelper.startHelperDaemon(appContext, () -> {
            // spawn 后等 binder 注册完成再 ping
            boolean alive = false;
            for (int i = 0; i < 10; i++) {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                helperClient.checkAvailability();
                if (helperClient.isAvailable()) { alive = true; break; }
            }
            if (alive) {
                appContext.getSharedPreferences(HELPER_PREFS, Context.MODE_PRIVATE)
                        .edit().putLong(KEY_SPAWNED_VERSION, wantVersion).apply();
                Log.i(TAG, "HelperDaemon spawned & alive (v" + wantVersion + ")");
            } else {
                Log.w(TAG, "HelperDaemon not reachable after spawn");
            }
        });
    }

    private long installedVersionCode(Context appContext) {
        try {
            return appContext.getPackageManager()
                    .getPackageInfo(appContext.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return NO_STORED_VERSION;
        }
    }

    private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor();

    public void startPolling() {
        if (polling) return;
        polling = true;
        scheduleNextPoll(0);
    }

    public void stopPolling() {
        polling = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void scheduleNextPoll(long delayMs) {
        handler.postDelayed(() -> {
            if (!polling) return;
            pollExecutor.execute(pollTask);
        }, delayMs);
    }

    private final Runnable pollTask = () -> {
        if (!polling) return;

        VehicleStatus status = readCurrentStatus();

        boolean dataValid = (status.speed >= 0 || status.batteryPercent >= 0);
        if (!dataValid) {
            consecutiveFailures++;
        } else {
            consecutiveFailures = 0;
        }

        currentPollState = PollState.classify(
                status.speed, status.gear, status.chargeGunState, dataValid);

        long interval = currentPollState.intervalMs;
        if (consecutiveFailures > 0) {
            interval = Math.min(
                    (long) (interval * Math.pow(1.5, consecutiveFailures)),
                    MAX_BACKOFF_INTERVAL);
        }

        final long nextDelay = interval;
        handler.post(() -> {
            if (listener != null) {
                listener.onStatusUpdated(status);
            }
            scheduleNextPoll(nextDelay);
        });
    };

    public VehicleStatus readCurrentStatus() {
        VehicleStatus s = new VehicleStatus();
        boolean hasAdb = AdbHelper.getSharedDadb() != null;

        try {
            // ========== 车身数据 (BydBodyworkApi) ==========
            // ADB 可用时跳过模拟模式的 API（模拟值会被 autoservice shell 覆盖前占位）
            if (bodyworkApi.isRealDevice()) {
                s.batteryPercent = bodyworkApi.getBatteryCapacity();
                s.powerLevel = bodyworkApi.getPowerLevel();
                s.powerLevelText = bodyworkApi.getPowerLevelText();
                s.isLocked = bodyworkApi.isLocked();
                s.doorLeftFrontOpen = bodyworkApi.isDoorOpen(BydBodyworkApi.DOOR_LEFT_FRONT);
                s.doorRightFrontOpen = bodyworkApi.isDoorOpen(BydBodyworkApi.DOOR_RIGHT_FRONT);
                s.doorLeftRearOpen = bodyworkApi.isDoorOpen(BydBodyworkApi.DOOR_LEFT_REAR);
                s.doorRightRearOpen = bodyworkApi.isDoorOpen(BydBodyworkApi.DOOR_RIGHT_REAR);
                s.trunkOpen = bodyworkApi.isDoorOpen(BydBodyworkApi.DOOR_TRUNK);
                s.hoodOpen = bodyworkApi.isDoorOpen(BydBodyworkApi.DOOR_HOOD);
                s.windowFL = bodyworkApi.getWindowFL();
                s.windowFR = bodyworkApi.getWindowFR();
                s.windowRL = bodyworkApi.getWindowRL();
                s.windowRR = bodyworkApi.getWindowRR();
                s.sunroofOpen = bodyworkApi.getSunroofState() == BydBodyworkApi.STATE_OPEN;
            }

            // ========== 空调数据 (BydAcApi) ==========
            if (acApi.isRealDevice()) {
                s.acOn = acApi.isOn();
                s.acTemp = acApi.getMainTemp();
                s.outsideTemp = acApi.getOutsideTemp();
                s.acWindLevel = acApi.getWindLevel();
                s.acWindMode = acApi.getWindMode();
                s.acCycleMode = acApi.getCycleMode();
                s.acControlMode = acApi.getControlMode();
            }

            // ========== 统计数据 (BydStatisticApi) ==========
            if (statisticApi.isAvailable() && !hasAdb) {
                s.elecPercent = statisticApi.getElecPercentage();
                s.evMileage = statisticApi.getEVMileage();
                s.totalMileage = statisticApi.getTotalMileage();
            }

            // ========== 胎压胎温 (BydTireApi) ==========
            if (tireApi.isRealDevice()) {
                s.tirePressureFL = tireApi.getPressureFL();
                s.tirePressureFR = tireApi.getPressureFR();
                s.tirePressureRL = tireApi.getPressureRL();
                s.tirePressureRR = tireApi.getPressureRR();
                s.tireTempFL = tireApi.getTempFL();
                s.tireTempFR = tireApi.getTempFR();
                s.tireTempRL = tireApi.getTempRL();
                s.tireTempRR = tireApi.getTempRR();
            }

            // ========== 行驶状态 (BydDriveApi) ==========
            if (driveApi.isRealDevice()) {
                s.speed = driveApi.getSpeed();
                s.gear = driveApi.getGear();
                s.powerKw = driveApi.getPowerKw();
                s.fuelPercent = driveApi.getFuelPercent();
                s.fuelAmount = driveApi.getFuelAmount();
                s.totalRange = driveApi.getTotalRange();
                s.hevMileage = driveApi.getHevMileage();
                s.currentElecConsumption = driveApi.getCurrentElecConsumption();
                s.currentFuelConsumption = driveApi.getCurrentFuelConsumption();
                s.avgElecConsumption = driveApi.getAvgElecConsumption();
                s.avgFuelConsumption = driveApi.getAvgFuelConsumption();
                s.tripDistance = driveApi.getTripDistance();
                s.tripTime = driveApi.getTripTime();
                s.tripElec = driveApi.getTripElecConsumption();
                s.tripFuel = driveApi.getTripFuelConsumption();
                s.smartChargePercent = driveApi.getSmartChargePercent();
                s.recoveryMode = driveApi.getRecoveryMode();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error reading vehicle status", e);
        }

        // ========== ADB shell 优先：通过 service call 读取真实数据 ==========
        if (AdbHelper.getSharedDadb() != null) {
            fillFromAutoserviceShell(s);
        }

        // ========== 补充数据（HelperClient 优先 → AutoserviceClient 降级）==========
        try {
            if (helperClient.isAvailable()) {
                fillExtrasFromHelper(s);
            } else if (AdbHelper.getSharedDadb() != null) {
                fillExtrasFromAutoservice(s);
            }
        } catch (Exception e) {
            Log.w(TAG, "Extra data fetch failed", e);
        }

        // ========== 仅对未拿到真实值的字段填模拟数据 ==========
        if (!driveApi.isRealDevice() && AdbHelper.getSharedDadb() == null) {
            fillDriveSimulationData(s);
        }
        if (!tireApi.isRealDevice() && AdbHelper.getSharedDadb() == null) {
            fillTireSimulationData(s);
        }

        return s;
    }

    /**
     * 补充行驶状态模拟数据（仅在 DriveApi 不可用时调用）
     */
    private void fillDriveSimulationData(VehicleStatus s) {
        s.speed = 0;
        s.gear = 0;
        s.powerKw = 0;
        if (s.fuelPercent < 0) s.fuelPercent = 37;
        if (s.fuelAmount < 0) s.fuelAmount = 21.0;
        if (s.totalRange < 0) s.totalRange = 396;
        if (s.hevMileage < 0) s.hevMileage = 18563.0;
        if (s.currentElecConsumption < 0) s.currentElecConsumption = 16.0;
        if (s.currentFuelConsumption < 0) s.currentFuelConsumption = 5.7;
        if (s.avgElecConsumption < 0) s.avgElecConsumption = 25.4;
        if (s.avgFuelConsumption < 0) s.avgFuelConsumption = 0;
        if (s.tripDistance <= 0) s.tripDistance = 0;
        if (s.tripTime == null || s.tripTime.equals("00:00")) s.tripTime = "00:00";
        s.tripElec = 0;
        s.tripFuel = 0;
        s.smartChargePercent = 25;
        s.recoveryMode = "最大回收";
    }

    /**
     * 补充胎压胎温模拟数据（仅在 TireApi 不可用时调用）
     */
    private void fillTireSimulationData(VehicleStatus s) {
        if (s.tirePressureFL < 0) s.tirePressureFL = 250;
        if (s.tirePressureFR < 0) s.tirePressureFR = 252;
        if (s.tirePressureRL < 0) s.tirePressureRL = 250;
        if (s.tirePressureRR < 0) s.tirePressureRR = 250;
        if (s.tireTempFL < 0) s.tireTempFL = 31;
        if (s.tireTempFR < 0) s.tireTempFR = 31;
        if (s.tireTempRL < 0) s.tireTempRL = 30;
        if (s.tireTempRR < 0) s.tireTempRR = 33;
    }

    private void fillExtrasFromHelper(VehicleStatus s) {
        int tempMax = helperClient.getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_BATT_TEMP_MAX);
        if (!FidRegistry.isSentinel(tempMax) && tempMax >= 0) s.batteryTempMax = tempMax - 40;

        int tempMin = helperClient.getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_BATT_TEMP_MIN);
        if (!FidRegistry.isSentinel(tempMin) && tempMin >= 0) s.batteryTempMin = tempMin - 40;

        int cellMax = helperClient.getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_CELL_VOLT_MAX);
        if (!FidRegistry.isSentinel(cellMax) && cellMax > 0) s.cellVoltageMax = cellMax;

        int cellMin = helperClient.getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_CELL_VOLT_MIN);
        if (!FidRegistry.isSentinel(cellMin) && cellMin > 0) s.cellVoltageMin = cellMin;

        int soh = helperClient.getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_SOH);
        if (!FidRegistry.isSentinel(soh) && soh >= 0) s.soh = soh;

        float v12 = helperClient.getFloat(FidRegistry.DEV_BODYWORK, FidRegistry.FID_12V_VOLTAGE);
        if (!FidRegistry.isSentinelFloat(v12) && v12 > 0) s.voltage12v = v12;

        int chargeGun = helperClient.getInt(FidRegistry.DEV_CHARGE, FidRegistry.FID_CHARGE_GUN);
        if (!FidRegistry.isSentinel(chargeGun)) s.chargeGunState = chargeGun;

        int powerState = helperClient.getInt(FidRegistry.DEV_POWER, FidRegistry.FID_POWER_STATE);
        if (!FidRegistry.isSentinel(powerState)) s.powerState = powerState;

        int driveMode = helperClient.getInt(FidRegistry.DEV_DRIVE_MODE, FidRegistry.FID_DRIVE_MODE);
        if (!FidRegistry.isSentinel(driveMode)) s.driveMode = driveMode;

        int motorPower = helperClient.getInt(FidRegistry.DEV_MOTOR, FidRegistry.FID_MOTOR_POWER);
        if (!FidRegistry.isSentinel(motorPower)) s.motorPowerKw = motorPower;
    }

    /**
     * 通过 ADB shell 执行 service call autoservice 读取核心车辆数据。
     * 当 BYD 反射 API 因 signature 权限返回 -1 时，作为降级通道使用。
     */
    private void fillFromAutoserviceShell(VehicleStatus s) {
        try {
            // AC
            int acState = autoserviceClient.getAcState();
            if (!FidRegistry.isSentinel(acState) && acState >= 0) {
                s.acOn = (acState == 1);
            }
            int acTemp = autoserviceClient.getAcTemp();
            if (!FidRegistry.isSentinel(acTemp) && acTemp > 0 && acTemp < 100) {
                s.acTemp = (acTemp > 30) ? acTemp / 2 : acTemp;
            }
            int outsideTemp = autoserviceClient.getOutsideTemp();
            if (!FidRegistry.isSentinel(outsideTemp) && outsideTemp > -50 && outsideTemp < 80) {
                s.outsideTemp = outsideTemp;
            }
            int fanLevel = autoserviceClient.getFanLevel();
            if (!FidRegistry.isSentinel(fanLevel) && fanLevel >= 0 && fanLevel <= 7) {
                s.acWindLevel = fanLevel;
            }
            int acCycle = autoserviceClient.getAcCycle();
            if (!FidRegistry.isSentinel(acCycle) && acCycle >= 0) {
                s.acCycleMode = acCycle;
            }

            // 电量（SOC，float）
            int soc = autoserviceClient.getBatteryCapacity();
            if (soc >= 0 && soc <= 100) {
                s.batteryPercent = soc;
            }

            // 里程
            int mileage = autoserviceClient.getMileage();
            if (!FidRegistry.isSentinel(mileage) && mileage > 0) {
                s.totalMileage = mileage;
            }

            // 车门
            int doorFL = autoserviceClient.getDoorState(FidRegistry.FID_DOOR_FL);
            if (!FidRegistry.isSentinel(doorFL)) s.doorLeftFrontOpen = (doorFL == 1);
            int doorFR = autoserviceClient.getDoorState(FidRegistry.FID_DOOR_FR);
            if (!FidRegistry.isSentinel(doorFR)) s.doorRightFrontOpen = (doorFR == 1);
            int doorRL = autoserviceClient.getDoorState(FidRegistry.FID_DOOR_RL);
            if (!FidRegistry.isSentinel(doorRL)) s.doorLeftRearOpen = (doorRL == 1);
            int doorRR = autoserviceClient.getDoorState(FidRegistry.FID_DOOR_RR);
            if (!FidRegistry.isSentinel(doorRR)) s.doorRightRearOpen = (doorRR == 1);

            // 引擎盖 / 后备箱
            int hood = autoserviceClient.getHoodState();
            if (!FidRegistry.isSentinel(hood)) s.hoodOpen = (hood == 1);
            int trunk = autoserviceClient.getTrunkState();
            if (!FidRegistry.isSentinel(trunk)) s.trunkOpen = (trunk == 1);

            // 门锁
            int lock = autoserviceClient.getLockState();
            if (!FidRegistry.isSentinel(lock)) s.isLocked = (lock == 1);

            // 车窗
            int winFL = autoserviceClient.getWindowState(FidRegistry.FID_WINDOW_FL);
            if (!FidRegistry.isSentinel(winFL)) s.windowFL = winFL;
            int winFR = autoserviceClient.getWindowState(FidRegistry.FID_WINDOW_FR);
            if (!FidRegistry.isSentinel(winFR)) s.windowFR = winFR;
            int winRL = autoserviceClient.getWindowState(FidRegistry.FID_WINDOW_RL);
            if (!FidRegistry.isSentinel(winRL)) s.windowRL = winRL;
            int winRR = autoserviceClient.getWindowState(FidRegistry.FID_WINDOW_RR);
            if (!FidRegistry.isSentinel(winRR)) s.windowRR = winRR;

            // 速度（float）
            float speed = autoserviceClient.getSpeed();
            if (!FidRegistry.isSentinelFloat(speed) && speed >= 0) {
                s.speed = Math.round(speed);
            }

            // 挡位
            int gear = autoserviceClient.getGear();
            if (!FidRegistry.isSentinel(gear) && gear >= 0) s.gear = gear;

            // 电机功率
            int motorPow = autoserviceClient.getMotorPower();
            if (!FidRegistry.isSentinel(motorPow)) s.motorPowerKw = motorPow;

            // 胎压
            int tireFL = autoserviceClient.getInt(FidRegistry.DEV_TIRE, FidRegistry.FID_TIRE_FL);
            if (!FidRegistry.isSentinel(tireFL) && tireFL > 0) s.tirePressureFL = tireFL;
            int tireFR = autoserviceClient.getInt(FidRegistry.DEV_TIRE, FidRegistry.FID_TIRE_FR);
            if (!FidRegistry.isSentinel(tireFR) && tireFR > 0) s.tirePressureFR = tireFR;
            int tireRL = autoserviceClient.getInt(FidRegistry.DEV_TIRE, FidRegistry.FID_TIRE_RL);
            if (!FidRegistry.isSentinel(tireRL) && tireRL > 0) s.tirePressureRL = tireRL;
            int tireRR = autoserviceClient.getInt(FidRegistry.DEV_TIRE, FidRegistry.FID_TIRE_RR);
            if (!FidRegistry.isSentinel(tireRR) && tireRR > 0) s.tirePressureRR = tireRR;
        } catch (Exception e) {
            Log.w(TAG, "autoservice shell fallback failed", e);
        }
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

    /**
     * 诊断报告：汇总所有 API 初始化状态、forceSimMode、设备对象、实际数据读取结果。
     * 必须在后台线程调用。
     */
    public String buildDiagReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 真车模式诊断 =====\n\n");

        sb.append("forceSimMode: ").append(forceSimMode).append("\n\n");

        sb.append("── API 初始化状态 ──\n");
        sb.append("  AcApi:        real=").append(acApi.isRealDevice())
          .append("  available=").append(acApi.isAvailable()).append("\n");
        sb.append("  BodyworkApi:  real=").append(bodyworkApi.isRealDevice())
          .append("  available=").append(bodyworkApi.isAvailable()).append("\n");
        sb.append("  StatisticApi: available=").append(statisticApi.isAvailable()).append("\n");
        sb.append("  DoorLockApi:  available=").append(doorLockApi.isAvailable()).append("\n");
        sb.append("  TireApi:      real=").append(tireApi.isRealDevice())
          .append("  available=").append(tireApi.isAvailable()).append("\n");
        sb.append("  DriveApi:     real=").append(driveApi.isRealDevice())
          .append("  available=").append(driveApi.isAvailable()).append("\n");
        sb.append("  HelperClient: available=").append(helperClient.isAvailable()).append("\n");
        sb.append("  Autoservice:  available=").append(autoserviceClient.isAvailable()).append("\n");
        sb.append("  Dadb:         ").append(AdbHelper.getSharedDadb() != null ? "已连接" : "未连接").append("\n");

        // ── service call autoservice 原始测试 ──
        sb.append("\n── service call autoservice 测试 ──\n");
        dadb.Dadb dadb = AdbHelper.getSharedDadb();
        if (dadb == null) {
            sb.append("  ✗ Dadb 未连接，无法测试\n");
        } else {
            String[][] tests = {
                {"AC 开关",     "5", String.valueOf(FidRegistry.DEV_AC),      String.valueOf(FidRegistry.FID_AC_STATE)},
                {"AC 温度",     "5", String.valueOf(FidRegistry.DEV_AC),      String.valueOf(FidRegistry.FID_AC_TEMP)},
                {"车外温度",    "5", String.valueOf(FidRegistry.DEV_AC),      String.valueOf(FidRegistry.FID_OUTSIDE_TEMP)},
                {"风量",        "5", String.valueOf(FidRegistry.DEV_AC),      String.valueOf(FidRegistry.FID_AC_FAN)},
                {"电池电量",    "7", String.valueOf(FidRegistry.DEV_STATISTIC), String.valueOf(FidRegistry.FID_SOC)},
                {"SOH",         "5", String.valueOf(FidRegistry.DEV_STATISTIC), String.valueOf(FidRegistry.FID_SOH)},
                {"车门左前",    "5", String.valueOf(FidRegistry.DEV_BODYWORK),String.valueOf(FidRegistry.FID_DOOR_FL)},
                {"车门右前",    "5", String.valueOf(FidRegistry.DEV_BODYWORK),String.valueOf(FidRegistry.FID_DOOR_FR)},
                {"速度",        "7", String.valueOf(FidRegistry.DEV_SPEED),   String.valueOf(FidRegistry.FID_SPEED)},
                {"挡位",        "5", String.valueOf(FidRegistry.DEV_GEARBOX), String.valueOf(FidRegistry.FID_GEAR)},
                {"胎压左前",    "5", String.valueOf(FidRegistry.DEV_TIRE),    String.valueOf(FidRegistry.FID_TIRE_FL)},
                {"12V电压",     "7", String.valueOf(FidRegistry.DEV_BODYWORK),String.valueOf(FidRegistry.FID_12V_VOLTAGE)},
                {"充电枪",      "5", String.valueOf(FidRegistry.DEV_CHARGE),  String.valueOf(FidRegistry.FID_CHARGE_GUN)},
                {"电源状态",    "5", String.valueOf(FidRegistry.DEV_POWER),   String.valueOf(FidRegistry.FID_POWER_STATE)},
            };

            for (String[] t : tests) {
                String label = t[0];
                String tx = t[1];
                String dev = t[2];
                String fid = t[3];
                String cmd = "service call autoservice " + tx + " i32 " + dev + " i32 " + fid;
                try {
                    dadb.AdbShellResponse resp = dadb.shell(cmd);
                    String raw = resp.getAllOutput().trim();
                    // 解析值
                    String parsed;
                    if ("7".equals(tx)) {
                        float val = AutoserviceClient.parseParcelFloat(raw);
                        parsed = String.valueOf(val);
                    } else {
                        int val = AutoserviceClient.parseParcelInt(raw);
                        if (FidRegistry.isSentinel(val)) {
                            parsed = val + " (哨兵: " + describeSentinel(val) + ")";
                        } else {
                            parsed = String.valueOf(val);
                        }
                    }
                    sb.append("  ").append(label).append(": ")
                      .append(parsed)
                      .append("  [").append(raw.replace("\n", " ")).append("]\n");
                } catch (Exception e) {
                    sb.append("  ").append(label).append(": ✗ ").append(e.getMessage()).append("\n");
                }
            }
        }

        // ── Polling 状态 ──
        sb.append("\n── Polling 状态 ──\n");
        sb.append("  polling: ").append(polling).append("\n");
        sb.append("  pollState: ").append(currentPollState).append("\n");
        sb.append("  consecutiveFailures: ").append(consecutiveFailures).append("\n");

        sb.append("\n===== 诊断完成 =====");
        return sb.toString();
    }

    private static String describeSentinel(int val) {
        if (val == FidRegistry.SENTINEL_NO_CAN) return "无CAN信号";
        if (val == FidRegistry.SENTINEL_UNINIT) return "未初始化";
        if (val == FidRegistry.SENTINEL_BAD_TX) return "错误事务码";
        if (val == FidRegistry.SENTINEL_NO_WRITE) return "未注册";
        return "未知";
    }
}
