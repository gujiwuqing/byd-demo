package com.diui.launcher.api;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diplus（迪加）本地 HTTP API 客户端。
 * Diplus 在车机上监听 localhost:8988，暴露车辆传感器数据。
 */
public class DiplusClient {

    private static final String TAG = "DiplusClient";
    private static final String BASE_URL = "http://127.0.0.1:8988";
    private static final int TIMEOUT_MS = 3000;

    /**
     * 测试 Diplus API 是否可达。
     * 通过 ADB shell 在车机内部调用 localhost:8988。
     *
     * @return 诊断报告字符串
     */
    public static String testConnection() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Diplus API 连接测试 =====\n\n");

        dadb.Dadb dadb = AdbHelper.getSharedDadb();
        if (dadb == null) {
            sb.append("✗ ADB 未连接，无法测试\n");
            sb.append("\n提示：请先确保车机 ADB 已连接\n");
            return sb.toString();
        }

        // 1. 检查 Diplus 进程是否运行
        sb.append("── 步骤 1: 检查 Diplus 进程 ──\n");
        try {
            dadb.AdbShellResponse psResp = dadb.shell("ps -A | grep -i diplus");
            String psOut = psResp.getAllOutput().trim();
            if (psOut.isEmpty()) {
                sb.append("  ⚠ 未检测到 Diplus 进程\n");
                sb.append("  提示：请确认车机已安装并启动迪加(Diplus)应用\n\n");
            } else {
                sb.append("  ✓ Diplus 进程运行中\n");
                sb.append("  ").append(psOut).append("\n\n");
            }
        } catch (Exception e) {
            sb.append("  ✗ 检查进程失败: ").append(e.getMessage()).append("\n\n");
        }

        // 2. 测试端口连通性
        sb.append("── 步骤 2: 测试端口 8988 ──\n");
        try {
            dadb.AdbShellResponse ncResp = dadb.shell(
                    "cat < /dev/null > /dev/tcp/127.0.0.1/8988 2>&1 && echo OPEN || echo CLOSED");
            String ncOut = ncResp.getAllOutput().trim();
            if (ncOut.contains("OPEN")) {
                sb.append("  ✓ 端口 8988 已开放\n\n");
            } else {
                sb.append("  ✗ 端口 8988 未开放\n");
                sb.append("  输出: ").append(ncOut).append("\n\n");
            }
        } catch (Exception e) {
            sb.append("  ⚠ 端口测试异常: ").append(e.getMessage()).append("\n\n");
        }

        // 3. 测试 getVal 接口（单传感器）
        sb.append("── 步骤 3: 测试 getVal 接口 ──\n");
        String[] testSensors = {"电量百分比", "车速", "里程", "车外温度"};
        for (String sensor : testSensors) {
            try {
                String cmd = "curl -s --connect-timeout 3 'http://localhost:8988/api/getVal?name="
                        + sensor + "&status=true'";
                dadb.AdbShellResponse resp = dadb.shell(cmd);
                String out = resp.getAllOutput().trim();
                if (out.isEmpty()) {
                    sb.append("  ").append(sensor).append(": ✗ 无响应\n");
                } else {
                    sb.append("  ").append(sensor).append(": ").append(out).append("\n");
                }
            } catch (Exception e) {
                sb.append("  ").append(sensor).append(": ✗ ").append(e.getMessage()).append("\n");
            }
        }

        sb.append("\n===== 测试完成 =====");
        return sb.toString();
    }

    /**
     * 获取 Diplus 所有可用传感器列表。
     * 调用 getDiPars 接口获取全量数据，解析并格式化输出。
     *
     * @return 传感器列表报告
     */
    public static String fetchAllSensors() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Diplus 传感器完整列表 =====\n\n");

        dadb.Dadb dadb = AdbHelper.getSharedDadb();
        if (dadb == null) {
            sb.append("✗ ADB 未连接\n");
            return sb.toString();
        }

        // 1. 调用 getDiPars 批量接口
        sb.append("── 调用 getDiPars 接口 ──\n\n");
        String rawJson = null;
        try {
            String cmd = "curl -s --connect-timeout 5 'http://localhost:8988/api/getDiPars'";
            dadb.AdbShellResponse resp = dadb.shell(cmd);
            rawJson = resp.getAllOutput().trim();
        } catch (Exception e) {
            sb.append("✗ 请求失败: ").append(e.getMessage()).append("\n");
            return sb.toString();
        }

        if (rawJson == null || rawJson.isEmpty()) {
            sb.append("✗ 接口返回为空，Diplus 可能未运行\n");
            return sb.toString();
        }

        // 2. 尝试解析 JSON
        sb.append("原始响应长度: ").append(rawJson.length()).append(" 字符\n\n");

        try {
            JSONObject json = new JSONObject(rawJson);
            int count = 0;
            sb.append(String.format("%-4s %-20s %s\n", "序号", "传感器名称", "当前值"));
            sb.append("─────────────────────────────────────────\n");

            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                count++;
                Object val = json.get(key);
                sb.append(String.format("%-4d %-20s %s\n", count, key, val.toString()));
            }
            sb.append("─────────────────────────────────────────\n");
            sb.append("共 ").append(count).append(" 个传感器\n");
        } catch (Exception e) {
            // JSON 解析失败，可能是其他格式，直接输出原始内容
            sb.append("⚠ JSON 解析失败，输出原始内容:\n\n");
            sb.append(rawJson).append("\n");
        }

        // 3. 额外尝试逐个已知传感器验证
        sb.append("\n── 逐个验证已知传感器 ──\n\n");
        String[][] knownSensors = {
                {"电量百分比", "电池 SOC", "1"},
                {"车速", "当前车速", "1"},
                {"里程", "总里程", "0.1"},
                {"油量百分比", "燃油百分比", "1"},
                {"续航里程", "总续航", "1"},
                {"纯电续航里程", "纯电续航", "1"},
                {"瞬时电耗", "当前电耗", "1"},
                {"瞬时油耗", "当前油耗", "1"},
                {"平均电耗", "平均电耗", "1"},
                {"平均油耗", "平均油耗", "1"},
                {"车外温度", "环境温度", "1"},
                {"车内温度", "车内温度", "1"},
                {"电池温度", "电池温度", "1"},
                {"左前胎压", "左前胎压", "0.01"},
                {"右前胎压", "右前胎压", "0.01"},
                {"左后胎压", "左后胎压", "0.01"},
                {"右后胎压", "右后胎压", "0.01"},
                {"发动机转速", "发动机RPM", "1"},
                {"电机转速", "电机RPM", "1"},
                {"方向角", "方向盘角度", "1"},
                {"电芯最高电压", "电芯最高压", "1"},
                {"电芯最低电压", "电芯最低压", "1"},
                {"充电状态", "充电状态", "1"},
                {"档位", "当前档位", "1"},
                {"HEV里程", "混动里程", "0.1"},
                {"EV里程", "纯电里程", "0.1"},
                {"电池健康度", "SOH", "1"},
                {"12V电压", "蓄电池电压", "1"},
                {"行驶时间", "行驶时长", "1"},
                {"钥匙电量", "遥控钥匙电量", "1"},
                {"水温", "发动机水温", "1"},
        };

        int found = 0;
        int notFound = 0;
        for (String[] sensor : knownSensors) {
            String name = sensor[0];
            String desc = sensor[1];
            String scale = sensor[2];
            try {
                String cmd = "curl -s --connect-timeout 2 'http://localhost:8988/api/getVal?name="
                        + name + "&status=true'";
                dadb.AdbShellResponse resp = dadb.shell(cmd);
                String out = resp.getAllOutput().trim();
                if (out.isEmpty() || out.contains("error") || out.contains("null")) {
                    sb.append("  ✗ ").append(name).append(" (").append(desc).append(")\n");
                    notFound++;
                } else {
                    sb.append("  ✓ ").append(name).append(" (").append(desc)
                            .append(") = ").append(out);
                    if (!"1".equals(scale)) {
                        sb.append("  [×").append(scale).append("]");
                    }
                    sb.append("\n");
                    found++;
                }
            } catch (Exception e) {
                sb.append("  ✗ ").append(name).append(": ").append(e.getMessage()).append("\n");
                notFound++;
            }
        }

        sb.append("\n可用: ").append(found).append("  不可用: ").append(notFound).append("\n");
        sb.append("\n===== 扫描完成 =====");
        return sb.toString();
    }
}
