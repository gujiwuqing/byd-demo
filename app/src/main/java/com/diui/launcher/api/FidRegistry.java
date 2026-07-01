package com.diui.launcher.api;

/**
 * autoservice Feature ID 常量表。
 * 来源：BYDMate FidMap + wheregoes/byd-apps + 车机实测验证。
 *
 * tx=5 表示 getInt，tx=7 表示 getFloat。
 * 注意：部分 FID 在不同车型上可能不可用（返回哨兵值）。
 */
public final class FidRegistry {

    // ========== 设备类型 ==========
    public static final int DEV_AC = 1000;
    public static final int DEV_BODYWORK = 1001;
    public static final int DEV_POWER_2 = 1002;
    public static final int DEV_LIGHT = 1004;
    public static final int DEV_DRIVE_MODE = 1006;
    public static final int DEV_SAFETY = 1007;
    public static final int DEV_CHARGE = 1009;
    public static final int DEV_GEARBOX = 1011;
    public static final int DEV_MOTOR = 1012;
    public static final int DEV_SPEED = 1013;
    public static final int DEV_STATISTIC = 1014;
    public static final int DEV_TIRE = 1016;
    public static final int DEV_POWER = 1023;
    public static final int DEV_LOCK = 1032;
    public static final int DEV_DOOR_LOCK = 1041;

    // ========== 事务码 ==========
    public static final int TX_GET_INT = 5;
    public static final int TX_SET_INT = 6;
    public static final int TX_GET_FLOAT = 7;

    // ========== AC (dev=1000) ==========
    public static final int FID_AC_STATE = 1077936144;         // tx=5, 0/1
    public static final int FID_AC_CYCLE = 1077936148;         // tx=5, 内/外循环
    public static final int FID_AC_FAN = 1077936156;           // tx=5, 0~7 风量 ← 修正！
    public static final int FID_AC_TEMP = 1077936168;          // tx=5, 设定温度
    public static final int FID_OUTSIDE_TEMP = 1077936184;     // tx=5, 车外温度
    public static final int FID_CABIN_TEMP = 1031798832;       // tx=5, 车内温度

    // ========== Bodywork (dev=1001) ==========
    public static final int FID_DOOR_FL = 692060168;           // tx=5, 0/1
    public static final int FID_DOOR_FR = 692060170;
    public static final int FID_DOOR_RL = 692060172;
    public static final int FID_DOOR_RR = 692060174;
    public static final int FID_HOOD = 692060188;              // tx=5, 引擎盖
    public static final int FID_TRUNK = 1074790416;            // tx=5, 后备箱
    public static final int FID_WINDOW_FL = 947912728;         // tx=5, 百分比
    public static final int FID_WINDOW_FR = 1267728400;
    public static final int FID_WINDOW_RL = 947912736;
    public static final int FID_WINDOW_RR = 947912752;
    public static final int FID_12V_VOLTAGE = 1128267816;      // tx=7 float!

    // ========== Statistic / Battery (dev=1014) ==========
    public static final int FID_SOC = 1246777400;              // tx=7 float! 电量百分比
    public static final int FID_MILEAGE = 1246765072;          // tx=5, ×0.1 = km
    public static final int FID_SOH = 1145045032;              // tx=5, 百分比
    public static final int FID_BATT_TEMP_MAX = 1148190752;    // tx=5, 需 -40
    public static final int FID_BATT_TEMP_MIN = 1148190736;    // tx=5, 需 -40
    public static final int FID_CELL_VOLT_MAX = 1147142192;    // tx=5, ×0.001 = V
    public static final int FID_CELL_VOLT_MIN = 1147142160;    // tx=5, ×0.001 = V
    public static final int FID_TOTAL_ELEC_CON = 1032871984;   // tx=7 float! 累计能耗 kWh

    // ========== Speed (dev=1013) ==========
    public static final int FID_SPEED = -1807745016;           // tx=7 float! ← 修正！

    // ========== Gearbox (dev=1011) ==========
    public static final int FID_GEAR = 555745336;              // tx=5

    // ========== Motor (dev=1012) ==========
    public static final int FID_MOTOR_POWER = 339738656;       // tx=5, kW

    // ========== Charge (dev=1009) ==========
    public static final int FID_CHARGE_GUN = 876609586;        // tx=5, 1=NONE 2=AC 3=DC
    public static final int FID_CHARGING_TYPE = 876609592;     // tx=5
    public static final int FID_CHARGING_BMS_STATE = 876609560;// tx=5, 1=充电中 2=完成
    public static final int FID_CHARGING_CAPACITY = 666894360; // tx=7 float! 本次充电量 kWh

    // ========== Power (dev=1023) ==========
    public static final int FID_POWER_STATE = 315621408;       // tx=5

    // ========== DriveMode (dev=1006) ==========
    public static final int FID_DRIVE_MODE = 555745294;        // tx=5
    public static final int FID_WORK_MODE = 874512420;         // tx=5, EV/HEV 模式

    // ========== Light (dev=1004) ==========
    public static final int FID_LIGHT_LOW = 950009866;         // tx=5, 近光灯
    public static final int FID_DRL = 1231040528;              // tx=5, 日行灯

    // ========== Safety (dev=1007) ==========
    public static final int FID_SEATBELT_FL = 692060184;       // tx=5, 安全带

    // ========== Lock (dev=1032) ==========
    public static final int FID_LOCK_FL = 1081081864;          // tx=5, 门锁

    // ========== Tire (dev=1016) ==========
    public static final int FID_TIRE_FL = -1728052956;         // tx=5, kPa
    public static final int FID_TIRE_FR = -1728052952;
    public static final int FID_TIRE_RL = -1728052948;
    public static final int FID_TIRE_RR = -1728052944;

    // ========== 写入 FID（来自 BYDMate LIVE_VALIDATED，Leopard 3 实测） ==========
    // 注意：写入 FID 与读取 FID 完全不同！

    // AC 写入 (dev=1000)
    public static final int WFID_AC_ON = 501219352;         // value=0 (unvalidated)
    public static final int WFID_AC_OFF = 501219364;        // value=1 ✓
    public static final int WFID_AC_TEMP = 501219368;       // value=16~30°C ✓
    public static final int WFID_AC_CYCLE = 501219355;      // value=0=外循环 1=内循环 ✓
    public static final int WFID_AC_DEFROST_REAR = 501219357; // value=0=off 1=on ✓

    // 车窗写入 (dev=1001) — 百分比 0~100
    public static final int WFID_WINDOW_FL = 1276219408;    // 左前窗 % ✓
    public static final int WFID_WINDOW_FR = 1276219424;    // 右前窗 % ✓
    public static final int WFID_WINDOW_RL = 1276219416;    // 左后窗 % ✓
    public static final int WFID_WINDOW_RR = 1276219432;    // 右后窗 % ✓

    // 门锁写入 (dev=1001)
    public static final int WFID_DOOR_LOCK = 1276141590;    // value=1=解锁 2=上锁 ✓

    // 天窗写入 (dev=1001)
    public static final int WFID_SUNROOF = 1125122056;      // value=1=开 2=关 3=倾 4=停 ✓
    public static final int WFID_SUNSHADE = 1125122060;     // value=1=开 2=关 ✓

    // 前备箱 (dev=1001)
    public static final int WFID_FRONT_TRUNK = 1276182560;  // value=1=开 3=关 ✓

    // 座椅加热/通风 (dev=1000) — switch: 1=on 2=off; level: 1~5
    public static final int WFID_SEAT_HEAT_DR_SW = 1276248084;  // 主驾加热开关 ✓
    public static final int WFID_SEAT_HEAT_DR_LV = 1276252180;  // 主驾加热档位 ✓
    public static final int WFID_SEAT_HEAT_PA_SW = 1276248092;  // 副驾加热开关 ✓
    public static final int WFID_SEAT_HEAT_PA_LV = 1276252188;  // 副驾加热档位 ✓
    public static final int WFID_SEAT_VENT_DR_SW = 1276248080;  // 主驾通风开关 ✓
    public static final int WFID_SEAT_VENT_DR_LV = 1276252176;  // 主驾通风档位 ✓
    public static final int WFID_SEAT_VENT_PA_SW = 1276248088;  // 副驾通风开关 ✓
    public static final int WFID_SEAT_VENT_PA_LV = 1276252184;  // 副驾通风档位 ✓

    // 室内灯/氛围灯 (dev=1023)
    public static final int WFID_INTERIOR_LIGHT = 1330643002;   // value=1=off 2=on ✓
    public static final int WFID_AMBIENT_LIGHT = 1069547536;    // value=1=off 2~5=亮度 ✓
    public static final int SENTINEL_NO_CAN = 0x0000FFFF;     // 65535, 无 CAN 信号
    public static final int SENTINEL_UNINIT = 0x000FFFFF;      // 未初始化
    public static final int SENTINEL_BAD_TX = 0xFFFFD8E3;      // -10013, 错误事务码
    public static final int SENTINEL_NO_WRITE = 0xFFFFD8E5;    // -10011, 未注册

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
