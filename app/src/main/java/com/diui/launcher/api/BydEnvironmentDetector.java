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
            // BYD API 类存在说明是真实车机。getInstance 调用失败无论 cause 是
            // SecurityException 还是其他 RuntimeException（IllegalState/NullPoint 等），
            // 多半是权限未授予或设备未就绪——一律判为 PERMISSION_NEEDED，触发 ADB 授权，
            // 绝不误判为 SIMULATOR 导致授权流程不触发。
            if (cause != null) {
                Log.i(TAG, "InvocationTargetException cause=" + cause.getClass().getName()
                        + " → PERMISSION_NEEDED: " + cause.getMessage());
            } else {
                Log.i(TAG, "InvocationTargetException (no cause) → PERMISSION_NEEDED");
            }
            return Environment.PERMISSION_NEEDED;
        } catch (Exception e) {
            // 其他异常（如 NoSuchMethodException）说明 API 签名不符，仍按需要授权处理
            Log.w(TAG, "Detection failed → PERMISSION_NEEDED: " + e.getMessage());
            return Environment.PERMISSION_NEEDED;
        }
    }
}
