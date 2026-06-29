package com.bydlauncher.api;

import android.content.Context;
import android.util.Log;

/**
 * BYD 行驶状态 API
 * 读取车速、档位、功率、油量、续航、能耗、行程、智保、能量回收等
 *
 * 尝试多个可能的 BYD 系统类名：
 *   android.hardware.bydauto.vehicle.BYDAutoVehicleInfoDevice
 *   android.hardware.bydauto.drive.BYDAutoDriveDevice
 *   android.hardware.bydauto.motor.BYDAutoMotorDevice
 */
public class BydDriveApi {

    private static final String TAG = "BydDriveApi";

    // 候选类名（按优先级排序）
    private static final String[] CANDIDATE_CLASSES = {
            "android.hardware.bydauto.vehicle.BYDAutoVehicleInfoDevice",
            "android.hardware.bydauto.drive.BYDAutoDriveDevice",
            "android.hardware.bydauto.vehicle.BYDAutoVehicleDevice",
            "android.hardware.bydauto.motor.BYDAutoMotorDevice",
            "android.hardware.bydauto.power.BYDAutoPowerDevice",
            "android.hardware.bydauto.energy.BYDAutoEnergyDevice",
    };

    private final Object device;
    private final boolean simulation;
    private final String resolvedClassName;

    // 模拟数据
    private final int simSpeed = 0;
    private final int simGear = 0; // 0=P, 1=R, 2=N, 3=D
    private final double simPowerKw = 0;
    private final int simFuelPercent = 37;
    private final double simFuelAmount = 21.0;
    private final int simTotalRange = 396;
    private final double simHevMileage = 18563.0;
    private final double simCurrentElecConsumption = 16.0;
    private final double simCurrentFuelConsumption = 5.7;
    private final double simAvgElecConsumption = 25.4;
    private final double simAvgFuelConsumption = 0;
    private final double simTripDistance = 0;
    private final String simTripTime = "00:00";
    private final double simTripElec = 0;
    private final double simTripFuel = 0;
    private final int simSmartChargePercent = 25;
    private final String simRecoveryMode = "最大回收";

    public BydDriveApi(Context context) {
        Object foundDevice = null;
        String foundClass = null;

        for (String className : CANDIDATE_CLASSES) {
            Object d = ReflectionHelper.getDeviceInstance(className, context);
            if (d != null) {
                foundDevice = d;
                foundClass = className;
                Log.i(TAG, "找到可用的行驶状态 API: " + className);
                break;
            }
        }

        this.device = foundDevice;
        this.resolvedClassName = foundClass;
        this.simulation = (device == null);

        if (simulation) {
            Log.i(TAG, "Running in simulation mode (未找到任何行驶状态 API)");
        } else {
            Log.i(TAG, "使用类: " + resolvedClassName);
            listAvailableMethods();
        }
    }

    private void listAvailableMethods() {
        if (device == null) return;
        java.lang.reflect.Method[] methods = device.getClass().getMethods();
        Log.i(TAG, "可用方法 (" + resolvedClassName + "):");
        for (java.lang.reflect.Method m : methods) {
            if (m.getDeclaringClass() == Object.class) continue;
            StringBuilder sb = new StringBuilder();
            sb.append("  ").append(m.getReturnType().getSimpleName())
              .append(" ").append(m.getName()).append("(");
            Class<?>[] params = m.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(params[i].getSimpleName());
            }
            sb.append(")");
            Log.i(TAG, sb.toString());
        }
    }

    public boolean isAvailable() {
        return device != null || simulation;
    }

    public boolean isRealDevice() {
        return device != null;
    }

    // ========== 车速 ==========

    public int getSpeed() {
        if (simulation) return simSpeed;
        int speed = tryGetInt("getSpeed");
        if (speed != -1) return speed;
        speed = tryGetInt("getVehicleSpeed");
        if (speed != -1) return speed;
        speed = tryGetInt("getSpeedValue");
        if (speed != -1) return speed;
        return 0;
    }

    // ========== 档位 ==========
    // 0=P, 1=R, 2=N, 3=D

    public int getGear() {
        if (simulation) return simGear;
        int gear = tryGetInt("getGear");
        if (gear != -1) return mapGearValue(gear);
        gear = tryGetInt("getGearPosition");
        if (gear != -1) return mapGearValue(gear);
        gear = tryGetInt("getShiftState");
        if (gear != -1) return mapGearValue(gear);
        return 0;
    }

    /**
     * 将 BYD 返回的档位值映射为统一的 0-3 编码
     * BYD 可能使用不同的枚举值，这里做兼容映射
     */
    private int mapGearValue(int raw) {
        // 常见映射方案：P=0, R=1, N=2, D=3 (直接用)
        if (raw >= 0 && raw <= 3) return raw;
        // 其他可能的映射：P=1, R=2, N=3, D=4
        if (raw >= 1 && raw <= 4) return raw - 1;
        // 未知值默认 P
        Log.w(TAG, "未知档位值: " + raw + "，默认显示 P");
        return 0;
    }

    // ========== 功率 ==========

    public double getPowerKw() {
        if (simulation) return simPowerKw;
        double power = tryGetDouble("getPower");
        if (power != -1.0) return power;
        power = tryGetDouble("getMotorPower");
        if (power != -1.0) return power;
        power = tryGetDouble("getPowerValue");
        if (power != -1.0) return power;
        power = tryGetDouble("getCurrentPower");
        if (power != -1.0) return power;
        // 也尝试 int 方法
        int intPower = tryGetInt("getPower");
        if (intPower != -1) return intPower;
        return 0;
    }

    // ========== 油量/续航 ==========

    public int getFuelPercent() {
        if (simulation) return simFuelPercent;
        int fuel = tryGetInt("getFuelPercent");
        if (fuel != -1) return fuel;
        fuel = tryGetInt("getFuelLevel");
        if (fuel != -1) return fuel;
        fuel = tryGetInt("getFuelPercentage");
        if (fuel != -1) return fuel;
        fuel = tryGetInt("getFuelPercentValue");
        if (fuel != -1) return fuel;
        return -1;
    }

    public double getFuelAmount() {
        if (simulation) return simFuelAmount;
        double amount = tryGetDouble("getFuelAmount");
        if (amount != -1.0) return amount;
        amount = tryGetDouble("getFuelVolume");
        if (amount != -1.0) return amount;
        amount = tryGetDouble("getFuelAmountValue");
        if (amount != -1.0) return amount;
        return -1;
    }

    public int getTotalRange() {
        if (simulation) return simTotalRange;
        int range = tryGetInt("getTotalRange");
        if (range != -1) return range;
        range = tryGetInt("getTotalMileage");
        if (range != -1) return range;
        range = tryGetInt("getTotalRangeValue");
        if (range != -1) return range;
        range = tryGetInt("getRemainingRange");
        if (range != -1) return range;
        return -1;
    }

    public double getHevMileage() {
        if (simulation) return simHevMileage;
        double mileage = tryGetDouble("getHevMileage");
        if (mileage != -1.0) return mileage;
        mileage = tryGetDouble("getHevMileageValue");
        if (mileage != -1.0) return mileage;
        mileage = tryGetDouble("getFuelMileage");
        if (mileage != -1.0) return mileage;
        return -1;
    }

    // ========== 能耗 ==========

    public double getCurrentElecConsumption() {
        if (simulation) return simCurrentElecConsumption;
        double val = tryGetDouble("getCurrentElecConsumption");
        if (val != -1.0) return val;
        val = tryGetDouble("getElecConsumption");
        if (val != -1.0) return val;
        val = tryGetDouble("getCurrentElecConValue");
        if (val != -1.0) return val;
        return -1;
    }

    public double getCurrentFuelConsumption() {
        if (simulation) return simCurrentFuelConsumption;
        double val = tryGetDouble("getCurrentFuelConsumption");
        if (val != -1.0) return val;
        val = tryGetDouble("getFuelConsumption");
        if (val != -1.0) return val;
        val = tryGetDouble("getCurrentFuelConValue");
        if (val != -1.0) return val;
        return -1;
    }

    public double getAvgElecConsumption() {
        if (simulation) return simAvgElecConsumption;
        double val = tryGetDouble("getAvgElecConsumption");
        if (val != -1.0) return val;
        val = tryGetDouble("getAverageElecConsumption");
        if (val != -1.0) return val;
        return -1;
    }

    public double getAvgFuelConsumption() {
        if (simulation) return simAvgFuelConsumption;
        double val = tryGetDouble("getAvgFuelConsumption");
        if (val != -1.0) return val;
        val = tryGetDouble("getAverageFuelConsumption");
        if (val != -1.0) return val;
        return -1;
    }

    // ========== 行程 ==========

    public double getTripDistance() {
        if (simulation) return simTripDistance;
        double val = tryGetDouble("getTripDistance");
        if (val != -1.0) return val;
        val = tryGetDouble("getTripMileage");
        if (val != -1.0) return val;
        return 0;
    }

    public String getTripTime() {
        if (simulation) return simTripTime;
        String val = tryGetString("getTripTime");
        if (val != null) return val;
        val = tryGetString("getTripDuration");
        if (val != null) return val;
        return "00:00";
    }

    public double getTripElecConsumption() {
        if (simulation) return simTripElec;
        double val = tryGetDouble("getTripElecConsumption");
        if (val != -1.0) return val;
        val = tryGetDouble("getTripElec");
        if (val != -1.0) return val;
        return 0;
    }

    public double getTripFuelConsumption() {
        if (simulation) return simTripFuel;
        double val = tryGetDouble("getTripFuelConsumption");
        if (val != -1.0) return val;
        val = tryGetDouble("getTripFuel");
        if (val != -1.0) return val;
        return 0;
    }

    // ========== 能量模式 ==========

    public int getSmartChargePercent() {
        if (simulation) return simSmartChargePercent;
        int val = tryGetInt("getSmartChargePercent");
        if (val != -1) return val;
        val = tryGetInt("getSmartChargeLevel");
        if (val != -1) return val;
        val = tryGetInt("getSmartChargePercentage");
        if (val != -1) return val;
        return 25;
    }

    public String getRecoveryMode() {
        if (simulation) return simRecoveryMode;
        String val = tryGetString("getRecoveryMode");
        if (val != null) return val;
        val = tryGetString("getEnergyRecoveryMode");
        if (val != null) return val;
        int mode = tryGetInt("getRecoveryLevel");
        if (mode != -1) return mapRecoveryLevel(mode);
        mode = tryGetInt("getEnergyRecoveryLevel");
        if (mode != -1) return mapRecoveryLevel(mode);
        return "最大回收";
    }

    private String mapRecoveryLevel(int level) {
        switch (level) {
            case 0: return "标准回收";
            case 1: return "较大回收";
            case 2: return "最大回收";
            default: return "级别" + level;
        }
    }

    // ========== 工具方法 ==========

    private int tryGetInt(String methodName) {
        return ReflectionHelper.invokeIntMethod(device, methodName);
    }

    private double tryGetDouble(String methodName) {
        return ReflectionHelper.invokeDoubleMethod(device, methodName);
    }

    private String tryGetString(String methodName) {
        return ReflectionHelper.invokeStringMethod(device, methodName);
    }
}
