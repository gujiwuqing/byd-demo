package com.diui.launcher.api;

import android.util.Log;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Iterator;

/**
 * Diplus（迪加）本地 HTTP API 客户端。
 * Diplus 在车机上监听 localhost:8988，暴露车辆传感器数据。
 *
 * API 说明：
 *  - getVal:   GET /api/getVal?name=<URL编码中文名>&status=true  → {"val": <数值>} 或 {"success":false}
 *  - getDiPars: GET /api/getDiPars?text=<传感器模板列表>          → JSON 对象，key=字段名 value=数值
 *    text 格式：{传感器中文名}|{传感器中文名}|...
 */
public class DiplusClient {

    private static final String TAG = "DiplusClient";
    private static final String BASE_URL = "http://localhost:8988";

    // 已知传感器：{中文名, 含义描述, 缩放因子字符串}
    private static final String[][] KNOWN_SENSORS = {
            {"电量百分比",    "电池 SOC",       "1"},
            {"车速",         "当前车速",        "1"},
            {"里程",         "总里程",          "0.1"},
            {"油量百分比",   "燃油百分比",       "1"},
            {"续航里程",     "总续航",          "1"},
            {"纯电续航里程", "纯电续航",         "1"},
            {"瞬时电耗",     "当前电耗",         "1"},
            {"瞬时油耗",     "当前油耗",         "1"},
            {"平均电耗",     "平均电耗",         "1"},
            {"平均油耗",     "平均油耗",         "1"},
            {"车外温度",     "环境温度",         "1"},
            {"车内温度",     "车内温度",         "1"},
            {"电池温度",     "电池温度",         "1"},
            {"左前胎压",     "左前胎压",         "0.01"},
            {"右前胎压",     "右前胎压",         "0.01"},
            {"左后胎压",     "左后胎压",         "0.01"},
            {"右后胎压",     "右后胎压",         "0.01"},
            {"发动机转速",   "发动机 RPM",       "1"},
            {"电机转速",     "电机 RPM",         "1"},
            {"方向角",       "方向盘角度",        "1"},
            {"电芯最高电压", "电芯最高压",        "1"},
            {"电芯最低电压", "电芯最低压",        "1"},
            {"充电状态",     "充电状态",          "1"},
            {"档位",         "当前档位",          "1"},
            {"HEV里程",      "混动里程",          "0.1"},
            {"EV里程",       "纯电里程",          "0.1"},
            {"电池健康度",   "SOH",              "1"},
            {"12V电压",      "蓄电池电压",        "1"},
            {"行驶时间",     "行驶时长",          "1"},
            {"钥匙电量",     "遥控钥匙电量",      "1"},
            {"水温",         "发动机水温",        "1"},
            {"制动深度",     "刹车踏板深度",      "1"},
            {"油门深度",     "油门踏板深度",      "1"},
    };

    // ----------------------------------------------------------------
    //  工具方法
    // ----------------------------------------------------------------

    /** URL 编码中文字符，失败时返回原字符串 */
    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    /**
     * 通过 ADB shell 执行 curl 命令并返回输出。
     * 失败返回空字符串。
     */
    private static String shellCurl(dadb.Dadb dadb, String url) {
        try {
            String cmd = "curl -s --connect-timeout 3 '" + url + "'";
            dadb.AdbShellResponse resp = dadb.shell(cmd);
            return resp.getAllOutput().trim();
        } catch (Exception e) {
            Log.w(TAG, "shellCurl failed: " + e.getMessage());
            return "";
        }
    }

    /**
     * 从 getVal 响应 JSON 中提取 val 字段。
     * 成功返回字符串数字，失败返回 null。
     */
    private static String parseVal(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has("val")) return obj.get("val").toString();
            if (obj.optBoolean("success", true) == false) return null;
        } catch (Exception ignored) {}
        return null;
    }

    // ----------------------------------------------------------------
    //  连接测试
    // ----------------------------------------------------------------

    /**
     * 测试 Diplus API 是否可达，输出诊断报告。
     */
    public static String testConnection() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Diplus API 连接测试 =====\n\n");

        dadb.Dadb dadb = AdbHelper.getSharedDadb();
        if (dadb == null) {
            sb.append("✗ ADB 未连接\n提示：请先确保车机 ADB 已连接\n");
            return sb.toString();
        }

        // 1. 检查进程
        sb.append("── 步骤 1: 检查 Diplus 进程 ──\n");
        try {
            String psOut = dadb.shell("ps -A | grep -i diplus").getAllOutput().trim();
            if (psOut.isEmpty()) {
                sb.append("  ⚠ 未检测到 Diplus 进程，请先启动迪加应用\n\n");
            } else {
                sb.append("  ✓ Diplus 进程运行中\n");
                for (String line : psOut.split("\n")) {
                    sb.append("  ").append(line.trim()).append("\n");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            sb.append("  ✗ 检查失败: ").append(e.getMessage()).append("\n\n");
        }

        // 2. 用 curl 探测端口（/dev/tcp 在部分车机不可用）
        sb.append("── 步骤 2: 测试端口 8988 ──\n");
        String portTest = shellCurl(dadb, BASE_URL + "/api/getVal?name=test");
        if (portTest.isEmpty()) {
            sb.append("  ✗ 端口 8988 无响应（Diplus 可能未运行或版本不支持）\n\n");
        } else {
            sb.append("  ✓ 端口 8988 已响应，原始内容: ").append(portTest).append("\n\n");
        }

        // 3. 测试 getVal 接口（URL 编码中文）
        sb.append("── 步骤 3: 测试 getVal 接口 ──\n");
        String[][] quickTests = {
                {"电量百分比", "电池 SOC"},
                {"车速",       "车速"},
                {"车外温度",   "环境温度"},
                {"水温",       "发动机水温"},
        };
        for (String[] t : quickTests) {
            String url = BASE_URL + "/api/getVal?name=" + encode(t[0]) + "&status=true";
            String out = shellCurl(dadb, url);
            String val = parseVal(out);
            if (val != null) {
                sb.append("  ✓ ").append(t[0]).append(" (").append(t[1]).append(") = ").append(val).append("\n");
            } else if (out.isEmpty()) {
                sb.append("  ✗ ").append(t[0]).append(": 无响应\n");
            } else {
                sb.append("  ⚠ ").append(t[0]).append(": ").append(out).append("\n");
            }
        }

        // 4. 测试 getDiPars 接口
        sb.append("\n── 步骤 4: 测试 getDiPars 接口 ──\n");
        try {
            // text 格式：{传感器名}|{传感器名}|...  值不需要 URL encode，参数整体 encode
            String textParam = encode("{电量百分比}|{车速}|{车外温度}");
            String url = BASE_URL + "/api/getDiPars?text=" + textParam;
            String out = shellCurl(dadb, url);
            if (out.isEmpty()) {
                sb.append("  ✗ 无响应\n");
            } else {
                sb.append("  原始: ").append(out).append("\n");
                try {
                    JSONObject json = new JSONObject(out);
                    sb.append("  ✓ JSON 解析成功，字段数: ").append(json.length()).append("\n");
                } catch (Exception e) {
                    sb.append("  ⚠ JSON 解析失败（可能格式不同）\n");
                }
            }
        } catch (Exception e) {
            sb.append("  ✗ ").append(e.getMessage()).append("\n");
        }

        sb.append("\n===== 测试完成 =====");
        return sb.toString();
    }

    // ----------------------------------------------------------------
    //  传感器完整扫描
    // ----------------------------------------------------------------

    /**
     * 逐个验证所有已知传感器，输出完整扫描报告。
     */
    public static String fetchAllSensors() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Diplus 传感器完整扫描 =====\n\n");

        dadb.Dadb dadb = AdbHelper.getSharedDadb();
        if (dadb == null) {
            sb.append("✗ ADB 未连接\n");
            return sb.toString();
        }

        // 1. 先用 getDiPars 批量拿一次
        sb.append("── 批量接口 getDiPars ──\n");
        try {
            StringBuilder textBuilder = new StringBuilder();
            for (String[] s : KNOWN_SENSORS) {
                if (textBuilder.length() > 0) textBuilder.append("|");
                textBuilder.append("{").append(s[0]).append("}");
            }
            String url = BASE_URL + "/api/getDiPars?text=" + encode(textBuilder.toString());
            String out = shellCurl(dadb, url);
            sb.append("  原始响应 (").append(out.length()).append(" 字符):\n  ").append(out).append("\n\n");
        } catch (Exception e) {
            sb.append("  ✗ ").append(e.getMessage()).append("\n\n");
        }

        // 2. 逐个 getVal 验证
        sb.append("── 逐个 getVal 验证 ──\n\n");
        sb.append(String.format("%-4s %-14s %-14s %-8s %s\n", "序号", "传感器名", "说明", "缩放", "当前值"));
        sb.append("─────────────────────────────────────────────────\n");

        int found = 0, notFound = 0;
        for (int i = 0; i < KNOWN_SENSORS.length; i++) {
            String name   = KNOWN_SENSORS[i][0];
            String desc   = KNOWN_SENSORS[i][1];
            String scale  = KNOWN_SENSORS[i][2];
            String url = BASE_URL + "/api/getVal?name=" + encode(name) + "&status=true";
            String out = shellCurl(dadb, url);
            String val = parseVal(out);
            if (val != null) {
                String display = val;
                if (!"1".equals(scale)) {
                    try {
                        double raw = Double.parseDouble(val);
                        double scaled = raw * Double.parseDouble(scale);
                        display = val + " →×" + scale + "= " + String.format("%.2f", scaled);
                    } catch (Exception ignored) {}
                }
                sb.append(String.format("  ✓ %2d. %-12s %-14s %s\n", i + 1, name, desc, display));
                found++;
            } else if (out.isEmpty()) {
                sb.append(String.format("  ✗ %2d. %-12s %-14s 无响应\n", i + 1, name, desc));
                notFound++;
            } else {
                sb.append(String.format("  ⚠ %2d. %-12s %-14s %s\n", i + 1, name, desc, out));
                notFound++;
            }
        }

        sb.append("─────────────────────────────────────────────────\n");
        sb.append("可用: ").append(found).append("  不可用/无响应: ").append(notFound).append("\n");
        sb.append("\n===== 扫描完成 =====");
        return sb.toString();
    }

    // ----------------------------------------------------------------
    //  读取车辆数据（供 BydVehicleManager 调用）
    // ----------------------------------------------------------------

    /**
     * 读取 Diplus 数据填充到 VehicleStatus，仅覆盖仍为 -1 的字段。
     *
     * @return 成功读取的传感器数量，-1 表示连接失败
     */
    public static int fillVehicleStatus(com.diui.launcher.model.VehicleStatus s) {
        dadb.Dadb dadb = AdbHelper.getSharedDadb();
        if (dadb == null) return -1;

        int count = 0;

        // 批量请求
        StringBuilder textBuilder = new StringBuilder();
        for (String[] sensor : KNOWN_SENSORS) {
            if (textBuilder.length() > 0) textBuilder.append("|");
            textBuilder.append("{").append(sensor[0]).append("}");
        }
        String batchUrl = BASE_URL + "/api/getDiPars?text=" + encode(textBuilder.toString());
        String batchOut = shellCurl(dadb, batchUrl);

        // 如果批量接口返回有效 JSON，直接解析
        JSONObject batch = null;
        try {
            if (!batchOut.isEmpty() && batchOut.startsWith("{")) {
                batch = new JSONObject(batchOut);
            }
        } catch (Exception ignored) {}

        // 逐个传感器填充
        for (String[] sensor : KNOWN_SENSORS) {
            String name  = sensor[0];
            String scale = sensor[2];
            String valStr = null;

            // 优先从批量结果取
            if (batch != null && batch.has(name)) {
                try { valStr = batch.get(name).toString(); } catch (Exception ignored) {}
            }

            // 批量没有则单独 getVal
            if (valStr == null) {
                String url = BASE_URL + "/api/getVal?name=" + encode(name) + "&status=true";
                String out = shellCurl(dadb, url);
                valStr = parseVal(out);
            }

            if (valStr == null) continue;

            double raw;
            try { raw = Double.parseDouble(valStr); }
            catch (Exception e) { continue; }

            double val = raw * Double.parseDouble(scale);
            int intVal = (int) Math.round(val);

            count++;
            switch (name) {
                case "电量百分比":
                    if (s.batteryPercent < 0) s.batteryPercent = intVal; break;
                case "车速":
                    if (s.speed < 0) s.speed = intVal; break;
                case "里程":
                    if (s.totalMileage < 0) s.totalMileage = val; break;
                case "油量百分比":
                    if (s.fuelPercent < 0) s.fuelPercent = intVal; break;
                case "续航里程":
                    if (s.totalRange < 0) s.totalRange = intVal; break;
                case "纯电续航里程":
                    if (s.evMileage < 0) s.evMileage = intVal; break;
                case "瞬时电耗":
                    if (s.currentElecConsumption < 0) s.currentElecConsumption = val; break;
                case "瞬时油耗":
                    if (s.currentFuelConsumption < 0) s.currentFuelConsumption = val; break;
                case "平均电耗":
                    if (s.avgElecConsumption < 0) s.avgElecConsumption = val; break;
                case "平均油耗":
                    if (s.avgFuelConsumption < 0) s.avgFuelConsumption = val; break;
                case "车外温度":
                    if (s.outsideTemp < 0) s.outsideTemp = intVal; break;
                case "左前胎压":
                    if (s.tirePressureFL < 0) s.tirePressureFL = (int)(raw * 100); break; // bar→kPa
                case "右前胎压":
                    if (s.tirePressureFR < 0) s.tirePressureFR = (int)(raw * 100); break;
                case "左后胎压":
                    if (s.tirePressureRL < 0) s.tirePressureRL = (int)(raw * 100); break;
                case "右后胎压":
                    if (s.tirePressureRR < 0) s.tirePressureRR = (int)(raw * 100); break;
                case "HEV里程":
                    if (s.hevMileage < 0) s.hevMileage = val; break;
                case "EV里程":
                    if (s.evMileage < 0) s.evMileage = intVal; break;
                default:
                    count--; // 不映射的传感器不计入有效数量
                    break;
            }
        }

        return count;
    }
}
