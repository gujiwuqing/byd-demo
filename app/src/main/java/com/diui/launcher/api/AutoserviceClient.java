package com.diui.launcher.api;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoserviceClient {

    private static final String TAG = "AutoserviceClient";
    private static final Pattern PARCEL_HEX_PATTERN = Pattern.compile(
            "Result:\\s*Parcel\\(([0-9a-fA-F\\s]+)'");

    /** 仅允许 service call autoservice 的 GET（tx 5/7/9），拦截一切写操作与任意命令。 */
    private static final Pattern COMMAND_BARRIER = Pattern.compile(
            "^service call autoservice [579] i32 \\d+ i32 -?\\d+$");

    private boolean available = false;

    public AutoserviceClient(Context context) {
        this.available = AdbHelper.isAdbAvailable();
    }

    public boolean isAvailable() {
        return available;
    }

    public int getInt(int deviceType, int featureId) {
        String cmd = "service call autoservice " + FidRegistry.TX_GET_INT
                + " i32 " + deviceType + " i32 " + featureId;
        String result = execCommand(cmd);
        return parseParcelInt(result);
    }

    public float getFloat(int deviceType, int featureId) {
        String cmd = "service call autoservice " + FidRegistry.TX_GET_FLOAT
                + " i32 " + deviceType + " i32 " + featureId;
        String result = execCommand(cmd);
        return parseParcelFloat(result);
    }

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

    private String execCommand(String cmd) {
        if (!COMMAND_BARRIER.matcher(cmd).matches()) {
            Log.w(TAG, "Refused command (write barrier): " + cmd);
            return null;
        }
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "Shell exec failed: " + cmd, e);
            return null;
        }
    }

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
