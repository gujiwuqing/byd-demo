package com.bydlauncher.api;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

public class ReflectionHelper {

    private static final String TAG = "BydReflection";

    public static Object getDeviceInstance(String className, Context context) {
        try {
            Class<?> clazz = Class.forName(className);
            Method getInstance = clazz.getMethod("getInstance", Context.class);
            return getInstance.invoke(null, new BydPermissionContext(context));
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "BYD API not available (non-BYD device): " + className);
            return null;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SecurityException) {
                Log.e(TAG, "权限不足: " + className);
                Log.e(TAG, "SecurityException: " + cause.getMessage());
                Log.e(TAG, "请在 adb shell 中执行: pm grant com.bydlauncher <权限名>");
                Log.e(TAG, "运行 BydPermissionHelper.diagnosePermissions() 查看完整命令");
            } else {
                Log.e(TAG, "Failed to get instance: " + className, e);
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get instance: " + className, e);
            return null;
        }
    }

    public static int invokeIntMethod(Object device, String methodName) {
        if (device == null) return -1;
        return invokeIntMethod(device, methodName, new Class<?>[]{}, new Object[]{});
    }

    public static int invokeIntMethod(Object device, String methodName, Class<?>[] paramTypes, Object[] params) {
        if (device == null) return -1;
        try {
            Method method = device.getClass().getMethod(methodName, paramTypes);
            Object result = method.invoke(device, params);
            return result instanceof Integer ? (int) result : -1;
        } catch (Exception e) {
            Log.w(TAG, "Failed to invoke " + methodName, e);
            return -1;
        }
    }

    public static double invokeDoubleMethod(Object device, String methodName) {
        if (device == null) return -1.0;
        try {
            Method method = device.getClass().getMethod(methodName);
            Object result = method.invoke(device);
            return result instanceof Double ? (double) result : -1.0;
        } catch (Exception e) {
            Log.w(TAG, "Failed to invoke " + methodName, e);
            return -1.0;
        }
    }

    public static String invokeStringMethod(Object device, String methodName) {
        if (device == null) return null;
        try {
            Method method = device.getClass().getMethod(methodName);
            Object result = method.invoke(device);
            return result instanceof String ? (String) result : null;
        } catch (Exception e) {
            Log.w(TAG, "Failed to invoke " + methodName, e);
            return null;
        }
    }

    public static int invokeVoidMethod(Object device, String methodName, Class<?>[] paramTypes, Object[] params) {
        if (device == null) return -1;
        try {
            Method method = device.getClass().getMethod(methodName, paramTypes);
            Object result = method.invoke(device, params);
            return result instanceof Integer ? (int) result : 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to invoke " + methodName, e);
            return -1;
        }
    }

    public static int setViaBaseClass(Object device, int deviceType, int featureId, int value) {
        try {
            Method baseSet = device.getClass().getSuperclass()
                    .getDeclaredMethod("set", int.class, int.class, int.class);
            baseSet.setAccessible(true);
            Object result = baseSet.invoke(device, deviceType, featureId, value);
            return result instanceof Integer ? (int) result : 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set via base class", e);
            return -1;
        }
    }

    public static void registerListener(Object device, Object listener) {
        try {
            Class<?> listenerInterface = Class.forName("android.hardware.IBYDAutoListener");
            Method register = device.getClass().getMethod("registerListener", listenerInterface, int[].class);
            register.invoke(device, listener, new int[0]);
        } catch (Exception e) {
            Log.w(TAG, "Failed to register listener", e);
        }
    }
}
