package com.bydlauncher.api;

import android.content.Context;

public class BydDoorLockApi {

    private static final String CLASS_NAME = "android.hardware.bydauto.doorlock.BYDAutoDoorLockDevice";

    public static final int AREA_LEFT_FRONT = 1;
    public static final int AREA_LEFT_REAR = 2;
    public static final int AREA_RIGHT_FRONT = 3;
    public static final int AREA_RIGHT_REAR = 4;
    public static final int AREA_BACK = 5;
    public static final int AREA_CHILDLOCK_LEFT = 6;
    public static final int AREA_CHILDLOCK_RIGHT = 7;

    public static final int STATE_INVALID = 0;
    public static final int STATE_UNLOCK = 1;
    public static final int STATE_LOCK = 2;

    private final Object device;

    public BydDoorLockApi(Context context) {
        this.device = ReflectionHelper.getDeviceInstance(CLASS_NAME, context);
    }

    public boolean isAvailable() {
        return device != null;
    }

    public int getDoorLockStatus(int area) {
        return ReflectionHelper.invokeIntMethod(device, "getDoorLockStatus",
                new Class<?>[]{int.class}, new Object[]{area});
    }
}
