package com.bydlauncher.api;

import android.content.Context;
import android.util.Log;

public class BydAcApi {

    private static final String TAG = "BydAcApi";
    private static final String CLASS_NAME = "android.hardware.bydauto.ac.BYDAutoAcDevice";
    private static final int DEVICE_TYPE = 1000;

    public static final int SOURCE_UI = 0;
    public static final int SOURCE_VOICE = 1;

    public static final int ZONE_MAIN = 1;
    public static final int ZONE_DEPUTY = 2;
    public static final int ZONE_REAR = 3;
    public static final int ZONE_OUTSIDE = 4;

    public static final int CYCLE_INNER = 0;
    public static final int CYCLE_OUTER = 1;

    public static final int MODE_AUTO = 0;
    public static final int MODE_MANUAL = 1;

    public static final int WIND_MODE_DEFROST = 0;
    public static final int WIND_MODE_FACE = 1;
    public static final int WIND_MODE_FOOT = 5;

    private final Object device;
    private final boolean simulation;

    private boolean simAcOn = true;
    private int simMainTemp = 25;
    private int simOutsideTemp = 26;
    private int simWindLevel = 3;
    private int simWindMode = WIND_MODE_FACE;
    private int simCycleMode = CYCLE_INNER;
    private int simControlMode = MODE_AUTO;

    public BydAcApi(Context context) {
        this.device = ReflectionHelper.getDeviceInstance(CLASS_NAME, context);
        this.simulation = (device == null);
        if (simulation) {
            Log.i(TAG, "Running in simulation mode");
        }
    }

    public boolean isAvailable() {
        return device != null || simulation;
    }

    public int getStartState() {
        if (simulation) return simAcOn ? 1 : 0;
        return ReflectionHelper.invokeIntMethod(device, "getAcStartState");
    }

    public boolean isOn() {
        return getStartState() == 1;
    }

    public int getControlMode() {
        if (simulation) return simControlMode;
        return ReflectionHelper.invokeIntMethod(device, "getAcControlMode");
    }

    public int getWindLevel() {
        if (simulation) return simWindLevel;
        return ReflectionHelper.invokeIntMethod(device, "getAcWindLevel");
    }

    public int getWindMode() {
        if (simulation) return simWindMode;
        return ReflectionHelper.invokeIntMethod(device, "getAcWindMode");
    }

    public int getCycleMode() {
        if (simulation) return simCycleMode;
        return ReflectionHelper.invokeIntMethod(device, "getAcCycleMode");
    }

    public int getTemperature(int zone) {
        if (simulation) {
            return zone == ZONE_OUTSIDE ? simOutsideTemp : simMainTemp;
        }
        return ReflectionHelper.invokeIntMethod(device, "getTemprature",
                new Class<?>[]{int.class}, new Object[]{zone});
    }

    public int getMainTemp() {
        return getTemperature(ZONE_MAIN);
    }

    public int getOutsideTemp() {
        return getTemperature(ZONE_OUTSIDE);
    }

    public int getCompressorMode() {
        if (simulation) return 0;
        return ReflectionHelper.invokeIntMethod(device, "getAcCompressorMode");
    }

    public int getMaxCoolingState() {
        if (simulation) return 0;
        return ReflectionHelper.invokeIntMethod(device, "getAcMaxCoolingState");
    }

    public int getVentilationState() {
        if (simulation) return 0;
        return ReflectionHelper.invokeIntMethod(device, "getAcVentilationState");
    }

    public int getDefrostOnlineState() {
        if (simulation) return 0;
        return ReflectionHelper.invokeIntMethod(device, "getAcDefrostOnlineState");
    }

    public int start() {
        if (simulation) { simAcOn = true; return 0; }
        return ReflectionHelper.invokeVoidMethod(device, "start",
                new Class<?>[]{int.class}, new Object[]{SOURCE_VOICE});
    }

    public int stop() {
        if (simulation) { simAcOn = false; return 0; }
        return ReflectionHelper.invokeVoidMethod(device, "stop",
                new Class<?>[]{int.class}, new Object[]{SOURCE_VOICE});
    }

    public int toggle() {
        return isOn() ? stop() : start();
    }

    public int setTemperature(int zone, int tempCelsius) {
        if (simulation) {
            if (zone == ZONE_MAIN) simMainTemp = Math.max(17, Math.min(33, tempCelsius));
            return 0;
        }
        return ReflectionHelper.invokeVoidMethod(device, "setAcTemperature",
                new Class<?>[]{int.class, int.class, int.class, int.class},
                new Object[]{zone, tempCelsius, SOURCE_VOICE, 1});
    }

    public int setMainTemp(int tempCelsius) {
        return setTemperature(ZONE_MAIN, Math.max(17, Math.min(33, tempCelsius)));
    }

    public int setWindLevel(int level) {
        if (simulation) { simWindLevel = Math.max(0, Math.min(7, level)); return 0; }
        if (device == null) return -1;
        return ReflectionHelper.setViaBaseClass(device, DEVICE_TYPE, 0x1DE0000C, Math.max(0, Math.min(7, level)));
    }

    public int setWindMode(int mode) {
        if (simulation) { simWindMode = mode; return 0; }
        return ReflectionHelper.invokeVoidMethod(device, "setAcWindMode",
                new Class<?>[]{int.class, int.class}, new Object[]{mode, SOURCE_VOICE});
    }

    public int setCycleMode(int mode) {
        if (simulation) { simCycleMode = mode; return 0; }
        return ReflectionHelper.invokeVoidMethod(device, "setAcCycleMode",
                new Class<?>[]{int.class, int.class}, new Object[]{mode, SOURCE_VOICE});
    }

    public int setControlMode(int mode) {
        if (simulation) { simControlMode = mode; return 0; }
        return ReflectionHelper.invokeVoidMethod(device, "setAcControlMode",
                new Class<?>[]{int.class, int.class}, new Object[]{mode, SOURCE_VOICE});
    }

    public boolean isRealDevice() {
        return device != null;
    }

    public int toggleCycleMode() {
        int current = getCycleMode();
        return setCycleMode(current == CYCLE_INNER ? CYCLE_OUTER : CYCLE_INNER);
    }
}
