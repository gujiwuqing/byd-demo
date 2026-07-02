package com.diui.launcher.api;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diplus（迪加）本地 HTTP API 客户端。
 *
 * Diplus 监听 0.0.0.0:8988，app 在车机上直接 HttpURLConnection 请求 localhost。
 *
 * API 格式（源码验证自 cnjackchen/diplus-www + jkaberg/byd-hass）：
 *
 *   getVal:
 *     GET /api/getVal?name=<URL编码的中文名>&status=true
 *     响应: {"val": <数值>} 或 {"success":false}
 *
 *   getDiPars:
 *     GET /api/getDiPars?text=<URL编码的模板字符串>
 *     模板格式: key:{中文名}|key:{中文名}  （数值用花括号）
 *             key:[中文名]|key:[中文名]  （文本描述用方括号）
 *     响应: {"success":true, "val":"key:value|key:value"}
 *     val 是字符串，按 | 拆分得到 key:value 对
 */
public class DiplusClient {

    private static final String TAG = "DiplusClient";
    private static final String BASE_URL = "http://localhost:8988";
    private static final int TIMEOUT_MS = 3000;

    /**
     * 传感器定义：{key, 中文名, 说明, 缩放因子, 括号类型}
     * 括号类型: "{}" 表示取数值，"[]" 表示取文本描述
     *
     * 来源：cnjackchen/diplus-www 的 diplus_param_list.php（109+ 个参数 ID）
     */
    private static final String[][] SENSORS = {
            // ── 核心行驶数据 ──
            {"power_status",      "电源状态",        "电源状态",          "1",    "{}"},  // ID 1
            {"speed",             "车速",            "当前车速 km/h",     "1",    "{}"},  // ID 2
            {"mileage",           "里程",            "总里程 ×0.1=km",    "0.1",  "{}"},  // ID 3
            {"gear",              "档位",            "P/R/N/D",          "1",    "[]"},  // ID 4
            {"engine_rpm",        "发动机转速",       "RPM",              "1",    "{}"},  // ID 5
            {"brake_depth",       "刹车深度",         "刹车踏板 %",        "1",    "{}"},  // ID 6
            {"accel_depth",       "加速踏板深度",     "油门踏板 %",        "1",    "{}"},  // ID 7
            {"front_motor_rpm",   "前电机转速",       "RPM",              "1",    "{}"},  // ID 8
            {"rear_motor_rpm",    "后电机转速",       "RPM",              "1",    "{}"},  // ID 9
            {"engine_power",      "发动机功率",       "kW",               "1",    "{}"},  // ID 10
            {"front_motor_torque","前电机扭矩",       "Nm",               "1",    "{}"},  // ID 11
            {"charge_gun",        "充电枪插枪状态",   "充电枪",            "1",    "{}"},  // ID 12
            {"elec_per_100km",    "百公里电耗",       "kWh/100km",        "1",    "{}"},  // ID 13

            // ── 电池 ──
            {"batt_temp_max",     "最高电池温度",     "°C",               "1",    "{}"},  // ID 14
            {"batt_temp_avg",     "平均电池温度",     "°C",               "1",    "{}"},  // ID 15
            {"batt_temp_min",     "最低电池温度",     "°C",               "1",    "{}"},  // ID 16
            {"batt_volt_max",     "最高电池电压",     "V",                "1",    "{}"},  // ID 17
            {"batt_volt_min",     "最低电池电压",     "V",                "1",    "{}"},  // ID 18

            // ── 安全带/锁 ──
            {"seatbelt_driver",   "主驾驶安全带状态", "0/1",              "1",    "{}"},  // ID 21
            {"remote_lock",       "远程锁车状态",     "0/1",              "1",    "{}"},  // ID 22

            // ── 温度 ──
            {"cabin_temp",        "车内温度",         "°C",               "1",    "{}"},  // ID 25
            {"outside_temp",      "车外温度",         "°C",               "1",    "{}"},  // ID 26
            {"ac_temp_driver",    "主驾驶空调温度",   "°C",               "1",    "{}"},  // ID 27
            {"temp_unit",         "温度单位",         "单位",              "1",    "{}"},  // ID 28

            // ── 电池/燃油 ──
            {"batt_capacity",     "电池容量",         "kWh",              "1",    "{}"},  // ID 29
            {"steering_angle",    "方向盘转角",       "度",               "1",    "{}"},  // ID 30
            {"steering_speed",    "方向盘转速",       "度/秒",             "1",    "{}"},  // ID 31
            {"total_elec_con",    "总电耗",           "kWh",              "1",    "{}"},  // ID 32
            {"battery_percent",   "电量百分比",       "%",                "1",    "{}"},  // ID 33
            {"fuel_percent",      "油量百分比",       "%",                "1",    "{}"},  // ID 34
            {"total_fuel_con",    "总燃油消耗",       "L",                "1",    "{}"},  // ID 35

            // ── 12V / 雷达 ──
            {"voltage_12v",       "蓄电池电压",       "V",                "1",    "{}"},  // ID 39

            // ── 胎压（注意：中文名含"轮"字！） ──
            {"tire_fl",           "左前轮气压",       "bar ×0.01",        "0.01", "{}"},  // ID 53
            {"tire_fr",           "右前轮气压",       "bar ×0.01",        "0.01", "{}"},  // ID 54
            {"tire_rl",           "左后轮气压",       "bar ×0.01",        "0.01", "{}"},  // ID 55
            {"tire_rr",           "右后轮气压",       "bar ×0.01",        "0.01", "{}"},  // ID 56

            // ── 充电状态 ──
            {"charge_status",     "充电状态",         "充电状态",          "1",    "[]"},  // ID 52

            // ── 车窗/天窗 ──
            {"window_fl",         "左前车窗打开百分比", "%",              "1",    "{}"},  // ID 61
            {"window_fr",         "右前车窗打开百分比", "%",              "1",    "{}"},  // ID 62
            {"window_rl",         "左后车窗打开百分比", "%",              "1",    "{}"},  // ID 63
            {"window_rr",         "右后车窗打开百分比", "%",              "1",    "{}"},  // ID 64
            {"sunroof",           "天窗打开百分比",     "%",              "1",    "{}"},  // ID 65

            // ── 工作模式 ──
            {"work_mode",         "整车工作模式",     "EV/HEV",           "1",    "[]"},  // ID 67
            {"run_mode",          "整车运行模式",     "ECO/SPORT",        "1",    "[]"},  // ID 68

            // ── 空调 ──
            {"ac_status",         "空调状态",         "开/关",            "1",    "{}"},  // ID 77
            {"ac_wind_level",     "空调风量",         "0~7",              "1",    "{}"},  // ID 78
            {"ac_cycle_mode",     "空调循环",         "内/外循环",         "1",    "{}"},  // ID 79
            {"ac_wind_mode",      "空调出风模式",     "吹面/吹脚/除霜",    "1",    "{}"},  // ID 80

            // ── 车门/引擎盖 ──
            {"door_fl",           "左前门",           "开/关",            "1",    "{}"},  // ID 81
            {"door_fr",           "右前门",           "开/关",            "1",    "{}"},  // ID 82
            {"door_rl",           "左后门",           "开/关",            "1",    "{}"},  // ID 83
            {"door_rr",           "右后门",           "开/关",            "1",    "{}"},  // ID 84
            {"hood",              "引擎盖",           "开/关",            "1",    "{}"},  // ID 85
            {"trunk",             "后备箱",           "开/关",            "1",    "{}"},  // ID 86
            {"fuel_cap",          "油箱盖",           "开/关",            "1",    "{}"},  // ID 87
            {"auto_parking",      "自动驻车",         "状态",             "1",    "{}"},  // ID 88

            // ── 门锁 ──
            {"lock_fl",           "主驾驶车门锁",     "锁/开",            "1",    "{}"},  // ID 93

            // ── 灯光 ──
            {"light_low",         "近光灯",           "开/关",            "1",    "{}"},  // ID 100
            {"light_high",        "远光灯",           "开/关",            "1",    "{}"},  // ID 101
            {"light_drl",         "日行灯",           "开/关",            "1",    "{}"},  // ID 107
            {"hazard",            "双闪",             "开/关",            "1",    "{}"},  // ID 109

            // ── 发动机 ──
            {"water_temp",        "发动机水温",       "°C",               "1",    "{}"},  // ID 108
    };

    // ── 工具方法 ─────────────────────────────────────────────────────

    private static String encode(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    private static String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            int code = conn.getResponseCode();
            if (code != 200) return null;
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString().trim();
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 从 getVal 响应提取 val。{"val": 85.5} → "85.5"
     */
    private static String parseGetVal(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has("val")) return obj.get("val").toString();
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 解析 getDiPars 响应。
     * 响应格式: {"success":true, "val":"key:value|key:value"}
     * 返回 key→value 的 Map。
     */
    private static Map<String, String> parseDiPars(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.isEmpty()) return map;
        try {
            JSONObject obj = new JSONObject(json);
            if (!obj.optBoolean("success", false)) return map;
            String valStr = obj.optString("val", "");
            if (valStr.isEmpty()) return map;

            // 按 |!| 或 | 拆分
            String[] pairs;
            if (valStr.contains("|!|")) {
                pairs = valStr.split("\\|!\\|");
            } else {
                pairs = valStr.split("\\|");
            }
            for (String pair : pairs) {
                // key:value 或 key|value
                int sep = pair.indexOf(':');
                if (sep < 0) sep = pair.indexOf('|');
                if (sep > 0 && sep < pair.length() - 1) {
                    String key = pair.substring(0, sep).trim();
                    String val = pair.substring(sep + 1).trim();
                    map.put(key, val);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "parseDiPars failed: " + e.getMessage());
        }
        return map;
    }

    /**
     * 构建 getDiPars 的 text 参数。
     * 格式: key:{中文名}|key:{中文名}  （数值）
     *       key:[中文名]|key:[中文名]  （文本）
     */
    private static String buildTextParam(String[][] sensors) {
        StringBuilder sb = new StringBuilder();
        for (String[] s : sensors) {
            if (sb.length() > 0) sb.append("|");
            String bracket = "{}".equals(s[4])
                    ? "{" + s[1] + "}"
                    : "[" + s[1] + "]";
            sb.append(s[0]).append(":").append(bracket);
        }
        return sb.toString();
    }

    // ── 连接测试（设置页按钮用）─────────────────────────────────────

    public static String testConnection() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Diplus API 连接测试 =====\n");
        sb.append("地址: ").append(BASE_URL).append("\n\n");

        // 1. 端口探测
        sb.append("── 步骤 1: 端口探测 ──\n");
        String pingOut = httpGet(BASE_URL + "/api/getVal?name=test");
        if (pingOut == null) {
            sb.append("  ✗ 端口 8988 无响应\n");
            sb.append("  请确认迪加(Diplus)应用已启动\n\n");
            sb.append("===== 测试终止 =====");
            return sb.toString();
        }
        sb.append("  ✓ 端口 8988 已响应: ").append(pingOut).append("\n\n");

        // 2. getVal 单传感器测试
        sb.append("── 步骤 2: getVal 接口测试 ──\n");
        String[][] quickTests = {
                {"电量百分比", "电池 SOC"},
                {"车速",       "车速"},
                {"车外温度",   "环境温度"},
                {"油量百分比", "燃油百分比"},
                {"里程",       "总里程"},
                {"发动机水温", "水温"},
                {"左前轮气压", "左前胎压"},
        };
        for (String[] t : quickTests) {
            String url = BASE_URL + "/api/getVal?name=" + encode(t[0]) + "&status=true";
            String out = httpGet(url);
            String val = parseGetVal(out);
            if (val != null) {
                sb.append("  ✓ ").append(t[0]).append(" = ").append(val).append("\n");
            } else {
                sb.append("  ✗ ").append(t[0])
                        .append(out != null ? " → " + out : " → 无响应").append("\n");
            }
        }

        // 3. getDiPars 批量测试
        sb.append("\n── 步骤 3: getDiPars 批量接口 ──\n");
        String text = "soc:{电量百分比}|spd:{车速}|tmp:{车外温度}|fuel:{油量百分比}";
        String batchUrl = BASE_URL + "/api/getDiPars?text=" + encode(text);
        String batchOut = httpGet(batchUrl);
        if (batchOut == null) {
            sb.append("  ✗ 无响应\n");
        } else {
            sb.append("  原始: ").append(batchOut).append("\n");
            Map<String, String> parsed = parseDiPars(batchOut);
            if (!parsed.isEmpty()) {
                sb.append("  ✓ 解析成功:\n");
                for (Map.Entry<String, String> e : parsed.entrySet()) {
                    sb.append("    ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
                }
            } else {
                sb.append("  ⚠ 解析为空（请检查 Diplus 版本）\n");
            }
        }

        sb.append("\n===== 测试完成 =====");
        return sb.toString();
    }

    // ── 传感器完整扫描（设置页按钮用）──────────────────────────────

    public static String fetchAllSensors() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Diplus 传感器完整扫描 =====\n");
        sb.append("共 ").append(SENSORS.length).append(" 个待测传感器\n\n");

        // 1. getDiPars 批量拉取
        sb.append("── 批量接口 getDiPars ──\n");
        String textParam = buildTextParam(SENSORS);
        String batchUrl = BASE_URL + "/api/getDiPars?text=" + encode(textParam);
        String batchOut = httpGet(batchUrl);
        Map<String, String> batch = new LinkedHashMap<>();

        if (batchOut == null) {
            sb.append("  ✗ 批量接口无响应，将逐个 getVal 验证\n\n");
        } else {
            sb.append("  响应 (").append(batchOut.length()).append(" 字符)\n");
            batch = parseDiPars(batchOut);
            if (batch.isEmpty()) {
                sb.append("  ⚠ 解析为空，将逐个 getVal 验证\n\n");
            } else {
                sb.append("  ✓ 解析到 ").append(batch.size()).append(" 个字段\n\n");
            }
        }

        // 2. 逐个验证
        sb.append("── 逐个验证 ──\n\n");
        sb.append(String.format("%-2s %-14s %-14s %-6s %s\n", "#", "传感器名", "说明", "缩放", "当前值"));
        sb.append("──────────────────────────────────────────────────\n");

        int found = 0, notFound = 0;
        for (int i = 0; i < SENSORS.length; i++) {
            String key   = SENSORS[i][0];
            String name  = SENSORS[i][1];
            String desc  = SENSORS[i][2];
            String scale = SENSORS[i][3];

            // 优先从批量结果取
            String valStr = batch.get(key);

            // 批量没有则单独 getVal
            if (valStr == null || valStr.isEmpty()) {
                String url = BASE_URL + "/api/getVal?name=" + encode(name) + "&status=true";
                String out = httpGet(url);
                valStr = parseGetVal(out);
            }

            if (valStr != null && !valStr.isEmpty()) {
                String display = valStr;
                if (!"1".equals(scale)) {
                    try {
                        double scaled = Double.parseDouble(valStr) * Double.parseDouble(scale);
                        display = valStr + " ×" + scale + "=" + String.format("%.3f", scaled);
                    } catch (Exception ignored) {}
                }
                sb.append(String.format("✓ %2d. %-12s %-14s %s\n", i + 1, name, desc, display));
                found++;
            } else {
                sb.append(String.format("✗ %2d. %-12s %-14s -\n", i + 1, name, desc));
                notFound++;
            }
        }

        sb.append("──────────────────────────────────────────────────\n");
        sb.append("可用: ").append(found)
                .append("  不可用: ").append(notFound).append("\n");
        sb.append("\n===== 扫描完成 =====");
        return sb.toString();
    }

    // ── 填充 VehicleStatus ──────────────────────────────────────────

    /**
     * 从 Diplus 读取车辆数据，填充 VehicleStatus 中仍为 -1 的字段。
     * 在后台线程调用。
     *
     * @return 成功填充的字段数；-1 表示 Diplus 无响应
     */
    public static int fillVehicleStatus(com.diui.launcher.model.VehicleStatus s) {
        // 连通性检查
        String ping = httpGet(BASE_URL + "/api/getVal?name=test");
        if (ping == null) return -1;

        // 批量拉取
        String textParam = buildTextParam(SENSORS);
        String batchOut = httpGet(BASE_URL + "/api/getDiPars?text=" + encode(textParam));
        Map<String, String> data = parseDiPars(batchOut);

        // 如果批量失败，逐个 getVal 降级
        if (data.isEmpty()) {
            for (String[] sensor : SENSORS) {
                String url = BASE_URL + "/api/getVal?name=" + encode(sensor[1]) + "&status=true";
                String out = httpGet(url);
                String val = parseGetVal(out);
                if (val != null) data.put(sensor[0], val);
            }
        }

        if (data.isEmpty()) return 0;

        int count = 0;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            String valStr = entry.getValue();

            double raw;
            try { raw = Double.parseDouble(valStr); }
            catch (Exception e) { continue; }

            // 查找 scale
            double scale = 1.0;
            for (String[] sensor : SENSORS) {
                if (sensor[0].equals(key)) {
                    try { scale = Double.parseDouble(sensor[3]); } catch (Exception ignored) {}
                    break;
                }
            }
            double val = raw * scale;
            int iv = (int) Math.round(val);

            boolean filled = true;
            switch (key) {
                case "battery_percent":   if (s.batteryPercent < 0)          { s.batteryPercent = iv; }          else filled = false; break;
                case "speed":             if (s.speed < 0)                   { s.speed = iv; }                  else filled = false; break;
                case "mileage":           if (s.totalMileage < 0)            { s.totalMileage = val; }           else filled = false; break;
                case "fuel_percent":      if (s.fuelPercent < 0)             { s.fuelPercent = iv; }             else filled = false; break;
                case "outside_temp":      if (s.outsideTemp < 0)             { s.outsideTemp = iv; }             else filled = false; break;
                case "elec_per_100km":    if (s.currentElecConsumption < 0)  { s.currentElecConsumption = val; } else filled = false; break;
                case "tire_fl":           if (s.tirePressureFL < 0)          { s.tirePressureFL = (int)(raw * 100); } else filled = false; break;
                case "tire_fr":           if (s.tirePressureFR < 0)          { s.tirePressureFR = (int)(raw * 100); } else filled = false; break;
                case "tire_rl":           if (s.tirePressureRL < 0)          { s.tirePressureRL = (int)(raw * 100); } else filled = false; break;
                case "tire_rr":           if (s.tirePressureRR < 0)          { s.tirePressureRR = (int)(raw * 100); } else filled = false; break;
                case "batt_temp_max":     if (s.batteryTempMax < 0)          { s.batteryTempMax = iv; }          else filled = false; break;
                case "batt_temp_min":     if (s.batteryTempMin < 0)          { s.batteryTempMin = iv; }          else filled = false; break;
                case "batt_volt_max":     if (s.cellVoltageMax < 0)          { s.cellVoltageMax = (int)(raw * 1000); } else filled = false; break;
                case "batt_volt_min":     if (s.cellVoltageMin < 0)          { s.cellVoltageMin = (int)(raw * 1000); } else filled = false; break;
                case "voltage_12v":       if (s.voltage12v < 0)              { s.voltage12v = val; }             else filled = false; break;
                default: filled = false; break;
            }
            if (filled) count++;
        }

        return count;
    }
}
