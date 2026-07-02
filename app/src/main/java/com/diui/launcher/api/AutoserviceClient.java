package com.diui.launcher.api;

import android.content.Context;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dadb.AdbShellResponse;
import dadb.Dadb;

/**
 * 通过 ADB shell 执行 service call autoservice 读取车辆数据。
 * 使用 dadb 连接（shell UID = 2000），绕过 app UID 的权限限制。
 * 当 dadb 不可用时降级为 Runtime.exec()（app UID，大概率无权限）。
 */
public class AutoserviceClient {

    private static final String TAG = "AutoserviceClient";
    private static final Pattern PARCEL_HEX_PATTERN = Pattern.compile(
            "Result:\\s*Parcel\\(([0-9a-fA-F\\s]+)'");

    private boolean available = true;

    public AutoserviceClient(Context context) {
    }

    public boolean isAvailable() {
        return available;
    }

    // ---------- 写入（tx=6 setInt）----------

    /**
     * 通过 ADB shell 向 autoservice 写入整数值。
     * 使用 service call autoservice 6 i32 <dev> i32 <fid> i32 <value>
     * 返回 0=成功，非0=失败。
     */
    public int writeInt(int deviceType, int fid, int value) {
        String cmd = "service call autoservice 6 i32 " + deviceType + " i32 " + fid + " i32 " + value;
        Log.i(TAG, "writeInt: dev=" + deviceType + " fid=" + fid + " val=" + value);
        String result = execShell(cmd);
        if (result == null) {
            Log.w(TAG, "writeInt failed: null result");
            return -1;
        }
        // 检查返回的异常码（第一个8位hex应为00000000）
        int ret = parseParcelInt(result);
        Log.i(TAG, "writeInt result: " + ret + " raw=" + result.trim());
        return ret == 0 ? 0 : ret;
    }

    // ---------- 核心读取 ----------

    public int getInt(int deviceType, int featureId) {
        String cmd = "service call autoservice " + FidRegistry.TX_GET_INT
                + " i32 " + deviceType + " i32 " + featureId;
        String result = execShell(cmd);
        return parseParcelInt(result);
    }

    public float getFloat(int deviceType, int featureId) {
        String cmd = "service call autoservice " + FidRegistry.TX_GET_FLOAT
                + " i32 " + deviceType + " i32 " + featureId;
        String result = execShell(cmd);
        return parseParcelFloat(result);
    }

    // ---------- AC 数据 ----------

    public int getAcState() {
        return getInt(FidRegistry.DEV_AC, FidRegistry.FID_AC_STATE);
    }

    public int getAcTemp() {
        return getInt(FidRegistry.DEV_AC, FidRegistry.FID_AC_TEMP);
    }

    public int getOutsideTemp() {
        return getInt(FidRegistry.DEV_AC, FidRegistry.FID_OUTSIDE_TEMP);
    }

    public int getCabinTemp() {
        return getInt(FidRegistry.DEV_AC, FidRegistry.FID_CABIN_TEMP);
    }

    public int getFanLevel() {
        return getInt(FidRegistry.DEV_AC, FidRegistry.FID_AC_FAN);
    }

    public int getAcCycle() {
        return getInt(FidRegistry.DEV_AC, FidRegistry.FID_AC_CYCLE);
    }

    // ---------- 车身数据 ----------

    public int getBatteryCapacity() {
        float soc = getFloat(FidRegistry.DEV_STATISTIC, FidRegistry.FID_SOC);
        return soc > 0 ? Math.round(soc) : -1;
    }

    public int getDoorState(int fid) {
        return getInt(FidRegistry.DEV_BODYWORK, fid);
    }

    public int getWindowState(int fid) {
        return getInt(FidRegistry.DEV_BODYWORK, fid);
    }

    public int getHoodState() {
        return getInt(FidRegistry.DEV_BODYWORK, FidRegistry.FID_HOOD);
    }

    public int getTrunkState() {
        return getInt(FidRegistry.DEV_BODYWORK, FidRegistry.FID_TRUNK);
    }

    public int getLockState() {
        return getInt(FidRegistry.DEV_LOCK, FidRegistry.FID_LOCK_FL);
    }

    // ---------- 行驶数据 ----------

    public float getSpeed() {
        return getFloat(FidRegistry.DEV_SPEED, FidRegistry.FID_SPEED);
    }

    public int getGear() {
        return getInt(FidRegistry.DEV_GEARBOX, FidRegistry.FID_GEAR);
    }

    public int getMotorPower() {
        return getInt(FidRegistry.DEV_MOTOR, FidRegistry.FID_MOTOR_POWER);
    }

    public int getWorkMode() {
        return getInt(FidRegistry.DEV_DRIVE_MODE, FidRegistry.FID_WORK_MODE);
    }

    public int getMileage() {
        return getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_MILEAGE);
    }

    // ---------- 电池扩展 ----------

    public Map<String, Object> readBatteryExtras() {
        Map<String, Object> data = new LinkedHashMap<>();

        int tempMax = getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_BATT_TEMP_MAX);
        if (!FidRegistry.isSentinel(tempMax) && tempMax >= 0) data.put("batteryTempMax", tempMax - 40);

        int tempMin = getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_BATT_TEMP_MIN);
        if (!FidRegistry.isSentinel(tempMin) && tempMin >= 0) data.put("batteryTempMin", tempMin - 40);

        int cellMax = getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_CELL_VOLT_MAX);
        if (!FidRegistry.isSentinel(cellMax) && cellMax > 0) data.put("cellVoltageMax", cellMax);

        int cellMin = getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_CELL_VOLT_MIN);
        if (!FidRegistry.isSentinel(cellMin) && cellMin > 0) data.put("cellVoltageMin", cellMin);

        int soh = getInt(FidRegistry.DEV_STATISTIC, FidRegistry.FID_SOH);
        if (!FidRegistry.isSentinel(soh) && soh >= 0) data.put("soh", soh);

        return data;
    }

    public Map<String, Object> readVehicleExtras() {
        Map<String, Object> data = new LinkedHashMap<>();

        float v12 = getFloat(FidRegistry.DEV_BODYWORK, FidRegistry.FID_12V_VOLTAGE);
        if (!FidRegistry.isSentinelFloat(v12) && v12 > 0) data.put("voltage12v", (double) v12);

        int chargeGun = getInt(FidRegistry.DEV_CHARGE, FidRegistry.FID_CHARGE_GUN);
        if (!FidRegistry.isSentinel(chargeGun)) data.put("chargeGunState", chargeGun);

        int powerState = getInt(FidRegistry.DEV_POWER, FidRegistry.FID_POWER_STATE);
        if (!FidRegistry.isSentinel(powerState)) data.put("powerState", powerState);

        int driveMode = getInt(FidRegistry.DEV_DRIVE_MODE, FidRegistry.FID_DRIVE_MODE);
        if (!FidRegistry.isSentinel(driveMode)) data.put("driveMode", driveMode);

        int motorPower = getInt(FidRegistry.DEV_MOTOR, FidRegistry.FID_MOTOR_POWER);
        if (!FidRegistry.isSentinel(motorPower)) data.put("motorPowerKw", motorPower);

        return data;
    }

    // ---------- 命令执行 ----------

    private String execShell(String cmd) {
        // 优先通过 dadb（shell UID）执行
        Dadb dadb = AdbHelper.getSharedDadb();
        if (dadb != null) {
            try {
                AdbShellResponse resp = dadb.shell(cmd);
                if (resp.getExitCode() == 0) {
                    return resp.getAllOutput();
                }
            } catch (Exception e) {
                Log.w(TAG, "dadb shell failed: " + cmd, e);
            }
        }

        // 降级：Runtime.exec（app UID，可能无权限）
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "exec failed: " + cmd, e);
            return null;
        }
    }

    // ---------- Parcel 解析 ----------

    public static int parseParcelInt(String parcelOutput) {
        if (parcelOutput == null || parcelOutput.isEmpty()) return -1;
        try {
            Matcher m = PARCEL_HEX_PATTERN.matcher(parcelOutput);
            if (!m.find()) return -1;
            String hex = m.group(1).replaceAll("\\s+", "");
            if (hex.length() < 16) return -1;
            String valueHex = hex.substring(8, 16);
            return (int) Long.parseLong(valueHex, 16);
        } catch (Exception e) {
            Log.w(TAG, "parseParcelInt failed: " + parcelOutput, e);
            return -1;
        }
    }

    public static float parseParcelFloat(String parcelOutput) {
        int bits = parseParcelInt(parcelOutput);
        if (bits == -1) return -1.0f;
        return Float.intBitsToFloat(bits);
    }

    // ---------- FID 全量扫描 ----------

    public interface ScanCallback {
        void onResult(String report);
    }

    /**
     * 扫描所有已知设备和 FID，标注哪些返回真实值、哪些是哨兵。
     */
    public static void scanAll(ScanCallback callback) {
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("===== FID 全量扫描 =====\n\n");

            dadb.Dadb dadb = AdbHelper.getSharedDadb();
            if (dadb == null) {
                sb.append("✗ Dadb 未连接，请先完成 ADB 授权\n");
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onResult(sb.toString()));
                return;
            }

            Object[][] sections = {
                {"AC (1000)", FidRegistry.DEV_AC, new Object[][]{
                    {"AC 开关", 5, FidRegistry.FID_AC_STATE},
                    {"AC 温度", 5, FidRegistry.FID_AC_TEMP},
                    {"车内温度", 5, FidRegistry.FID_CABIN_TEMP},
                    {"车外温度", 5, FidRegistry.FID_OUTSIDE_TEMP},
                    {"风量", 5, FidRegistry.FID_AC_FAN},
                    {"AC循环", 5, FidRegistry.FID_AC_CYCLE},
                }},
                {"Bodywork (1001)", FidRegistry.DEV_BODYWORK, new Object[][]{
                    {"车门左前", 5, FidRegistry.FID_DOOR_FL},
                    {"车门右前", 5, FidRegistry.FID_DOOR_FR},
                    {"车门左后", 5, FidRegistry.FID_DOOR_RL},
                    {"车门右后", 5, FidRegistry.FID_DOOR_RR},
                    {"车窗左前", 5, FidRegistry.FID_WINDOW_FL},
                    {"车窗右前", 5, FidRegistry.FID_WINDOW_FR},
                    {"车窗左后", 5, FidRegistry.FID_WINDOW_RL},
                    {"车窗右后", 5, FidRegistry.FID_WINDOW_RR},
                    {"12V电压", 7, FidRegistry.FID_12V_VOLTAGE},
                    {"引擎盖", 5, FidRegistry.FID_HOOD},
                    {"后备箱", 5, FidRegistry.FID_TRUNK},
                }},
                {"Battery (1014)", FidRegistry.DEV_STATISTIC, new Object[][]{
                    {"电量SOC", 7, FidRegistry.FID_SOC},
                    {"SOH", 5, FidRegistry.FID_SOH},
                    {"里程", 5, FidRegistry.FID_MILEAGE},
                    {"电池温max", 5, FidRegistry.FID_BATT_TEMP_MAX},
                    {"电池温min", 5, FidRegistry.FID_BATT_TEMP_MIN},
                    {"电芯压max", 5, FidRegistry.FID_CELL_VOLT_MAX},
                    {"电芯压min", 5, FidRegistry.FID_CELL_VOLT_MIN},
                    {"累计能耗", 7, FidRegistry.FID_TOTAL_ELEC_CON},
                }},
                {"Speed (1013)", FidRegistry.DEV_SPEED, new Object[][]{
                    {"速度", 7, FidRegistry.FID_SPEED},
                }},
                {"Gearbox (1011)", FidRegistry.DEV_GEARBOX, new Object[][]{
                    {"挡位", 5, FidRegistry.FID_GEAR},
                }},
                {"Tire (1016)", FidRegistry.DEV_TIRE, new Object[][]{
                    {"胎压左前", 5, FidRegistry.FID_TIRE_FL},
                    {"胎压右前", 5, FidRegistry.FID_TIRE_FR},
                    {"胎压左后", 5, FidRegistry.FID_TIRE_RL},
                    {"胎压右后", 5, FidRegistry.FID_TIRE_RR},
                }},
                {"Charge (1009)", FidRegistry.DEV_CHARGE, new Object[][]{
                    {"充电枪", 5, FidRegistry.FID_CHARGE_GUN},
                }},
                {"Power (1023)", FidRegistry.DEV_POWER, new Object[][]{
                    {"电源状态", 5, FidRegistry.FID_POWER_STATE},
                }},
                {"DriveMode (1006)", FidRegistry.DEV_DRIVE_MODE, new Object[][]{
                    {"驱动模式", 5, FidRegistry.FID_DRIVE_MODE},
                    {"工作模式", 5, FidRegistry.FID_WORK_MODE},
                }},
                {"Motor (1012)", FidRegistry.DEV_MOTOR, new Object[][]{
                    {"电机功率", 5, FidRegistry.FID_MOTOR_POWER},
                }},
                {"Lock (1032)", FidRegistry.DEV_LOCK, new Object[][]{
                    {"门锁FL", 5, FidRegistry.FID_LOCK_FL},
                }},
                {"Light (1004)", FidRegistry.DEV_LIGHT, new Object[][]{
                    {"近光灯", 5, FidRegistry.FID_LIGHT_LOW},
                    {"日行灯", 5, FidRegistry.FID_DRL},
                }},
                {"Safety (1007)", FidRegistry.DEV_SAFETY, new Object[][]{
                    {"安全带FL", 5, FidRegistry.FID_SEATBELT_FL},
                }},
            };

            for (Object[] section : sections) {
                String devName = (String) section[0];
                int dev = (int) section[1];
                Object[][] fids = (Object[][]) section[2];

                sb.append("── ").append(devName).append(" ──\n");
                for (Object[] entry : fids) {
                    String name = (String) entry[0];
                    int tx = (int) entry[1];
                    int fid = (int) entry[2];
                    String cmd = "service call autoservice " + tx + " i32 " + dev + " i32 " + fid;
                    try {
                        dadb.AdbShellResponse resp = dadb.shell(cmd);
                        String raw = resp.getAllOutput().trim();
                        if (tx == 7) {
                            float val = parseParcelFloat(raw);
                            sb.append("  ").append(name).append(": ").append(val).append(" ✓\n");
                        } else {
                            int val = parseParcelInt(raw);
                            if (FidRegistry.isSentinel(val)) {
                                sb.append("  ").append(name).append(": ").append(val).append(" ✗\n");
                            } else {
                                sb.append("  ").append(name).append(": ").append(val).append(" ✓\n");
                            }
                        }
                    } catch (Exception e) {
                        sb.append("  ").append(name).append(": ERR ").append(e.getMessage()).append("\n");
                    }
                }
                sb.append("\n");
            }

            sb.append("===== 扫描完成 =====");
            String report = sb.toString();
            android.util.Log.i("FidScan", report);
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> callback.onResult(report));
        }).start();
    }

    // ---------- Content Provider 读取 ----------

    /**
     * 探测所有可能的车辆数据通路，包括 AAOS CarPropertyManager、
     * ICarPropertyService（DiCar）、CarSettingsProvider 等。
     */
    public static void probeContentProviders(ScanCallback callback) {
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("===== Content Provider & Car API 探测 =====\n\n");

            dadb.Dadb dadb = AdbHelper.getSharedDadb();
            if (dadb == null) {
                sb.append("✗ Dadb 未连接\n");
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onResult(sb.toString()));
                return;
            }

            // 1. AAOS CarPropertyManager（最重要！）
            sb.append("── AAOS CarPropertyManager 探测 ──\n");
            try {
                // 检查 android.car 包是否存在
                dadb.AdbShellResponse r = dadb.shell(
                    "dumpsys car_service 2>&1 | head -20 || echo 'car_service 不存在'");
                sb.append(r.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            // AAOS 标准属性 ID（VehicleProperty）
            sb.append("\n── AAOS VehicleProperty 探测 ──\n");
            try {
                // 通过 dumpsys vehiclehal 查看支持的属性
                dadb.AdbShellResponse r = dadb.shell(
                    "dumpsys vehiclehal 2>&1 | grep -iE 'range|fuel|ev|battery|soc|speed|mileage' | head -30 || echo 'vehiclehal 不可用'");
                sb.append(r.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            // 2. ICarPropertyService (DiCar 路径)
            sb.append("\n── ICarPropertyService (DiCar) ──\n");
            try {
                dadb.AdbShellResponse r = dadb.shell(
                    "content query --uri content://com.byd.car.server.provider.CarServiceProvider/sync_binder 2>&1 | head -10");
                sb.append(r.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            // 3. CarStatusProvider（已知可用）
            sb.append("\n── CarStatusProvider ──\n");
            try {
                dadb.AdbShellResponse r = dadb.shell(
                    "content query --uri content://com.byd.carStatusProvider/car_status 2>&1 | grep -v 'travel_points' | head -30");
                sb.append(r.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            // 4. CarSettingsProvider 各路径
            sb.append("\n── CarSettingsProvider 路径探测 ──\n");
            String[] csPaths = {
                "content://com.byd.providers.carsettings",
                "content://com.byd.providers.carsettings/travel",
                "content://com.byd.providers.carsettings/config",
                "content://com.byd.providers.carsettings/global",
                "content://com.byd.providers.carsettings/car",
                "content://com.byd.providers.carsettings/vehicle",
                "content://com.byd.providers.carsettings/range",
                "content://com.byd.providers.carsettings/fuel",
            };
            for (String uri : csPaths) {
                try {
                    dadb.AdbShellResponse r = dadb.shell(
                        "content query --uri " + uri + " 2>&1 | head -10");
                    String out = r.getAllOutput().trim();
                    if (!out.isEmpty() && !out.contains("Exception") && !out.contains("No result found")) {
                        sb.append("\n✓ ").append(uri).append(":\n");
                        sb.append(out).append("\n");
                    }
                } catch (Exception ignored) {}
            }

            // 5. 查询所有已注册 ContentProvider 找线索
            sb.append("\n── 系统注册的 Car 相关 Provider ──\n");
            try {
                dadb.AdbShellResponse r = dadb.shell(
                    "dumpsys package providers 2>/dev/null | grep -iE 'byd|car|vehicle|fuel|range|energy' | grep -i 'provider\\|authority' | head -30");
                sb.append(r.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            // 6. dumpsys activity providers（找 BYD ContentProvider）
            sb.append("\n── BYD Content Providers (authorities) ──\n");
            try {
                dadb.AdbShellResponse r = dadb.shell(
                    "content query --uri content://settings/system/car_fuel_range 2>&1 | head -5\n" +
                    "content query --uri content://settings/system/car_ev_range 2>&1 | head -5\n" +
                    "content query --uri content://settings/global/car_fuel_level 2>&1 | head -5");
                sb.append(r.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            // 7. 查看 DiCarServer 暴露了哪些服务
            sb.append("\n── DiCarServer 暴露的 Binder 服务 ──\n");
            try {
                dadb.AdbShellResponse r = dadb.shell(
                    "service list 2>/dev/null | grep -iE 'byd|car|vehicle|property|dicar|diplus' | head -20");
                sb.append(r.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            // 8. 系统属性里的车辆数据
            sb.append("\n── 系统属性（车辆数据）──\n");
            try {
                dadb.AdbShellResponse r = dadb.shell(
                    "getprop 2>/dev/null | grep -iE 'fuel|range|ev|soc|battery|mileage|vehicle' | head -20");
                sb.append(r.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            // 9. 查 vehiclehal 支持的完整属性列表
            sb.append("\n── VehicleHAL 属性列表 ──\n");
            try {
                dadb.AdbShellResponse r = dadb.shell(
                    "dumpsys vehiclehal 2>&1 | grep 'prop:' | head -50 || echo '无 vehiclehal'");
                sb.append(r.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            // 10. GPack VehicleServiceProvider
            sb.append("\n── GPack VehicleServiceProvider ──\n");
            try {
                dadb.AdbShellResponse r = dadb.shell(
                    "content query --uri content://com.gpack.service.provider.VehicleServiceProvider/ 2>&1 | head -30");
                sb.append(r.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            // 11. System Settings 车辆相关 key
            sb.append("\n── System Settings (车辆 key) ──\n");
            String[] settingsKeys = {
                "KEY_VEHICLE_TYPE", "KEY_REALLY_CARMODE", "EXIST_CAR_SETTINGS",
                "EXIST_CAR_WINDOW", "car_fuel_range", "car_ev_range", "car_hev_range",
                "car_fuel_percent", "car_soc", "car_speed", "car_gear",
                "car_total_mileage", "car_ev_mileage", "car_fuel_mileage"
            };
            for (String key : settingsKeys) {
                try {
                    dadb.AdbShellResponse r = dadb.shell("settings get system " + key + " 2>&1");
                    String val = r.getAllOutput().trim();
                    if (!val.isEmpty() && !val.equals("null")) {
                        sb.append("  ").append(key).append(" = ").append(val).append("\n");
                    }
                } catch (Exception ignored) {}
            }

            // 12. 全量 system settings 车辆关键词过滤
            sb.append("\n── System Settings (关键词: fuel/ev/range/soc/car) ──\n");
            try {
                dadb.AdbShellResponse r = dadb.shell(
                    "settings list system 2>/dev/null | grep -iE 'fuel|ev_range|hev|range|soc|speed|gear|mileage|oil|power' | head -40");
                String out = r.getAllOutput().trim();
                if (!out.isEmpty()) {
                    sb.append(out).append("\n");
                } else {
                    sb.append("  (无匹配)\n");
                }
            } catch (Exception e) {
                sb.append("✗ ").append(e.getMessage()).append("\n");
            }

            sb.append("\n===== 探测完成 =====");
            String report = sb.toString();
            android.util.Log.i("ContentProviderProbe", report);
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> callback.onResult(report));
        }).start();
    }

    /**
     * URI: content://com.byd.carStatusProvider/car_status
     * 无需特殊权限，ADB shell 直接可读。
     */
    public com.diui.launcher.model.VehicleStatus.MaintenanceInfo readMaintenanceInfo() {
        String cmd = "content query --uri content://com.byd.carStatusProvider/car_status 2>/dev/null";
        String result = execShell(cmd);
        if (result == null || result.isEmpty()) return null;

        int daysLeft = -1, mileLeft = -1, issueNum = 0;
        for (String line : result.split("\n")) {
            line = line.trim();
            if (line.contains("key=car_status_maintenance_time")) {
                daysLeft = extractIntValue(line);
            } else if (line.contains("key=car_status_maintenance_mile")) {
                mileLeft = extractIntValue(line);
            } else if (line.contains("key=car_status_issue_num")) {
                issueNum = extractIntValue(line);
            }
        }
        return new com.diui.launcher.model.VehicleStatus.MaintenanceInfo(daysLeft, mileLeft, issueNum);
    }

    private static int extractIntValue(String line) {
        // 格式: "Row: X id=Y, key=..., value=Z"
        int idx = line.lastIndexOf("value=");
        if (idx < 0) return -1;
        try {
            return Integer.parseInt(line.substring(idx + 6).trim());
        } catch (Exception e) {
            return -1;
        }
    }

    // ---------- 暴力扫描（发现未知 FID）----------

    /**
     * 批量暴力扫描：把 FID 列表写入临时文件，在车机 shell 本地循环。
     * 每个设备类型只发两次 dadb.shell()，避免单 FID 网络往返。
     */
    public static void bruteForceScan(int[] devTypes, int[] fidCandidates, ScanCallback callback) {
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("===== 暴力 FID 扫描 =====\n");
            sb.append("设备数: ").append(devTypes.length)
              .append("  FID候选数: ").append(fidCandidates.length).append("\n\n");

            dadb.Dadb dadb = AdbHelper.getSharedDadb();
            if (dadb == null) {
                sb.append("✗ Dadb 未连接\n");
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onResult(sb.toString()));
                return;
            }

            // 把 FID 列表写入车机临时文件（逐行），shell 脚本从文件读取
            StringBuilder fidLines = new StringBuilder();
            for (int fid : fidCandidates) fidLines.append(fid).append("\n");

            try {
                // 写 FID 列表到车机临时文件
                dadb.shell("printf '" + fidLines.toString().replace("'", "") + "' > /data/local/tmp/_fids.txt");
            } catch (Exception e) {
                sb.append("✗ 写临时文件失败: ").append(e.getMessage()).append("\n");
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(() -> callback.onResult(sb.toString()));
                return;
            }

            int found = 0;
            for (int dev : devTypes) {
                int devFound = 0;
                StringBuilder devSb = new StringBuilder();

                // tx=5 (int) — 在车机本地循环
                String cmd5 = "while IFS= read -r fid; do"
                    + " r=$(service call autoservice 5 i32 " + dev + " i32 $fid 2>/dev/null);"
                    + " v=$(echo \"$r\" | sed -n 's/.*Parcel([0-9a-f]* \\([0-9a-f]*\\).*/\\1/p');"
                    + " case \"$v\" in 0000ffff|000fffff|ffffd8e3|ffffd8e5|00000000|ffffffff|'') ;;"
                    + " *) echo \"I $fid $v\";; esac;"
                    + " done < /data/local/tmp/_fids.txt";

                // tx=7 (float)
                String cmd7 = "while IFS= read -r fid; do"
                    + " r=$(service call autoservice 7 i32 " + dev + " i32 $fid 2>/dev/null);"
                    + " v=$(echo \"$r\" | sed -n 's/.*Parcel([0-9a-f]* \\([0-9a-f]*\\).*/\\1/p');"
                    + " case \"$v\" in 0000ffff|000fffff|ffffd8e3|ffffd8e5|00000000|ffffffff|bf800000|7fc00000|'') ;;"
                    + " *) echo \"F $fid $v\";; esac;"
                    + " done < /data/local/tmp/_fids.txt";

                try {
                    String out5 = dadb.shell(cmd5).getAllOutput().trim();
                    for (String line : out5.split("\n")) {
                        String[] p = line.trim().split("\\s+");
                        if (p.length == 3 && p[0].equals("I")) {
                            try {
                                int fid = Integer.parseInt(p[1]);
                                int val = (int) Long.parseLong(p[2], 16);
                                devSb.append("  [INT] ").append(fid)
                                     .append(" = ").append(val).append("\n");
                                devFound++;
                            } catch (Exception ignored) {}
                        }
                    }

                    String out7 = dadb.shell(cmd7).getAllOutput().trim();
                    for (String line : out7.split("\n")) {
                        String[] p = line.trim().split("\\s+");
                        if (p.length == 3 && p[0].equals("F")) {
                            try {
                                int fid = Integer.parseInt(p[1]);
                                float fval = Float.intBitsToFloat((int) Long.parseLong(p[2], 16));
                                if (fval > 0 && fval < 1e9f && !Float.isInfinite(fval)) {
                                    devSb.append("  [FLT] ").append(fid)
                                         .append(" = ").append(fval).append("\n");
                                    devFound++;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    sb.append("dev=").append(dev).append(" ERR: ").append(e.getMessage()).append("\n");
                }

                if (devFound > 0) {
                    sb.append("── dev=").append(dev).append(" (").append(devFound).append(") ──\n");
                    sb.append(devSb);
                    found += devFound;
                }
            }

            // 清理临时文件
            try { dadb.shell("rm /data/local/tmp/_fids.txt"); } catch (Exception ignored) {}

            sb.append("\n===== 共发现 ").append(found).append(" 个有效 FID =====");
            String report = sb.toString();
            android.util.Log.i("FidBrute", report);
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> callback.onResult(report));
        }).start();
    }
}
