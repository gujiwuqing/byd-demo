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
        Log.i(TAG, "Initialized - AC:" + acApi.isAvailable()
                + " Body:" + bodyworkApi.isAvailable()
                + " Stat:" + statisticApi.isAvailable()
                + " Lock:" + doorLockApi.isAvailable());
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
            }
            if (acApi.isAvailable()) {
                s.acOn = acApi.isOn();
                s.acTemp = acApi.getMainTemp();
                s.outsideTemp = acApi.getOutsideTemp();
                s.acWindLevel = acApi.getWindLevel();
                s.acWindMode = acApi.getWindMode();
                s.acCycleMode = acApi.getCycleMode();
                s.acControlMode = acApi.getControlMode();
            }
            if (statisticApi.isAvailable()) {
                s.elecPercent = statisticApi.getElecPercentage();
                s.evMileage = statisticApi.getEVMileage();
                s.totalMileage = statisticApi.getTotalMileage();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading vehicle status", e);
        }

        // 模拟模式下补充额外数据
        if (!acApi.isRealDevice()) {
            fillSimulationData(s);
        }

        return s;
    }

    private void fillSimulationData(VehicleStatus s) {
        s.fuelPercent = 37;
        s.fuelAmount = 21.0;
        s.totalRange = 396;
        s.hevMileage = 18563.0;
        s.currentElecConsumption = 16.0;
        s.currentFuelConsumption = 5.7;
        s.avgElecConsumption = 25.4;
        s.avgFuelConsumption = 0;
        s.speed = 0;
        s.gear = 0;
        s.powerKw = 0;
        s.outsideTemp = 38;

        s.tirePressureFL = 250;
        s.tirePressureFR = 252;
        s.tirePressureRL = 250;
        s.tirePressureRR = 250;
        s.tireTempFL = 31;
        s.tireTempFR = 31;
        s.tireTempRL = 30;
        s.tireTempRR = 33;

        s.tripDistance = 0;
        s.tripTime = "00:00";
        s.tripElec = 0;
        s.tripFuel = 0;

        s.smartChargePercent = 25;
        s.recoveryMode = "最大回收";
    }
}
