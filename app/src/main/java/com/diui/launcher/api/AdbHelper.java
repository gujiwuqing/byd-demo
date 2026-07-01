package com.diui.launcher.api;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import dadb.AdbKeyPair;
import dadb.Dadb;
import dadb.AdbShellResponse;

public class AdbHelper {

    private static final String TAG = "AdbHelper";
    private static final int ADB_PORT = 5555;

    private static final AtomicReference<Dadb> sharedDadb = new AtomicReference<>(null);
    private static final AtomicBoolean authPending = new AtomicBoolean(false);
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static volatile String resolvedHost = null;

    public interface AuthCallback {
        void onAuthPending();
        void onAuthGranted();
        void onAuthFailed(String error);
    }

    public interface GrantCallback {
        void onResult(boolean success, List<String> granted, List<String> failed, List<String> signature);
    }

    public interface AvailableCallback {
        void onResult(boolean available);
    }

    // ---------- ADB 地址探测 ----------

    /**
     * 同步探测——必须在后台线程调用。
     * 依次尝试 127.0.0.1 → Wi-Fi IP → 所有非回环 IPv4，返回第一个 5555 端口可达的地址。
     */
    public static String findAdbHost(Context context) {
        if (resolvedHost != null && probePort(resolvedHost, ADB_PORT)) {
            return resolvedHost;
        }
        resolvedHost = null;

        List<String> candidates = new ArrayList<>();
        candidates.add("127.0.0.1");

        // WifiManager 获取 Wi-Fi IP（比 NetworkInterface 更可靠）
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                int ip = wm.getConnectionInfo().getIpAddress();
                if (ip != 0) {
                    String wifiIp = String.format("%d.%d.%d.%d",
                            ip & 0xff, (ip >> 8) & 0xff, (ip >> 16) & 0xff, (ip >> 24) & 0xff);
                    if (!candidates.contains(wifiIp)) {
                        candidates.add(wifiIp);
                        Log.i(TAG, "Wi-Fi IP: " + wifiIp);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "获取 Wi-Fi IP 失败", e);
        }

        // NetworkInterface 枚举所有网络接口 IP
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String hostAddr = addr.getHostAddress();
                        if (!candidates.contains(hostAddr)) {
                            candidates.add(hostAddr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "枚举网络接口失败", e);
        }

        Log.i(TAG, "ADB 探测候选地址: " + candidates);

        for (String host : candidates) {
            if (probePort(host, ADB_PORT)) {
                resolvedHost = host;
                Log.i(TAG, "ADB 可达: " + host + ":" + ADB_PORT);
                return host;
            }
            Log.d(TAG, "ADB 不可达: " + host + ":" + ADB_PORT);
        }

        Log.w(TAG, "ADB 所有候选地址均不可达");
        return null;
    }

    /**
     * 同步检测——必须在后台线程调用。
     */
    public static boolean isAdbAvailable(Context context) {
        return findAdbHost(context) != null;
    }

    /**
     * 异步检测——可安全在主线程调用，结果回调到主线程。
     */
    public static void checkAvailableAsync(Context context, AvailableCallback callback) {
        executor.execute(() -> {
            String host = findAdbHost(context);
            mainHandler.post(() -> callback.onResult(host != null));
        });
    }

    private static String getAdbHost() {
        return resolvedHost != null ? resolvedHost : "127.0.0.1";
    }

    private static boolean probePort(String host, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------- 诊断 ----------

    public interface DiagCallback {
        void onResult(String report);
    }

    /**
     * 异步执行完整 ADB 网络诊断，结果回调到主线程。
     * 报告包含所有候选 IP、端口探测结果、已缓存 host 等信息。
     */
    public static void runDiagnostics(Context context, DiagCallback callback) {
        executor.execute(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("===== ADB 网络诊断 =====\n\n");

            // 已缓存的 host
            sb.append("缓存 host: ").append(resolvedHost != null ? resolvedHost : "无").append("\n\n");

            // Wi-Fi IP
            String wifiIp = null;
            try {
                WifiManager wm = (WifiManager) context.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    int ip = wm.getConnectionInfo().getIpAddress();
                    if (ip != 0) {
                        wifiIp = String.format("%d.%d.%d.%d",
                                ip & 0xff, (ip >> 8) & 0xff, (ip >> 16) & 0xff, (ip >> 24) & 0xff);
                    }
                }
            } catch (Exception ignored) {}
            sb.append("Wi-Fi IP: ").append(wifiIp != null ? wifiIp : "未获取到").append("\n\n");

            // 所有网络接口
            sb.append("── 网络接口 ──\n");
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces != null && interfaces.hasMoreElements()) {
                    NetworkInterface intf = interfaces.nextElement();
                    Enumeration<InetAddress> addrs = intf.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (addr instanceof Inet4Address) {
                            sb.append("  ").append(intf.getName())
                              .append(" → ").append(addr.getHostAddress())
                              .append(addr.isLoopbackAddress() ? " (loopback)" : "")
                              .append("\n");
                        }
                    }
                }
            } catch (Exception e) {
                sb.append("  枚举失败: ").append(e.getMessage()).append("\n");
            }

            // 端口探测
            sb.append("\n── 端口探测 (TCP ").append(ADB_PORT).append(") ──\n");
            List<String> candidates = new ArrayList<>();
            candidates.add("127.0.0.1");
            if (wifiIp != null && !candidates.contains(wifiIp)) candidates.add(wifiIp);
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces != null && interfaces.hasMoreElements()) {
                    NetworkInterface intf = interfaces.nextElement();
                    Enumeration<InetAddress> addrs = intf.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            String h = addr.getHostAddress();
                            if (!candidates.contains(h)) candidates.add(h);
                        }
                    }
                }
            } catch (Exception ignored) {}

            for (String host : candidates) {
                long start = System.currentTimeMillis();
                boolean ok = probePort(host, ADB_PORT);
                long elapsed = System.currentTimeMillis() - start;
                sb.append("  ").append(host).append(":").append(ADB_PORT)
                  .append(" → ").append(ok ? "✓ 可达" : "✗ 不可达")
                  .append(" (").append(elapsed).append("ms)")
                  .append("\n");
            }

            // Dadb 连接状态
            Dadb dadb = sharedDadb.get();
            sb.append("\n── Dadb 连接 ──\n");
            sb.append("  当前连接: ").append(dadb != null ? "有" : "无").append("\n");
            if (dadb != null) {
                try {
                    AdbShellResponse resp = dadb.shell("echo ok");
                    sb.append("  shell echo: ").append(resp.getExitCode() == 0 ? "✓ 正常" : "✗ 失败").append("\n");
                } catch (Exception e) {
                    sb.append("  shell echo: ✗ 异常 ").append(e.getMessage()).append("\n");
                }
            }

            sb.append("\n===== 诊断完成 =====");

            String report = sb.toString();
            Log.i(TAG, report);
            mainHandler.post(() -> callback.onResult(report));
        });
    }

    // ---------- 密钥管理 ----------

    private static AdbKeyPair getOrCreateKeyPair(Context context) {
        File keyDir = context.getFilesDir();
        File privateKey = new File(keyDir, "adbkey");
        File publicKey = new File(keyDir, "adbkey.pub");

        if (privateKey.exists() && publicKey.exists()) {
            try {
                return AdbKeyPair.read(privateKey, publicKey);
            } catch (Exception e) {
                Log.w(TAG, "读取密钥失败，重新生成", e);
            }
        }

        Log.i(TAG, "生成新的 ADB 密钥对");
        AdbKeyPair.generate(privateKey, publicKey);
        return AdbKeyPair.read(privateKey, publicKey);
    }

    public static void clearKeys(Context context) {
        File keyDir = context.getFilesDir();
        new File(keyDir, "adbkey").delete();
        new File(keyDir, "adbkey.pub").delete();
        Dadb dadb = sharedDadb.getAndSet(null);
        if (dadb != null) {
            try { dadb.close(); } catch (Exception ignored) {}
        }
        resolvedHost = null;
        Log.i(TAG, "ADB 密钥已清除，下次连接将弹出授权框");
    }

    // ---------- 连接与认证 ----------

    private static Dadb tryConnect(Context context, long timeoutMs) {
        AdbKeyPair keyPair = getOrCreateKeyPair(context);
        String host = getAdbHost();
        final Dadb[] result = {null};

        Thread connectThread = new Thread(() -> {
            try {
                Dadb dadb = Dadb.create(host, ADB_PORT, keyPair);
                AdbShellResponse test = dadb.shell("echo ok");
                if (test.getExitCode() == 0) {
                    result[0] = dadb;
                } else {
                    dadb.close();
                }
            } catch (Exception e) {
                Log.d(TAG, "连接尝试 " + host + ": " + e.getMessage());
            }
        }, "adb-connect-probe");
        connectThread.setDaemon(true);
        connectThread.start();

        try {
            connectThread.join(timeoutMs);
        } catch (InterruptedException ignored) {}

        return result[0];
    }

    public static void connectAndAuth(Context context, AuthCallback callback) {
        executor.execute(() -> {
            String host = findAdbHost(context);
            if (host == null) {
                Log.w(TAG, "ADB 端口未开放");
                callback.onAuthFailed("ADB 未开启，请在设置中开启无线 ADB 调试");
                return;
            }

            Dadb dadb = tryConnect(context, 2000);
            if (dadb != null) {
                sharedDadb.set(dadb);
                Log.i(TAG, "ADB 已授权，直接连接成功 (" + host + ")");
                callback.onAuthGranted();
                return;
            }

            authPending.set(true);
            callback.onAuthPending();
            Log.i(TAG, "等待用户授权 ADB（车机屏幕应弹出授权框）... host=" + host);

            int maxAttempts = 40;
            for (int i = 0; i < maxAttempts && authPending.get(); i++) {
                try { Thread.sleep(3000); } catch (InterruptedException e) { break; }

                dadb = tryConnect(context, 2000);
                if (dadb != null) {
                    sharedDadb.set(dadb);
                    authPending.set(false);
                    Log.i(TAG, "ADB 授权成功！（第 " + (i + 1) + " 次尝试, host=" + host + ")");
                    callback.onAuthGranted();
                    return;
                }
                Log.d(TAG, "等待授权... (" + (i + 1) + "/" + maxAttempts + ")");
            }

            authPending.set(false);
            Log.w(TAG, "ADB 授权超时");
            callback.onAuthFailed("授权超时，请在弹窗中点击\"允许\"后重试");
        });
    }

    // ---------- 权限授予 ----------

    public static void grantPermissions(Context context, GrantCallback callback) {
        executor.execute(() -> {
            List<String> granted = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            List<String> signature = new ArrayList<>();

            Dadb dadb = sharedDadb.get();
            if (dadb == null) {
                dadb = tryConnect(context, 3000);
                if (dadb != null) sharedDadb.set(dadb);
            }

            if (dadb == null) {
                Log.e(TAG, "ADB 未连接，无法授权");
                callback.onResult(false, granted, failed, signature);
                return;
            }

            String pkg = context.getPackageName();
            StringBuilder script = new StringBuilder();
            for (String perm : BydPermissionHelper.getAllPermissions()) {
                if (script.length() > 0) script.append("; ");
                script.append("pm grant ").append(pkg).append(" ").append(perm).append(" 2>&1");
            }

            try {
                AdbShellResponse result = dadb.shell(script.toString());
                String output = result.getAllOutput();
                Log.i(TAG, "授权输出: " + output);

                for (String perm : BydPermissionHelper.getAllPermissions()) {
                    String shortName = perm.substring("android.permission.BYDAUTO_".length());
                    if (output.contains("not a changeable permission type") && output.contains(perm)) {
                        signature.add(shortName);
                    } else {
                        granted.add(shortName);
                    }
                }

                callback.onResult(true, granted, failed, signature);
            } catch (Exception e) {
                Log.e(TAG, "执行 pm grant 失败", e);
                callback.onResult(false, granted, failed, signature);
            }
        });
    }

    // ---------- HelperDaemon 管理 ----------

    public interface DaemonDiagCallback {
        void onResult(String report);
    }

    /**
     * 启动 HelperDaemon 并收集每一步的诊断信息，结果回调到主线程。
     */
    public static void startHelperDaemonWithDiag(Context context, DaemonDiagCallback callback) {
        executor.execute(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("===== HelperDaemon 启动诊断 =====\n\n");

            // 1. Dadb 连接
            Dadb dadb = sharedDadb.get();
            sb.append("1. sharedDadb: ").append(dadb != null ? "有缓存连接" : "无").append("\n");

            if (dadb == null) {
                sb.append("   尝试重新连接 (host=").append(getAdbHost()).append(")...\n");
                dadb = tryConnect(context, 3000);
                if (dadb != null) {
                    sharedDadb.set(dadb);
                    sb.append("   ✓ 重新连接成功\n");
                } else {
                    sb.append("   ✗ 重新连接失败\n");
                    sb.append("\n===== 诊断完成（ADB 未连接）=====");
                    String report = sb.toString();
                    Log.i(TAG, report);
                    mainHandler.post(() -> callback.onResult(report));
                    return;
                }
            }

            // 2. shell 验证 + SELinux
            sb.append("\n2. shell 环境:\n");
            try {
                AdbShellResponse idResp = dadb.shell("echo ok && id && getenforce && cat /proc/self/attr/current 2>/dev/null");
                sb.append("   ").append(idResp.getAllOutput().trim().replace("\n", "\n   ")).append("\n");
            } catch (Exception e) {
                sb.append("   ✗ 异常: ").append(e.getMessage()).append("\n");
            }

            // 3. APK 路径 + appUid
            String apkPath = context.getApplicationInfo().sourceDir;
            int appUid = context.getApplicationInfo().uid;
            sb.append("\n3. APK: ").append(apkPath).append("\n");
            sb.append("   appUid: ").append(appUid).append("\n");

            // 4. 检查已有 daemon 进程
            sb.append("\n4. 已有 daemon 进程:\n");
            try {
                AdbShellResponse psResp = dadb.shell("ps -ef 2>/dev/null | grep -E 'HelperDaemon|diui_helper' | grep -v grep || echo '无'");
                sb.append("   ").append(psResp.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("   查询失败: ").append(e.getMessage()).append("\n");
            }

            // 5. 测试 app_process 是否可用
            sb.append("\n5. app_process 基础测试:\n");
            try {
                AdbShellResponse apResp = dadb.shell("app_process /system/bin --help 2>&1 | head -3 || echo 'app_process 不可用'");
                sb.append("   ").append(apResp.getAllOutput().trim().replace("\n", "\n   ")).append("\n");
            } catch (Exception e) {
                sb.append("   ✗ 异常: ").append(e.getMessage()).append("\n");
            }

            // 6. 启动 daemon（前台，捕获完整输出）
            sb.append("\n6. 启动 daemon（前台，3秒）:\n");
            String fgCmd = "CLASSPATH=" + apkPath
                    + " app_process /system/bin"
                    + " com.diui.launcher.helper.HelperDaemon"
                    + " " + appUid
                    + " 2>&1 &"
                    + " DPID=$!; sleep 3; kill $DPID 2>/dev/null; wait $DPID 2>/dev/null";
            sb.append("   cmd: CLASSPATH=...apk app_process /system/bin ...HelperDaemon ").append(appUid).append("\n");
            try {
                AdbShellResponse startResp = dadb.shell(fgCmd);
                String output = startResp.getAllOutput();
                sb.append("   exit=").append(startResp.getExitCode()).append("\n");
                if (output.length() > 0) {
                    sb.append("   output:\n");
                    for (String line : output.split("\n")) {
                        sb.append("     ").append(line).append("\n");
                    }
                } else {
                    sb.append("   output: (空)\n");
                }
            } catch (Exception e) {
                sb.append("   ✗ 异常: ").append(e.getClass().getSimpleName())
                  .append(": ").append(e.getMessage()).append("\n");
            }

            // 7. SELinux audit 日志
            sb.append("\n7. SELinux/dmesg (最近 denied):\n");
            try {
                AdbShellResponse dmesgResp = dadb.shell("dmesg 2>/dev/null | grep -iE 'avc.*denied|diui|helper|app_process' | tail -10 || echo '无权限或无记录'");
                String dmesgOut = dmesgResp.getAllOutput().trim();
                if (dmesgOut.length() > 0) {
                    for (String line : dmesgOut.split("\n")) {
                        sb.append("   ").append(line).append("\n");
                    }
                } else {
                    sb.append("   (空)\n");
                }
            } catch (Exception e) {
                sb.append("   查询失败: ").append(e.getMessage()).append("\n");
            }

            // 8. 后台启动（正式方式）
            sb.append("\n8. 后台启动 daemon:\n");
            String bgCmd = "CLASSPATH=" + apkPath
                    + " nohup app_process /system/bin"
                    + " com.diui.launcher.helper.HelperDaemon"
                    + " " + appUid
                    + " > /dev/null 2>&1 &";
            try {
                AdbShellResponse bgResp = dadb.shell(bgCmd);
                sb.append("   exit=").append(bgResp.getExitCode()).append("\n");
                Thread.sleep(2000);
            } catch (Exception e) {
                sb.append("   ✗ 异常: ").append(e.getMessage()).append("\n");
            }

            // 9. 检查 binder 服务
            sb.append("\n9. diui_helper binder:\n");
            try {
                AdbShellResponse svcResp = dadb.shell("service list 2>/dev/null | grep diui_helper || echo '未注册'");
                sb.append("   ").append(svcResp.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("   查询失败: ").append(e.getMessage()).append("\n");
            }

            // 10. 再次检查进程
            sb.append("\n10. daemon 进程（启动后）:\n");
            try {
                AdbShellResponse psResp2 = dadb.shell("ps -ef 2>/dev/null | grep -E 'HelperDaemon|diui_helper' | grep -v grep || echo '无'");
                sb.append("   ").append(psResp2.getAllOutput().trim()).append("\n");
            } catch (Exception e) {
                sb.append("   查询失败: ").append(e.getMessage()).append("\n");
            }

            sb.append("\n===== 诊断完成 =====");
            String report = sb.toString();
            Log.i(TAG, report);
            mainHandler.post(() -> callback.onResult(report));
        });
    }

    public static void startHelperDaemon(Context context, Runnable onStarted) {
        executor.execute(() -> {
            Dadb dadb = sharedDadb.get();
            if (dadb == null) {
                dadb = tryConnect(context, 3000);
                if (dadb != null) sharedDadb.set(dadb);
            }

            if (dadb == null) {
                Log.e(TAG, "无法启动 HelperDaemon: ADB 未连接");
                return;
            }

            try {
                String apkPath = context.getApplicationInfo().sourceDir;
                int appUid = context.getApplicationInfo().uid;
                String cmd = "CLASSPATH=" + apkPath
                        + " nohup app_process /system/bin"
                        + " com.diui.launcher.helper.HelperDaemon"
                        + " " + appUid
                        + " > /dev/null 2>&1 &";

                dadb.shell(cmd);
                Log.i(TAG, "HelperDaemon 已启动 (uid=" + appUid + ")");
                Thread.sleep(1000);
                if (onStarted != null) onStarted.run();
            } catch (Exception e) {
                Log.e(TAG, "启动 HelperDaemon 失败", e);
            }
        });
    }

    public static void killHelper(Context context, Runnable onKilled) {
        executor.execute(() -> {
            Dadb dadb = sharedDadb.get();
            if (dadb == null) {
                dadb = tryConnect(context, 3000);
                if (dadb != null) sharedDadb.set(dadb);
            }

            if (dadb == null) {
                Log.e(TAG, "无法终止 HelperDaemon: ADB 未连接");
                if (onKilled != null) onKilled.run();
                return;
            }

            try {
                dadb.shell("pkill -f HelperDaemon 2>/dev/null; sleep 0.5");
                Log.i(TAG, "HelperDaemon 已终止");
            } catch (Exception e) {
                Log.w(TAG, "终止 HelperDaemon 失败: " + e.getMessage());
            }

            if (onKilled != null) onKilled.run();
        });
    }
}
