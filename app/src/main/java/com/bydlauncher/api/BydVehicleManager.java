package com.bydlauncher.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bydlauncher.model.VehicleStatus;

public class BydVehicleManager {

    private static final String TAG = "BydVehicleManager";
    private static BydVehicleManager instance;

    private final BydAcApi acApi;
    private final BydBodyworkApi bodyworkApi;
    private final BydStatisticApi statisticApi;
    private final BydDoorLockApi doorLockApi;
    private final BydTireApi tireApi;
    private final BydDriveApi driveApi;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private VehicleStatusListener listener;
    private boolean polling = false;
    private static final long POLL_INTERVAL = 2000;

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

    public BydAcApi getAcApi() { return acApi; }
    public BydBodyworkApi getBodyworkApi() { return bodyworkApi; }
    public BydStatisticApi getStatisticApi() { return statisticApi; }
    public BydDoorLockApi getDoorLockApi() { return doorLockApi; }
    public BydTireApi getTireApi() { return tireApi; }
    public BydDriveApi getDriveApi() { return driveApi; }

    public void setListener(VehicleStatusListener listener) {
        this.listener = listener;
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
            if (listener != null) {
                listener.onStatusUpdated(status);
            }
            handler.postDelayed(this, POLL_INTERVAL);
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
}
