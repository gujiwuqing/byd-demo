package com.diui.launcher.model;

public class VehicleStatus {
    // 电源状态
    public int powerLevel = -1;
    public String powerLevelText = "N/A";

    // 电池/燃油
    public int batteryPercent = -1;
    public double elecPercent = -1;
    public int fuelPercent = -1;
    public double fuelAmount = -1;

    // 续航
    public int evMileage = -1;
    public int totalRange = -1;
    public double totalMileage = -1;
    public double hevMileage = -1;

    // 能耗
    public double currentElecConsumption = -1;
    public double currentFuelConsumption = -1;
    public double avgElecConsumption = -1;
    public double avgFuelConsumption = -1;

    // 驾驶
    public int speed = 0;
    public int gear = 0; // 0=P, 1=R, 2=N, 3=D
    public double powerKw = 0;

    // 温度
    public int outsideTemp = -1;

    // 胎压胎温
    public int tirePressureFL = -1;
    public int tirePressureFR = -1;
    public int tirePressureRL = -1;
    public int tirePressureRR = -1;
    public int tireTempFL = -1;
    public int tireTempFR = -1;
    public int tireTempRL = -1;
    public int tireTempRR = -1;

    // 行程
    public double tripDistance = 0;
    public String tripTime = "00:00";
    public double tripElec = 0;
    public double tripFuel = 0;

    // 车门状态
    public boolean isLocked = false;
    public boolean doorLeftFrontOpen = false;
    public boolean doorRightFrontOpen = false;
    public boolean doorLeftRearOpen = false;
    public boolean doorRightRearOpen = false;
    public boolean trunkOpen = false;
    public boolean hoodOpen = false;

    // 车窗状态 (0=关, 1=开)
    public int windowFL = 0;
    public int windowFR = 0;
    public int windowRL = 0;
    public int windowRR = 0;
    public boolean windowLocked = false;

    // 天窗
    public boolean sunroofOpen = false;

    // 空调
    public boolean acOn = false;
    public int acTemp = 25;
    public int acWindLevel = 0;
    public int acWindMode = 0;
    public int acCycleMode = 0;
    public int acControlMode = 0;

    // 电池详情（service call autoservice 补充数据）
    public int batteryTempMax = -1;
    public int batteryTempMin = -1;
    public int cellVoltageMax = -1;
    public int cellVoltageMin = -1;
    public double voltage12v = -1;
    public int soh = -1;

    // 车辆状态（service call autoservice 补充数据）
    public int chargeGunState = 0;
    public int powerState = -1;
    public int driveMode = -1;
    public double motorPowerKw = 0;

    // 能量模式
    public int smartChargePercent = 25;
    public String recoveryMode = "最大回收";

    public String getEvMileageText() {
        return evMileage >= 0 ? evMileage + "km" : "N/A";
    }

    public String getTotalRangeText() {
        return totalRange >= 0 ? totalRange + "km" : "N/A";
    }

    public String getTotalMileageText() {
        return totalMileage >= 0 ? String.format("%.1fkm", totalMileage) : "N/A";
    }

    public String getHevMileageText() {
        return hevMileage >= 0 ? String.format("%.1fkm", hevMileage) : "N/A";
    }

    public String getBatteryText() {
        if (elecPercent >= 0) return String.format("%.0f%%", elecPercent);
        if (batteryPercent >= 0) return batteryPercent + "%";
        return "N/A";
    }

    public int getBatteryValue() {
        if (elecPercent >= 0) return (int) elecPercent;
        return Math.max(batteryPercent, 0);
    }

    public String getFuelText() {
        return fuelAmount >= 0 ? String.format("%.1fL", fuelAmount) : "N/A";
    }

    public String getSpeedText() {
        return speed + "";
    }

    public String getPowerKwText() {
        return String.format("%.1fKw", powerKw);
    }

    public String getGearText() {
        switch (gear) {
            case 0: return "P";
            case 1: return "R";
            case 2: return "N";
            case 3: return "D";
            default: return "P";
        }
    }

    public String getOutsideTempText() {
        return outsideTemp > -40 && outsideTemp < 80 ? outsideTemp + "°C" : "N/A";
    }

    public String getAcTempText() {
        return acTemp > 0 && acTemp < 100 ? acTemp + "°C" : "N/A";
    }

    public String getWindLevelText() {
        return acWindLevel >= 0 && acWindLevel <= 7 ? "风量 " + acWindLevel : "N/A";
    }

    public String getCycleModeText() {
        return acCycleMode == 0 ? "内循环" : "外循环";
    }

    public String getAcModeText() {
        return acControlMode == 0 ? "自动" : "手动";
    }

    public String getBatteryTempText() {
        if (batteryTempMax < -40 || batteryTempMin < -40) return "N/A";
        return batteryTempMin + "~" + batteryTempMax + "°C";
    }

    public String getCellVoltageText() {
        if (cellVoltageMax < 0 || cellVoltageMin < 0) return "N/A";
        return cellVoltageMin + "~" + cellVoltageMax + "mV";
    }

    public String getVoltage12vText() {
        return voltage12v >= 0 ? String.format("%.1fV", voltage12v) : "N/A";
    }

    public String getSohText() {
        return soh >= 0 ? soh + "%" : "N/A";
    }

    public String getDriveModeText() {
        switch (driveMode) {
            case 0: return "标准";
            case 1: return "运动";
            case 2: return "经济";
            case 3: return "雪地";
            default: return driveMode >= 0 ? "模式" + driveMode : "N/A";
        }
    }

    public String getPowerStateText() {
        switch (powerState) {
            case 0: return "OFF";
            case 1: return "ACC";
            case 2: return "ON";
            default: return powerState >= 0 ? "状态" + powerState : "N/A";
        }
    }

    public String getChargeGunStateText() {
        switch (chargeGunState) {
            case 0: return "未连接";
            case 1: return "已连接";
            case 2: return "充电中";
            default: return "状态" + chargeGunState;
        }
    }

    public boolean hasAnyDoorOpen() {
        return doorLeftFrontOpen || doorRightFrontOpen
                || doorLeftRearOpen || doorRightRearOpen
                || trunkOpen || hoodOpen;
    }
}
