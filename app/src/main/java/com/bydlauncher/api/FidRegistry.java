package com.bydlauncher.api;

public final class FidRegistry {

    public static final int DEV_AC = 1000;
    public static final int DEV_BODYWORK = 1001;
    public static final int DEV_DRIVE_MODE = 1006;
    public static final int DEV_CHARGE = 1009;
    public static final int DEV_GEARBOX = 1011;
    public static final int DEV_MOTOR = 1012;
    public static final int DEV_SPEED = 1013;
    public static final int DEV_BATTERY = 1014;
    public static final int DEV_TIRE = 1016;
    public static final int DEV_POWER = 1023;
    public static final int DEV_DOOR_LOCK = 1041;

    public static final int TX_GET_INT = 5;
    public static final int TX_SET_INT = 6;
    public static final int TX_GET_FLOAT = 7;

    public static final int FID_SOC = 1246777400;
    public static final int FID_MILEAGE = 1246765072;
    public static final int FID_SOH = 1145045032;
    public static final int FID_BATT_TEMP_MAX = 1148190752;
    public static final int FID_BATT_TEMP_MIN = 1148190736;
    public static final int FID_CELL_VOLT_MAX = 1147142192;
    public static final int FID_CELL_VOLT_MIN = 1147142160;
    public static final int FID_ACCUM_ENERGY = 1032871984;

    public static final int FID_SPEED = -1807745016;
    public static final int FID_GEAR = 555745336;
    public static final int FID_MOTOR_POWER = 339738656;
    public static final int FID_CHARGE_GUN = 876609586;
    public static final int FID_POWER_STATE = 315621408;
    public static final int FID_DRIVE_MODE = 555745294;

    public static final int FID_12V_VOLTAGE = 1128267816;
    public static final int FID_DOOR_FL = 692060168;
    public static final int FID_DOOR_FR = 692060170;
    public static final int FID_DOOR_RL = 692060172;
    public static final int FID_DOOR_RR = 692060174;
    public static final int FID_WINDOW_FL = 947912728;
    public static final int FID_WINDOW_FR = 1267728400;
    public static final int FID_WINDOW_RL = 947912736;
    public static final int FID_WINDOW_RR = 947912752;

    public static final int FID_TIRE_FL = -1728052956;
    public static final int FID_TIRE_FR = -1728052952;
    public static final int FID_TIRE_RL = -1728052948;
    public static final int FID_TIRE_RR = -1728052944;

    public static final int FID_AC_STATE = 1077936144;
    public static final int FID_AC_TEMP = 1077936168;
    public static final int FID_CABIN_TEMP = 1031798832;
    public static final int FID_OUTSIDE_TEMP = 1077936184;
    public static final int FID_AC_WIND = 0x1DE0000C;

    public static final int SENTINEL_NO_CAN = 0x0000FFFF;
    public static final int SENTINEL_UNINIT = 0x000FFFFF;
    public static final int SENTINEL_BAD_TX = 0xFFFFD8E3;
    public static final int SENTINEL_NO_WRITE = 0xFFFFD8E5;

    public static boolean isSentinel(int value) {
        return value == SENTINEL_NO_CAN
                || value == SENTINEL_UNINIT
                || value == SENTINEL_BAD_TX
                || value == SENTINEL_NO_WRITE;
    }

    public static boolean isSentinelFloat(float value) {
        return value == -1.0f || Float.isNaN(value);
    }

    private FidRegistry() {}
}
