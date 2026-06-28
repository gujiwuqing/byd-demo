package com.bydlauncher.api;

import android.content.Context;
import android.util.Log;

public class BydBodyworkApi {

    private static final String TAG = "BydBodyworkApi";
    private static final String CLASS_NAME = "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice";

    public static final int DOOR_LEFT_FRONT = 1;
    public static final int DOOR_RIGHT_FRONT = 2;
    public static final int DOOR_LEFT_REAR = 3;
    public static final int DOOR_RIGHT_REAR = 4;
    public static final int DOOR_HOOD = 5;
    public static final int DOOR_TRUNK = 6;
    public static final int DOOR_FUEL_CAP = 7;

    public static final int STATE_CLOSED = 0;
    public static final int STATE_OPEN = 1;

    public static final int POWER_OFF = 0;
    public static final int POWER_ACC = 1;
    public static final int POWER_ON = 2;
    public static final int POWER_READY = 3;

    public static final int SYSTEM_NORMAL = 0;
    public static final int SYSTEM_SECURED = 1;

    private final Object device;
    private final boolean simulation;

    private int simBattery = 78;
    private int simPowerLevel = POWER_READY;
    private boolean simLocked = true;

    public BydBodyworkApi(Context context) {
        this.device = ReflectionHelper.getDeviceInstance(CLASS_NAME, context);
        this.simulation = (device == null);
        if (simulation) {
            Log.i(TAG, "Running in simulation mode");
        }
    }

    public boolean isAvailable() {
        return device != null || simulation;
    }

    public int getDoorState(int area) {
        if (simulation) return STATE_CLOSED;
        return ReflectionHelper.invokeIntMethod(device, "getDoorState",
                new Class<?>[]{int.class}, new Object[]{area});
    }

    public boolean isDoorOpen(int area) {
        return getDoorState(area) == STATE_OPEN;
    }

    public int getAutoSystemState() {
        if (simulation) return simLocked ? SYSTEM_SECURED : SYSTEM_NORMAL;
        return ReflectionHelper.invokeIntMethod(device, "getAutoSystemState");
    }

    public boolean isLocked() {
        return getAutoSystemState() == SYSTEM_SECURED;
    }

    public void toggleLock() {
        if (simulation) {
            simLocked = !simLocked;
        }
    }

    public int getPowerLevel() {
        if (simulation) return simPowerLevel;
        return ReflectionHelper.invokeIntMethod(device, "getPowerLevel");
    }

    public String getPowerLevelText() {
        switch (getPowerLevel()) {
            case POWER_OFF: return "OFF";
            case POWER_ACC: return "ACC";
            case POWER_ON: return "ON";
            case POWER_READY: return "READY";
            default: return "N/A";
        }
    }

    public String getVIN() {
        if (simulation) return "SIM0000000000000";
        return ReflectionHelper.invokeStringMethod(device, "getAutoVIN");
    }

    public int getBatteryCapacity() {
        if (simulation) return simBattery;
        return ReflectionHelper.invokeIntMethod(device, "getBatteryCapacity");
    }

    public int getWindowState(int area) {
        if (simulation) return STATE_CLOSED;
        return ReflectionHelper.invokeIntMethod(device, "getWindowState",
                new Class<?>[]{int.class}, new Object[]{area});
    }

    public int getSunroofState() {
        if (simulation) return STATE_CLOSED;
        return ReflectionHelper.invokeIntMethod(device, "getSunroofState");
    }
}
