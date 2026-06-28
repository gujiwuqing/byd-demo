package com.bydlauncher.model;

public class VehicleStatus {
    // 电源状态
    public int powerLevel = -1;
    public String powerLevelText = "N/A";

    // 电池
    public int batteryPercent = -1;
    public double elecPercent = -1;
    public int evMileage = -1;
    public double totalMileage = -1;

    // 车门状态
    public boolean isLocked = false;
    public boolean doorLeftFrontOpen = false;
    public boolean doorRightFrontOpen = false;
    public boolean doorLeftRearOpen = false;
    public boolean doorRightRearOpen = false;
    public boolean trunkOpen = false;
    public boolean hoodOpen = false;

    // 空调
    public boolean acOn = false;
    public int acTemp = 25;
    public int outsideTemp = -1;
    public int acWindLevel = 0;
    public int acWindMode = 0;
    public int acCycleMode = 0;
    public int acControlMode = 0;

    public String getEvMileageText() {
        return evMileage >= 0 ? evMileage + " km" : "N/A";
    }

    public String getTotalMileageText() {
        return totalMileage >= 0 ? String.format("%.1f km", totalMileage) : "N/A";
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

    public String getAcTempText() {
        return acTemp > 0 && acTemp < 100 ? acTemp + "°C" : "N/A";
    }

    public String getOutsideTempText() {
        return outsideTemp > -40 && outsideTemp < 80 ? outsideTemp + "°C" : "N/A";
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

    public boolean hasAnyDoorOpen() {
        return doorLeftFrontOpen || doorRightFrontOpen
                || doorLeftRearOpen || doorRightRearOpen
                || trunkOpen || hoodOpen;
    }
}
