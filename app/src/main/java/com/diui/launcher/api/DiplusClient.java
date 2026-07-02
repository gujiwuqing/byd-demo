package com.diui.launcher.api;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Diplus（迪加）本地 HTTP API 客户端。
 *
 * Diplus 监听 0.0.0.0:8988，因此：
 *   - 车机内部（本 app）：直接 http://localhost:8988/...
 *   - 外部网络（手机/PC 连同一 WiFi）：http://<车机IP>:8988/...
 *
 * 不需要 ADB shell，直接用 HttpURLConnection 即可。
 *
 * API：
 *   getVal:    /api/getVal?name=<URL编码中文>&status=true  → {"val": 数值} 或 {"success":false}
 *   getDiPars: /api/getDiPars?text=<URL编码的{名}|{名}...> → JSON {名: 值, ...}
 */
public class DiplusClient {

    private static final String TAG = "DiplusClient";
    private static final String BASE_URL = "http://localhost:8988";
    private static final int TIMEOUT_MS = 3000;

    // {中文传感器名, 说明, 缩放因子}
    private static final String[][] KNOWN_SENSORS = {
            {"电量百分比",    "电池 SOC",         "1"},
            {"车速",          "当前车速",          "1"},
            {"里程",          "总里程",            "0.1"},
            {"油量百分比",    "燃油百分比",         "1"},
            {"续航里程",      "总续航",            "1"},
            {"纯电续航里程",  "纯电续航",           "1"},
            {"瞬时电耗",      "当前电耗",           "1"},
            {"瞬时油耗",      "当前油耗",           "1"},
            {"平均电耗",      "平均电耗",           "1"},
            {"平均油耗",      "平均油耗",           "1"},
            {"车外温度",      "环境温度",           "1"},
            {"车内温度",      "车内温度",           "1"},
            {"电池温度",      "电池温度",           "1"},
            {"左前胎压",      "左前胎压",           "0.01"},
            {"右前胎压",      "右前胎压",           "0.01"},
            {"左后胎压",      "左后胎压",           "0.01"},
            {"右后胎压",      "右后胎压",           "0.01"},
            {"发动机转速",    "发动机 RPM",         "1"},
            {"电机转速",      "电机 RPM",           "1"},
            {"方向角",        "方向盘角度",          "1"},
            {"电芯最高电压",  "电芯最高压",          "1"},
            {"电芯最低电压",  "电芯最低压",          "1"},
            {"充电状态",      "充电状态",            "1"},
            {"档位",          "当前档位",            "1"},
            {"HEV里程",       "混动里程",            "0.1"},
            {"EV里程",        "纯电里程",            "0.1"},
            {"电池健康度",    "SOH",                "1"},
            {"12V电压",       "蓄电池电压",          "1"},
            {"行驶时间",      "行驶时长（秒）",       "1"},
            {"钥匙电量",      "遥控钥匙电量",         "1"},
            {"水温",          "发动机水温",           "1"},
            {"制动深度",      "刹车踏板深度",         "1"},
            {"油门深度",      "油门踏板深度",         "1"},
    };

    // ── 工具方法 ──────────────────────────────────────────────────────

    private static String encode(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    /**
     * 发起 GET 请求，返回响应体字符串。失败返回 null。
     */
    private static String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoInput(true);

            int code = conn.getResponseCode();
            if (code != 200) {
                Log.w(TAG, "HTTP " + code + " for " + urlStr);
                return null;
            }
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString().trim();
        } catch (Exception e) {
            Log.w(TAG, "httpGet failed [" + urlStr + "]: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 从 getVal 响应中提取数值字符串。失败返回 null。
     */
    private static String parseVal(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has("val")) return obj.get("val").toString();
        } catch (Exception ignored) {}
        return null;
    }

    // ── 连接测试（设置页按钮用）──────────────────────────────────────

    public static String testConnection() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Diplus API 连接测试 =====\n");
        sb.append("地址: ").append(BASE_URL).append("\n\n");

        // 1. 探测端口（直接 HTTP，不走 ADB）
        sb.append("── 步骤 1: 端口探测 ──\n");
        String pingOut = httpGet(BASE_URL + "/api/getVal?name=test");
        if (pingOut == null) {
            sb.append("  ✗ 端口 8988 无响应\n");
            sb.append("  请确认迪加(Diplus)应用已启动\n\n");
        } else {
            sb.append("  ✓ 端口 8988 已响应: ").append(pingOut).append("\n\n");
        }

        // 2. 测试 getVal（几个典型传感器）
        sb.append("── 步骤 2: getVal 接口测试 ──\n");
        String[][] quickTests = {
                {"电量百分比", "电池 SOC"},
                {"车速",       "车速"},
                {"车外温度",   "环境温度"},
                {"水温",       "发动机水温"},
                {"油量百分比", "燃油百分比"},
                {"续航里程",   "总续航"},
        };
        for (String[] t : quickTests) {
            String out = httpGet(BASE_URL + "/api/getVal?name=" + encode(t[0]) + "&status=true");
            String val = parseVal(out);
            if (val != null) {
                sb.append("  ✓ ").append(t[0]).append(" = ").append(val).append("\n");
            } else {
                sb.append("  ✗ ").append(t[0])
                  .append(out != null ? " → " + out : " → 无响应").append("\n");
            }
        }

        // 3. 测试 getDiPars 批量接口
        sb.append("\n── 步骤 3: getDiPars 批量接口 ──\n");
        String textParam = encode("{电量百分比}|{车速}|{车外温度}|{油量百分比}");
        String batchOut = httpGet(BASE_URL + "/api/getDiPars?text=" + textParam);
        if (batchOut == null) {
            sb.append("  ✗ 无响应\n");
        } else {
            sb.append("  原始: ").append(batchOut).append("\n");
            try {
                JSONObject json = new JSONObject(batchOut);
                sb.append("  ✓ 解析成功，字段数: ").append(json.length()).append("\n");
            } catch (Exception e) {
                sb.append("  ⚠ JSON 解析失败: ").append(e.getMessage()).append("\n");
            }
        }

        sb.append("\n===== 测试完成 =====");
        return sb.toString();
    }

    // ── 传感器完整扫描（设置页按钮用）───────────────────────────────

    public static String fetchAllSensors() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Diplus 传感器完整扫描 =====\n\n");

        // 1. 先用 getDiPars 批量拉一次
        sb.append("── 批量接口 getDiPars ──\n");
        StringBuilder textBuilder = new StringBuilder();
        for (String[] s : KNOWN_SENSORS) {
            if (textBuilder.length() > 0) textBuilder.append("|");
            textBuilder.append("{").append(s[0]).append("}");
        }
        String batchOut = httpGet(BASE_URL + "/api/getDiPars?text=" + encode(textBuilder.toString()));
        JSONObject batch = null;
        if (batchOut == null) {
            sb.append("  ✗ 批量接口无响应\n\n");
        } else {
            sb.append("  原始 (").append(batchOut.length()).append(" 字符): ")
              .append(batchOut).append("\n");
            try {
                batch = new JSONObject(batchOut);
                sb.append("  ✓ 解析成功，字段数: ").append(batch.length()).append("\n\n");
            } catch (Exception e) {
                sb.append("  ⚠ JSON 解析失败，将逐个 getVal 验证\n\n");
            }
        }

        // 2. 逐个 getVal 验证
        sb.append("── 逐个 getVal 验证 ──\n\n");
        sb.append(String.format("%-2s %-14s %-16s %-6s %s\n", "#", "传感器名", "说明", "缩放", "当前值"));
        sb.append("──────────────────────────────────────────────────\n");

        int found = 0, notFound = 0;
        for (int i = 0; i < KNOWN_SENSORS.length; i++) {
            String name  = KNOWN_SENSORS[i][0];
            String desc  = KNOWN_SENSORS[i][1];
            String scale = KNOWN_SENSORS[i][2];

            // 优先批量结果
            String valStr = null;
            if (batch != null && batch.has(name)) {
                try { valStr = batch.get(name).toString(); } catch (Exception ignored) {}
            }
            // 批量没有则单独请求
            if (valStr == null) {
                String out = httpGet(BASE_URL + "/api/getVal?name=" + encode(name) + "&status=true");
                valStr = parseVal(out);
            }

            if (valStr != null) {
                String display = valStr;
                if (!"1".equals(scale)) {
                    try {
                        double scaled = Double.parseDouble(valStr) * Double.parseDouble(scale);
                        display = valStr + " ×" + scale + "=" + String.format("%.3f", scaled);
                    } catch (Exception ignored) {}
                }
                sb.append(String.format("✓ %2d. %-12s %-16s %s\n", i + 1, name, desc, display));
                found++;
            } else {
                sb.append(String.format("✗ %2d. %-12s %-16s 无效/不支持\n", i + 1, name, desc));
                notFound++;
            }
        }

        sb.append("──────────────────────────────────────────────────\n");
        sb.append("可用: ").append(found)
          .append("  不可用: ").append(notFound).append("\n");
        sb.append("\n===== 扫描完成 =====");
        return sb.toString();
    }

    // ── 填充 VehicleStatus（供 BydVehicleManager 调用）────────────────

    /**
     * 从 Diplus 读取车辆数据，填充到 VehicleStatus 中仍为 -1 的字段。
     * 在后台线程调用。
     *
     * @return 成功填充的字段数；-1 表示 Diplus 无响应
     */
    public static int fillVehicleStatus(com.diui.launcher.model.VehicleStatus s) {
        // 先测一下连通性
        String ping = httpGet(BASE_URL + "/api/getVal?name=test");
        if (ping == null) return -1;

        // 批量拉取
        StringBuilder textBuilder = new StringBuilder();
        for (String[] sensor : KNOWN_SENSORS) {
            if (textBuilder.length() > 0) textBuilder.append("|");
            textBuilder.append("{").append(sensor[0]).append("}");
        }
        String batchOut = httpGet(BASE_URL + "/api/getDiPars?text=" + encode(textBuilder.toString()));
        JSONObject batch = null;
        if (batchOut != null) {
            try { batch = new JSONObject(batchOut); } catch (Exception ignored) {}
        }

        int count = 0;
        for (String[] sensor : KNOWN_SENSORS) {
            String name  = sensor[0];
            String scale = sensor[2];

            String valStr = null;
            if (batch != null && batch.has(name)) {
                try { valStr = batch.get(name).toString(); } catch (Exception ignored) {}
            }
            if (valStr == null) {
                String out = httpGet(BASE_URL + "/api/getVal?name=" + encode(name) + "&status=true");
                valStr = parseVal(out);
            }
            if (valStr == null) continue;

            double raw;
            try { raw = Double.parseDouble(valStr); } catch (Exception e) { continue; }
            double val = raw * Double.parseDouble(scale);
            int iv = (int) Math.round(val);

            boolean filled = true;
            switch (name) {
                case "电量百分比":   if (s.batteryPercent < 0)          { s.batteryPercent = iv; } else filled = false; break;
                case "车速":         if (s.speed < 0)                   { s.speed = iv; }         else filled = false; break;
                case "里程":         if (s.totalMileage < 0)            { s.totalMileage = val; } else filled = false; break;
                case "油量百分比":   if (s.fuelPercent < 0)             { s.fuelPercent = iv; }   else filled = false; break;
                case "续航里程":     if (s.totalRange < 0)              { s.totalRange = iv; }    else filled = false; break;
                case "纯电续航里程": if (s.evMileage < 0)               { s.evMileage = iv; }     else filled = false; break;
                case "瞬时电耗":     if (s.currentElecConsumption < 0)  { s.currentElecConsumption = val; } else filled = false; break;
                case "瞬时油耗":     if (s.currentFuelConsumption < 0)  { s.currentFuelConsumption = val; } else filled = false; break;
                case "平均电耗":     if (s.avgElecConsumption < 0)      { s.avgElecConsumption = val; }     else filled = false; break;
                case "平均油耗":     if (s.avgFuelConsumption < 0)      { s.avgFuelConsumption = val; }     else filled = false; break;
                case "车外温度":     if (s.outsideTemp < 0)             { s.outsideTemp = iv; }   else filled = false; break;
                // 胎压单位：Diplus 返回 bar（×0.01 后），×100 转 kPa
                case "左前胎压":     if (s.tirePressureFL < 0)          { s.tirePressureFL = (int)(raw * 100); } else filled = false; break;
                case "右前胎压":     if (s.tirePressureFR < 0)          { s.tirePressureFR = (int)(raw * 100); } else filled = false; break;
                case "左后胎压":     if (s.tirePressureRL < 0)          { s.tirePressureRL = (int)(raw * 100); } else filled = false; break;
                case "右后胎压":     if (s.tirePressureRR < 0)          { s.tirePressureRR = (int)(raw * 100); } else filled = false; break;
                case "HEV里程":      if (s.hevMileage < 0)              { s.hevMileage = val; }   else filled = false; break;
                case "EV里程":       if (s.evMileage < 0)               { s.evMileage = iv; }     else filled = false; break;
                default: filled = false; break;
            }
            if (filled) count++;
        }
        return count;
    }
}
