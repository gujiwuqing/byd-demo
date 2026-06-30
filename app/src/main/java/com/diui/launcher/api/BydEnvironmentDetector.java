package com.diui.launcher.api;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

public class BydEnvironmentDetector {

    private static final String TAG = "BydEnvDetector";

    public enum Environment {
        REAL_DEVICE,
        PERMISSION_NEEDED,
        SIMULATOR
    }

    public static Environment detect(Context context) {
        Class<?> acClass;
        try {
            acClass = Class.forName("android.hardware.bydauto.ac.BYDAutoAcDevice");
            Log.i(TAG, "BYD API class found");
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "BYD API class not found → SIMULATOR");
            return Environment.SIMULATOR;
        }

        try {
            Method getInstance = acClass.getMethod("getInstance", Context.class);
            Object device = getInstance.invoke(null, new BydPermissionContext(context));
            if (device == null) {
                Log.i(TAG, "getInstance returned null → PERMISSION_NEEDED");
                return Environment.PERMISSION_NEEDED;
            }

            Method getState = acClass.getMethod("getAcStartState");
            Object result = getState.invoke(device);
            int value = (result instanceof Integer) ? (int) result : -1;

            if (value == 65535 || value == -10011) {
                Log.i(TAG, "API returned sentinel value " + value + " → PERMISSION_NEEDED");
                return Environment.PERMISSION_NEEDED;
            }

            Log.i(TAG, "API returned valid value " + value + " → REAL_DEVICE");
            return Environment.REAL_DEVICE;

        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SecurityException) {
                Log.i(TAG, "SecurityException → PERMISSION_NEEDED: " + cause.getMessage());
                return Environment.PERMISSION_NEEDED;
            }
            Log.w(TAG, "InvocationTargetException → SIMULATOR", e);
            return Environment.SIMULATOR;
        } catch (Exception e) {
            Log.w(TAG, "Detection failed → SIMULATOR", e);
            return Environment.SIMULATOR;
        }
    }
}
