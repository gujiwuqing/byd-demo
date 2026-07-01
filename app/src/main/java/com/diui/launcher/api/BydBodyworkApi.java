package com.diui.launcher.api;

import android.content.Context;
import android.util.Log;

import dadb.AdbShellResponse;

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

    // 车窗模拟状态 (0=关, 1=开)
    private int simWindowFL = 0;
    private int simWindowFR = 0;
    private int simWindowRL = 0;
    private int simWindowRR = 0;
    private boolean simTrunkOpen = false;
    private boolean simHoodOpen = false;

    public BydBodyworkApi(Context context) {
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
        if (simulation) { simLocked = !simLocked; return; }
        boolean locked = isLocked();
        adbWrite(FidRegistry.WFID_DOOR_LOCK, locked ? 1 : 2); // 1=解锁 2=上锁
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

    // ========== 车窗控制 ==========

    /**
     * 设置车窗状态
     * @param area 车窗位置 (DOOR_LEFT_FRONT, DOOR_RIGHT_FRONT, DOOR_LEFT_REAR, DOOR_RIGHT_REAR)
     * @param state STATE_CLOSED=0 关 STATE_OPEN=1 开
     */
    public void setWindowState(int area, int state) {
        if (simulation) {
            switch (area) {
                case DOOR_LEFT_FRONT: simWindowFL = state; break;
                case DOOR_RIGHT_FRONT: simWindowFR = state; break;
                case DOOR_LEFT_REAR: simWindowRL = state; break;
                case DOOR_RIGHT_REAR: simWindowRR = state; break;
            }
            return;
        }
        int fid = windowWriteFid(area);
        if (fid == -1) return;
        adbWrite(fid, state == STATE_OPEN ? 100 : 0);
    }

    public void setWindowPercent(int area, int percent) {
        if (simulation) return;
        int fid = windowWriteFid(area);
        if (fid != -1) adbWrite(fid, Math.max(0, Math.min(100, percent)));
    }

    private int windowWriteFid(int area) {
        switch (area) {
            case DOOR_LEFT_FRONT:  return FidRegistry.WFID_WINDOW_FL;
            case DOOR_RIGHT_FRONT: return FidRegistry.WFID_WINDOW_FR;
            case DOOR_LEFT_REAR:   return FidRegistry.WFID_WINDOW_RL;
            case DOOR_RIGHT_REAR:  return FidRegistry.WFID_WINDOW_RR;
            default: return -1;
        }
    }

    public void openWindow(int area) {
        setWindowState(area, STATE_OPEN);
    }

    public void closeWindow(int area) {
        setWindowState(area, STATE_CLOSED);
    }

    public int getWindowFL() { return simulation ? simWindowFL : getWindowState(DOOR_LEFT_FRONT); }
    public int getWindowFR() { return simulation ? simWindowFR : getWindowState(DOOR_RIGHT_FRONT); }
    public int getWindowRL() { return simulation ? simWindowRL : getWindowState(DOOR_LEFT_REAR); }
    public int getWindowRR() { return simulation ? simWindowRR : getWindowState(DOOR_RIGHT_REAR); }

    // ========== 后备箱控制 ==========

    public void openTrunk() {
        if (simulation) { simTrunkOpen = true; return; }
        adbWrite(FidRegistry.WFID_FRONT_TRUNK, 1);
    }

    public void closeTrunk() {
        if (simulation) { simTrunkOpen = false; return; }
        adbWrite(FidRegistry.WFID_FRONT_TRUNK, 3);
    }

    public boolean isTrunkOpen() {
        if (simulation) return simTrunkOpen;
        return getDoorState(DOOR_TRUNK) == STATE_OPEN;
    }

    // ========== 引擎盖控制 ==========

    public void openHood() {
        if (simulation) { simHoodOpen = true; return; }
        ReflectionHelper.invokeVoidMethod(device, "openHood",
                new Class<?>[]{}, new Object[]{});
    }

    public void closeHood() {
        if (simulation) { simHoodOpen = false; return; }
        ReflectionHelper.invokeVoidMethod(device, "closeHood",
                new Class<?>[]{}, new Object[]{});
    }

    public boolean isHoodOpen() {
        if (simulation) return simHoodOpen;
        return getDoorState(DOOR_HOOD) == STATE_OPEN;
    }

    private void adbWrite(int fid, int value) {
        dadb.Dadb d = AdbHelper.getSharedDadb();
        if (d == null) { Log.w(TAG, "adbWrite: dadb not connected"); return; }
        try {
            String cmd = "service call autoservice 6 i32 1001 i32 " + fid + " i32 " + value;
            Log.i(TAG, "adbWrite fid=" + fid + " val=" + value);
            d.shell(cmd);
        } catch (Exception e) {
            Log.e(TAG, "adbWrite failed fid=" + fid, e);
        }
    }

    public boolean isRealDevice() {
        return device != null;
    }
}
