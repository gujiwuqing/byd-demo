package com.bydlauncher.api;

import android.content.Context;
import android.util.Log;

/**
 * BYD 胎压胎温 API
 * 反射调用 android.hardware.bydauto.tire.BYDAutoTyreDevice
 */
public class BydTireApi {

    private static final String TAG = "BydTireApi";
    private static final String CLASS_NAME = "android.hardware.bydauto.tire.BYDAutoTyreDevice";

    // 轮胎位置常量（参考 BYD 其他 API 的编号风格）
    public static final int TIRE_LEFT_FRONT = 1;
    public static final int TIRE_RIGHT_FRONT = 2;
    public static final int TIRE_LEFT_REAR = 3;
    public static final int TIRE_RIGHT_REAR = 4;

    private final Object device;
    private final boolean simulation;

    // 模拟数据
    private final int simPressureFL = 250;  // kPa
    private final int simPressureFR = 252;
    private final int simPressureRL = 250;
    private final int simPressureRR = 250;
    private final int simTempFL = 31;  // °C
    private final int simTempFR = 31;
    private final int simTempRL = 30;
    private final int simTempRR = 33;

    public BydTireApi(Context context) {
        if (BydVehicleManager.isForceSimulation()) {
            this.device = null;
            this.simulation = true;
        } else {
            this.device = ReflectionHelper.getDeviceInstance(CLASS_NAME, context);
            this.simulation = (device == null);
        }
        if (simulation) {
            Log.i(TAG, "Running in simulation mode");
        } else {
            Log.i(TAG, "Real device connected, listing available methods...");
            listAvailableMethods();
        }
    }

    private void listAvailableMethods() {
        if (device == null) return;
        java.lang.reflect.Method[] methods = device.getClass().getMethods();
        Log.i(TAG, "可用方法:");
        for (java.lang.reflect.Method m : methods) {
            if (m.getDeclaringClass() == Object.class) continue;
            Log.i(TAG, "  " + m.getReturnType().getSimpleName() + " " + m.getName() + "()");
        }
    }

    public boolean isAvailable() {
        return device != null || simulation;
    }

    public boolean isRealDevice() {
        return device != null;
    }

    /**
     * 获取胎压（单位：kPa）
     * @param position 轮胎位置 (TIRE_LEFT_FRONT 等)
     * @return 胎压值，-1 表示不可用
     */
    public int getTirePressure(int position) {
        if (simulation) {
            switch (position) {
                case TIRE_LEFT_FRONT: return simPressureFL;
                case TIRE_RIGHT_FRONT: return simPressureFR;
                case TIRE_LEFT_REAR: return simPressureRL;
                case TIRE_RIGHT_REAR: return simPressureRR;
                default: return -1;
            }
        }

        // 尝试多种可能的方法名
        int pressure = tryGetInt("getTirePressure", position);
        if (pressure != -1) return pressure;

        pressure = tryGetInt("getTyrePressure", position);
        if (pressure != -1) return pressure;

        pressure = tryGetInt("getPressure", position);
        if (pressure != -1) return pressure;

        pressure = tryGetInt("getTirePressureValue", position);
        if (pressure != -1) return pressure;

        return -1;
    }

    /**
     * 获取胎温（单位：°C）
     * @param position 轮胎位置
     * @return 胎温值，-1 表示不可用
     */
    public int getTireTemperature(int position) {
        if (simulation) {
            switch (position) {
                case TIRE_LEFT_FRONT: return simTempFL;
                case TIRE_RIGHT_FRONT: return simTempFR;
                case TIRE_LEFT_REAR: return simTempRL;
                case TIRE_RIGHT_REAR: return simTempRR;
                default: return -1;
            }
        }

        // 尝试多种可能的方法名
        int temp = tryGetInt("getTireTemperature", position);
        if (temp != -1) return temp;

        temp = tryGetInt("getTyreTemperature", position);
        if (temp != -1) return temp;

        temp = tryGetInt("getTemperature", position);
        if (temp != -1) return temp;

        temp = tryGetInt("getTireTemp", position);
        if (temp != -1) return temp;

        temp = tryGetInt("getTireTemperatureValue", position);
        if (temp != -1) return temp;

        return -1;
    }

    /**
     * 便捷方法：获取左前轮胎压
     */
    public int getPressureFL() {
        return getTirePressure(TIRE_LEFT_FRONT);
    }

    public int getPressureFR() {
        return getTirePressure(TIRE_RIGHT_FRONT);
    }

    public int getPressureRL() {
        return getTirePressure(TIRE_LEFT_REAR);
    }

    public int getPressureRR() {
        return getTirePressure(TIRE_RIGHT_REAR);
    }

    public int getTempFL() {
        return getTireTemperature(TIRE_LEFT_FRONT);
    }

    public int getTempFR() {
        return getTireTemperature(TIRE_RIGHT_FRONT);
    }

    public int getTempRL() {
        return getTireTemperature(TIRE_LEFT_REAR);
    }

    public int getTempRR() {
        return getTireTemperature(TIRE_RIGHT_REAR);
    }

    /**
     * 尝试调用带 int 参数的 int 方法，失败返回 -1
     */
    private int tryGetInt(String methodName, int param) {
        return ReflectionHelper.invokeIntMethod(device, methodName,
                new Class<?>[]{int.class}, new Object[]{param});
    }
}
