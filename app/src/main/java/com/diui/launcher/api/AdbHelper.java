package com.diui.launcher.api;

import android.content.Context;
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
     * 依次尝试 127.0.0.1 + 本机所有非回环 IPv4 地址，返回第一个 5555 端口可达的地址。
     */
    public static String findAdbHost() {
        if (resolvedHost != null && probePort(resolvedHost, ADB_PORT)) {
            return resolvedHost;
        }
        resolvedHost = null;

        List<String> candidates = new ArrayList<>();
        candidates.add("127.0.0.1");
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        candidates.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "枚举网络接口失败", e);
        }

        for (String host : candidates) {
            if (probePort(host, ADB_PORT)) {
                resolvedHost = host;
                Log.i(TAG, "ADB 可达: " + host + ":" + ADB_PORT);
                return host;
            }
        }

        Log.w(TAG, "ADB 不可达，已尝试: " + candidates);
        return null;
    }

    /**
     * 同步检测——必须在后台线程调用。
     */
    public static boolean isAdbAvailable() {
        return findAdbHost() != null;
    }

    /**
     * 异步检测——可安全在主线程调用，结果回调到主线程。
     */
    public static void checkAvailableAsync(AvailableCallback callback) {
        executor.execute(() -> {
            String host = findAdbHost();
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
            String host = findAdbHost();
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
                String cmd = "CLASSPATH=" + apkPath
                        + " nohup app_process /system/bin"
                        + " com.diui.launcher.helper.HelperDaemon"
                        + " > /dev/null 2>&1 &";

                dadb.shell(cmd);
                Log.i(TAG, "HelperDaemon 已启动");
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
