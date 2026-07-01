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

    public int getAcWind() {
        return getInt(FidRegistry.DEV_AC, FidRegistry.FID_AC_WIND);
    }

    // ---------- 车身数据 ----------

    public int getBatteryCapacity() {
        return getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_SOC);
    }

    public int getDoorState(int fid) {
        return getInt(FidRegistry.DEV_BODYWORK, fid);
    }

    public int getWindowState(int fid) {
        return getInt(FidRegistry.DEV_BODYWORK, fid);
    }

    // ---------- 行驶数据 ----------

    public int getSpeed() {
        return getInt(FidRegistry.DEV_SPEED, FidRegistry.FID_SPEED);
    }

    public int getGear() {
        return getInt(FidRegistry.DEV_GEARBOX, FidRegistry.FID_GEAR);
    }

    // ---------- 电池扩展 ----------

    public Map<String, Object> readBatteryExtras() {
        Map<String, Object> data = new LinkedHashMap<>();

        int tempMax = getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_BATT_TEMP_MAX);
        if (!FidRegistry.isSentinel(tempMax) && tempMax >= 0) data.put("batteryTempMax", tempMax - 40);

        int tempMin = getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_BATT_TEMP_MIN);
        if (!FidRegistry.isSentinel(tempMin) && tempMin >= 0) data.put("batteryTempMin", tempMin - 40);

        int cellMax = getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_CELL_VOLT_MAX);
        if (!FidRegistry.isSentinel(cellMax) && cellMax > 0) data.put("cellVoltageMax", cellMax);

        int cellMin = getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_CELL_VOLT_MIN);
        if (!FidRegistry.isSentinel(cellMin) && cellMin > 0) data.put("cellVoltageMin", cellMin);

        int soh = getInt(FidRegistry.DEV_BATTERY, FidRegistry.FID_SOH);
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
}
