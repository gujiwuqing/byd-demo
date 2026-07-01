package com.diui.launcher.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.diui.launcher.model.VehicleStatus;

import java.util.Map;

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
        if (!AdbHelper.isAdbAvailable()) {
            Log.i(TAG, "ADB unavailable, skip helper daemon");
            return;
        }

        long wantVersion = installedVersionCode(appContext);
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

    public void startPolling() {
        if (polling) return;
        polling = true;
        handler.post(pollRunnable);
    }

    public void stopPolling() {
        polling = false;
        handler.removeCallbacks(pollRunnable);
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
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

            if (listener != null) {
                listener.onStatusUpdated(status);
            }

            long interval = currentPollState.intervalMs;
            if (consecutiveFailures > 0) {
                interval = Math.min(
                        (long) (interval * Math.pow(1.5, consecutiveFailures)),
                        MAX_BACKOFF_INTERVAL);
            }

            handler.postDelayed(this, interval);
        }
    };

    public VehicleStatus readCurrentStatus() {
        VehicleStatus s = new VehicleStatus();
        try {
            // ========== 车身数据 (BydBodyworkApi) ==========
            if (bodyworkApi.isAvailable()) {
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

                // 车窗状态
                s.windowFL = bodyworkApi.getWindowFL();
                s.windowFR = bodyworkApi.getWindowFR();
                s.windowRL = bodyworkApi.getWindowRL();
                s.windowRR = bodyworkApi.getWindowRR();
                s.sunroofOpen = bodyworkApi.getSunroofState() == BydBodyworkApi.STATE_OPEN;
            }

            // ========== 空调数据 (BydAcApi) ==========
            if (acApi.isAvailable()) {
                s.acOn = acApi.isOn();
                s.acTemp = acApi.getMainTemp();
                s.outsideTemp = acApi.getOutsideTemp();
                s.acWindLevel = acApi.getWindLevel();
                s.acWindMode = acApi.getWindMode();
                s.acCycleMode = acApi.getCycleMode();
                s.acControlMode = acApi.getControlMode();
            }

            // ========== 统计数据 (BydStatisticApi) ==========
            if (statisticApi.isAvailable()) {
                s.elecPercent = statisticApi.getElecPercentage();
                s.evMileage = statisticApi.getEVMileage();
                s.totalMileage = statisticApi.getTotalMileage();
            }

            // ========== 胎压胎温 (BydTireApi) ==========
            if (tireApi.isAvailable()) {
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
            if (driveApi.isAvailable()) {
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

        // 当 DriveApi 或 TireApi 不可用时，补充模拟数据
        if (!driveApi.isRealDevice()) {
            fillDriveSimulationData(s);
        }
        if (!tireApi.isRealDevice()) {
            fillTireSimulationData(s);
        }

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
        int tempMax = helperClient.getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_BATT_TEMP_MAX);
        if (!FidRegistry.isSentinel(tempMax) && tempMax >= 0) s.batteryTempMax = tempMax - 40;

        int tempMin = helperClient.getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_BATT_TEMP_MIN);
        if (!FidRegistry.isSentinel(tempMin) && tempMin >= 0) s.batteryTempMin = tempMin - 40;

        int cellMax = helperClient.getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_CELL_VOLT_MAX);
        if (!FidRegistry.isSentinel(cellMax) && cellMax > 0) s.cellVoltageMax = cellMax;

        int cellMin = helperClient.getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_CELL_VOLT_MIN);
        if (!FidRegistry.isSentinel(cellMin) && cellMin > 0) s.cellVoltageMin = cellMin;

        int soh = helperClient.getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_SOH);
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
}
