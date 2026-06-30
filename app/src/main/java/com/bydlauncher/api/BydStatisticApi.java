package com.bydlauncher.api;

import android.content.Context;
import android.util.Log;

public class BydStatisticApi {

    private static final String TAG = "BydStatisticApi";
    private static final String CLASS_NAME = "android.hardware.bydauto.statistic.BYDAutoStatisticDevice";

    private final Object device;
    private final boolean simulation;

    private final double simElecPercent = 78.5;
    private final int simEvMileage = 326;
    private final double simTotalMileage = 15832.6;

    public BydStatisticApi(Context context) {
        if (BydVehicleManager.isForceSimulation()) {
            this.device = null;
            this.simulation = true;
        } else {
            this.device = ReflectionHelper.getDeviceInstance(CLASS_NAME, context);
            this.simulation = (device == null);
        }
        if (simulation) {
            Log.i(TAG, "Running in simulation mode");
        }
    }

    public boolean isAvailable() {
        return device != null || simulation;
    }

    public double getElecPercentage() {
        if (simulation) return simElecPercent;
        return ReflectionHelper.invokeDoubleMethod(device, "getElecPercentageValue");
    }

    public int getEVMileage() {
        if (simulation) return simEvMileage;
        return ReflectionHelper.invokeIntMethod(device, "getEVMileageValue");
    }

    public double getTotalElecConsumption() {
        if (simulation) return 16.8;
        return ReflectionHelper.invokeDoubleMethod(device, "getTotalElecConValue");
    }

    public double getTotalFuelConsumption() {
        if (simulation) return 0;
        return ReflectionHelper.invokeDoubleMethod(device, "getTotalFuelConValue");
    }

    public double getTotalMileage() {
        if (simulation) return simTotalMileage;
        return ReflectionHelper.invokeDoubleMethod(device, "getTotalMileageValue");
    }

    public int getVehicleType() {
        if (simulation) return 1;
        return ReflectionHelper.invokeIntMethod(device, "getType");
    }
}
