package com.diui.launcher.api;

import android.content.Context;
import android.util.Log;

import dadb.AdbShellResponse;

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
        // 走 ADB shell setInt，BYD API 服务端校验权限会拒绝
        int r = adbWrite(FidRegistry.WFID_AC_ON, 0);
        if (r != 0) r = adbWrite(FidRegistry.WFID_AC_OFF, 0); // 兜底：用 off FID 发 0
        return r;
    }

    public int stop() {
        if (simulation) { simAcOn = false; return 0; }
        return adbWrite(FidRegistry.WFID_AC_OFF, 1);
    }

    public int toggle() {
        return isOn() ? stop() : start();
    }

    public int setTemperature(int zone, int tempCelsius) {
        if (simulation) {
            if (zone == ZONE_MAIN) simMainTemp = Math.max(17, Math.min(33, tempCelsius));
            return 0;
        }
        int temp = Math.max(16, Math.min(30, tempCelsius));
        return adbWrite(FidRegistry.WFID_AC_TEMP, temp);
    }

    public int setMainTemp(int tempCelsius) {
        return setTemperature(ZONE_MAIN, tempCelsius);
    }

    public int setWindLevel(int level) {
        if (simulation) { simWindLevel = Math.max(0, Math.min(7, level)); return 0; }
        // 风量暂无已验证 write FID，保留原路径
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
        // WFID_AC_CYCLE: 0=外循环 1=内循环
        int val = (mode == CYCLE_INNER) ? 1 : 0;
        return adbWrite(FidRegistry.WFID_AC_CYCLE, val);
    }

    public int setControlMode(int mode) {
        if (simulation) { simControlMode = mode; return 0; }
        return ReflectionHelper.invokeVoidMethod(device, "setAcControlMode",
                new Class<?>[]{int.class, int.class}, new Object[]{mode, SOURCE_VOICE});
    }

    public int setDefrostRear(boolean on) {
        if (simulation) return 0;
        return adbWrite(FidRegistry.WFID_AC_DEFROST_REAR, on ? 1 : 0);
    }

    /** 座椅加热：seat=0主驾 1副驾, level=0=关 1~5=档位 */
    public int setSeatHeat(int seat, int level) {
        if (simulation) return 0;
        int sw = (seat == 0) ? FidRegistry.WFID_SEAT_HEAT_DR_SW : FidRegistry.WFID_SEAT_HEAT_PA_SW;
        int lv = (seat == 0) ? FidRegistry.WFID_SEAT_HEAT_DR_LV : FidRegistry.WFID_SEAT_HEAT_PA_LV;
        if (level == 0) return adbWrite(sw, 2); // 2=off
        adbWrite(sw, 1); // 1=on
        return adbWrite(lv, Math.max(1, Math.min(5, level)));
    }

    /** 座椅通风：seat=0主驾 1副驾, level=0=关 1~5=档位 */
    public int setSeatVent(int seat, int level) {
        if (simulation) return 0;
        int sw = (seat == 0) ? FidRegistry.WFID_SEAT_VENT_DR_SW : FidRegistry.WFID_SEAT_VENT_PA_SW;
        int lv = (seat == 0) ? FidRegistry.WFID_SEAT_VENT_DR_LV : FidRegistry.WFID_SEAT_VENT_PA_LV;
        if (level == 0) return adbWrite(sw, 2);
        adbWrite(sw, 1);
        return adbWrite(lv, Math.max(1, Math.min(5, level)));
    }

    private int adbWrite(int fid, int value) {
        dadb.Dadb d = AdbHelper.getSharedDadb();
        if (d == null) {
            Log.w(TAG, "adbWrite: dadb not connected");
            return -1;
        }
        try {
            String cmd = "service call autoservice 6 i32 " + DEVICE_TYPE + " i32 " + fid + " i32 " + value;
            dadb.AdbShellResponse resp = d.shell(cmd);
            Log.i(TAG, "adbWrite fid=" + fid + " val=" + value + " → " + resp.getAllOutput().trim());
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "adbWrite failed fid=" + fid, e);
            return -1;
        }
    }

    public boolean isRealDevice() {
        return device != null;
    }

    public int toggleCycleMode() {
        int current = getCycleMode();
        return setCycleMode(current == CYCLE_INNER ? CYCLE_OUTER : CYCLE_INNER);
    }
}
