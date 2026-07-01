package com.diui.launcher.api;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地 ADB 客户端，连接 127.0.0.1:5555 执行 shell 命令。
 * 用于在应用内自动授权 BYD 车辆数据权限（pm grant）及启动 HelperDaemon。
 *
 * 流程：连接本机 ADB → 系统弹出授权弹窗 → 用户点"允许" → 执行 shell 命令
 *
 * 协议参考 Android system/core/adb：24 字节小端包头，payload 校验和为
 * 无符号字节求和，magic = ~command。AUTH 用 NONEwithRSA 签名
 * (15字节 ASN1 DigestInfo ‖ token)，公钥序列化为 524 字节小端 blob。
 */
public class AdbHelper {

    private static final String TAG = "AdbHelper";
    private static final String ADB_HOST = "127.0.0.1";
    private static final int ADB_PORT = 5555;
    private static final int TIMEOUT = 5000;

    private static final String HELPER_PROCESS_NAME = "diui_helper";

    private static final int CMD_CNXN = 0x4e584e43;
    private static final int CMD_AUTH = 0x48545541;
    private static final int CMD_OPEN = 0x4e45504f;
    private static final int CMD_OKAY = 0x59414b4f;
    private static final int CMD_CLSE = 0x45534c43;
    private static final int CMD_WRTE = 0x45545257;

    private static final int AUTH_TOKEN = 1;
    private static final int AUTH_SIGNATURE = 2;
    private static final int AUTH_RSAPUBLICKEY = 3;

    /** ADB 协议版本（system/core/adb A_VERSION = 0x01000001）。 */
    private static final int VERSION = 0x01000001;
    private static final int MAX_PAYLOAD = 262144;

    private static final int USER_PROMPT_TIMEOUT_MS = 120000;

    public interface GrantCallback {
        void onResult(boolean success, List<String> granted, List<String> failed, List<String> signature);
    }

    /** 流 ID 递增器，避免多次 execShell 的 stream id 冲突。 */
    private static final AtomicInteger streamIdSeq = new AtomicInteger(1);

    public static void grantPermissions(Context context, GrantCallback callback) {
        new Thread(() -> {
            List<String> granted = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            List<String> signature = new ArrayList<>();
            Socket socket = null;

            try {
                socket = connect();
                if (socket == null) {
                    Log.e(TAG, "无法连接本地 ADB（ADB 可能未开启）");
                    callback.onResult(false, granted, failed, signature);
                    return;
                }

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                if (!authenticate(socket, in, out, context)) {
                    Log.e(TAG, "ADB 认证失败");
                    callback.onResult(false, granted, failed, signature);
                    return;
                }

                Log.i(TAG, "ADB 认证成功，开始授权...");

                String pkg = context.getPackageName();
                StringBuilder script = new StringBuilder();
                for (String perm : BydPermissionHelper.getAllPermissions()) {
                    if (script.length() > 0) script.append("; ");
                    script.append("pm grant ").append(pkg).append(" ").append(perm).append(" 2>&1");
                }

                String combinedResult = execShell(in, out, script.toString());
                Log.i(TAG, "授权脚本输出: " + combinedResult);

                for (String perm : BydPermissionHelper.getAllPermissions()) {
                    String shortName = perm.substring("android.permission.BYDAUTO_".length());
                    if (combinedResult != null && combinedResult.contains("not a changeable permission type")
                            && combinedResult.contains(perm)) {
                        signature.add(shortName);
                    } else {
                        boolean hasError = combinedResult != null &&
                                (combinedResult.contains("SecurityException") ||
                                 combinedResult.contains("java.lang.Exception"));
                        if (!hasError) granted.add(shortName);
                        else failed.add(shortName);
                    }
                }

                Log.i(TAG, "授权完成: 成功 " + granted.size() + ", signature " + signature.size() + ", 失败 " + failed.size());
                callback.onResult(failed.isEmpty(), granted, failed, signature);

            } catch (Exception e) {
                Log.e(TAG, "ADB 授权异常", e);
                callback.onResult(false, granted, failed, signature);
            } finally {
                if (socket != null) {
                    try { socket.close(); } catch (IOException ignored) {}
                }
            }
        }, "adb-grant").start();
    }

    public static boolean isAdbAvailable() {
        try (Socket s = new Socket()) {
            s.connect(new java.net.InetSocketAddress(ADB_HOST, ADB_PORT), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 启动 HelperDaemon（shell uid）。命令参考 BYDMate 的 spawn 配方：
     * - setsid 脱离 shell session（adbd 在 & 后台化后会 SIGHUP 子进程）
     * - --nice-name 便于按进程名 kill/检测
     * - 传入 appUid 供 daemon 做 uid 鉴权
     * - </dev/null 重定向 stdin
     * - trailing poll-loop 让 spawning shell 保持存活直到 daemon 注册
     *   （否则 adbd 关闭 exec stream 时 SIGHUP 会让 JVM 还没启动就死）
     */
    public static void startHelperDaemon(Context context, Runnable onStarted) {
        new Thread(() -> {
            Socket socket = null;
            try {
                socket = connect();
                if (socket == null) {
                    Log.e(TAG, "Cannot start helper: ADB unavailable");
                    return;
                }

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                if (!authenticate(socket, in, out, context)) {
                    Log.e(TAG, "Cannot start helper: ADB auth failed");
                    return;
                }

                String apkPath = context.getApplicationInfo().sourceDir;
                int appUid = Process.myUid();
                String cmd = "CLASSPATH=" + apkPath
                        + " setsid app_process /system/bin"
                        + " --nice-name=" + HELPER_PROCESS_NAME
                        + " com.diui.launcher.helper.HelperDaemon"
                        + " " + appUid
                        + " </dev/null >/dev/null 2>&1 &"
                        + " for i in 1 2 3; do"
                        + " service list 2>/dev/null | grep -q " + HELPER_PROCESS_NAME
                        + " && break; sleep 1; done";

                String result = execShell(in, out, cmd);
                Log.i(TAG, "HelperDaemon spawn result: " + result);

                Thread.sleep(1000);
                if (onStarted != null) onStarted.run();

            } catch (Exception e) {
                Log.e(TAG, "Failed to start HelperDaemon", e);
            } finally {
                if (socket != null) {
                    try { socket.close(); } catch (IOException ignored) {}
                }
            }
        }, "helper-start").start();
    }

    /** 按进程名 kill 旧 daemon（版本更新后让新 daemon 抢占 binder service + file lock）。 */
    public static void killHelper(Context context, Runnable onDone) {
        new Thread(() -> {
            Socket socket = null;
            try {
                socket = connect();
                if (socket == null) { if (onDone != null) onDone.run(); return; }
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                if (!authenticate(socket, in, out, context)) {
                    if (onDone != null) onDone.run();
                    return;
                }
                execShell(in, out, "for p in $(pgrep -f " + HELPER_PROCESS_NAME
                        + "); do kill -9 $p; done");
                Log.i(TAG, "killHelper dispatched");
            } catch (Exception e) {
                Log.w(TAG, "killHelper failed", e);
            } finally {
                if (socket != null) try { socket.close(); } catch (IOException ignored) {}
                if (onDone != null) onDone.run();
            }
        }, "helper-kill").start();
    }

    /** 检测名为 diui_helper 的进程是否在运行。 */
    public static boolean helperHeartbeat(Context context) {
        Socket socket = null;
        try {
            socket = connect();
            if (socket == null) return false;
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            if (!authenticate(socket, in, out, context)) return false;
            String out2 = execShell(in, out, "ps -A -o NAME");
            if (out2 == null) return false;
            for (String line : out2.split("\n")) {
                if (line.trim().equals(HELPER_PROCESS_NAME)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static Socket connect() throws IOException {
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(ADB_HOST, ADB_PORT), TIMEOUT);
        socket.setSoTimeout(TIMEOUT);
        return socket;
    }

    private static byte[] nullTerminate(String s) {
        byte[] strBytes = s.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[strBytes.length + 1];
        System.arraycopy(strBytes, 0, result, 0, strBytes.length);
        result[strBytes.length] = 0;
        return result;
    }

    private static byte[] buildCnxnBanner() {
        return nullTerminate("host::");
    }

    /**
     * ADB 认证流程：
     * 1. 发 CNXN，若 adbd 直接回 CNXN（无鉴权）则成功。
     * 2. 若回 AUTH(TOKEN)，用私钥签名 (DigestInfo‖token) 发 AUTH(SIGNATURE)。
     * 3. 签名通过（密钥已被设备记住）→ CNXN 成功。
     * 4. 签名不通过（首次）→ adbd 再发 AUTH(TOKEN)，发送 524 字节公钥 AUTH(RSAPUBLICKEY)，
     *    系统弹出"允许 USB 调试？"对话框，等待用户点允许后回 CNXN。
     */
    private static boolean authenticate(Socket socket, InputStream in, OutputStream out, Context context) throws Exception {
        byte[] banner = buildCnxnBanner();
        Log.d(TAG, "发送 CNXN banner (" + banner.length + " 字节)");
        sendMessage(out, CMD_CNXN, VERSION, MAX_PAYLOAD, banner);

        int[] msg = readMessage(in);
        if (msg == null) {
            Log.e(TAG, "读取消息失败，未收到任何响应");
            return false;
        }

        if (msg[0] == CMD_CNXN) {
            readData(in, msg[3]);
            Log.i(TAG, "已授权，直接连接成功");
            return true;
        }

        if (msg[0] != CMD_AUTH || msg[1] != AUTH_TOKEN) {
            Log.e(TAG, "收到意外消息: cmd=0x" + Integer.toHexString(msg[0]) + " type=" + msg[1]);
            return false;
        }

        byte[] token = readData(in, msg[3]);
        if (token == null || token.length != 20) {
            Log.e(TAG, "AUTH_TOKEN 长度错误: " + (token == null ? "null" : token.length));
            return false;
        }

        AdbKeyManager keyManager = new AdbKeyManager(context);

        byte[] sig = keyManager.signToken(token);
        sendMessage(out, CMD_AUTH, AUTH_SIGNATURE, 0, sig);
        Log.d(TAG, "已发送 AUTH_SIGNATURE (" + sig.length + " 字节)");

        msg = readMessage(in);
        if (msg == null) {
            Log.e(TAG, "签名后未收到响应");
            return false;
        }
        if (msg[0] == CMD_CNXN) {
            readData(in, msg[3]);
            Log.i(TAG, "签名认证成功");
            return true;
        }

        if (msg[0] != CMD_AUTH || msg[1] != AUTH_TOKEN) {
            Log.e(TAG, "签名后收到意外消息: cmd=0x" + Integer.toHexString(msg[0]) + " type=" + msg[1]);
            return false;
        }

        Log.i(TAG, "公钥未被信任，发送 AUTH_RSAPUBLICKEY...");
        readData(in, msg[3]);

        String pubKey = keyManager.getAdbPublicKeyString();
        sendMessage(out, CMD_AUTH, AUTH_RSAPUBLICKEY, 0, nullTerminate(pubKey));
        Log.i(TAG, "已发送公钥，等待用户点击\"允许\"（最多 " + (USER_PROMPT_TIMEOUT_MS / 1000) + " 秒）");

        socket.setSoTimeout(USER_PROMPT_TIMEOUT_MS);
        msg = readMessage(in);
        socket.setSoTimeout(TIMEOUT);

        if (msg != null && msg[0] == CMD_CNXN) {
            readData(in, msg[3]);
            Log.i(TAG, "公钥认证成功");
            return true;
        }

        Log.e(TAG, "公钥认证未通过: " + (msg == null ? "无响应" : "cmd=0x" + Integer.toHexString(msg[0])));
        return false;
    }

    /**
     * 执行 shell 命令并返回 stdout。递增 localId 防止 stream id 冲突；
     * 严格校验 OKAY.arg1==localId 并处理 stale packet。
     */
    private static String execShell(InputStream in, OutputStream out, String command) throws IOException {
        int localId = streamIdSeq.getAndIncrement();

        sendMessage(out, CMD_OPEN, localId, 0, nullTerminate("shell:" + command));

        int remoteId = 0;
        boolean gotOkay = false;
        for (int i = 0; i < 20; i++) {
            int[] msg = readMessage(in);
            if (msg == null) {
                Log.e(TAG, "execShell: 读取消息失败");
                return null;
            }
            if (msg[0] == CMD_OKAY && msg[2] == localId) {
                remoteId = msg[1];
                gotOkay = true;
                break;
            }
            handleStalePacket(in, out, msg, localId);
        }
        if (!gotOkay) {
            Log.e(TAG, "execShell: 未收到匹配 localId=" + localId + " 的 OKAY");
            return null;
        }

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            int[] msg = readMessage(in);
            if (msg == null) break;

            if (msg[0] == CMD_WRTE && msg[1] == remoteId && msg[2] == localId) {
                byte[] data = readData(in, msg[3]);
                output.append(new String(data, StandardCharsets.UTF_8));
                sendMessage(out, CMD_OKAY, localId, remoteId, new byte[0]);
            } else if (msg[0] == CMD_CLSE && msg[1] == remoteId) {
                sendMessage(out, CMD_CLSE, localId, remoteId, new byte[0]);
                break;
            } else {
                handleStalePacket(in, out, msg, localId);
            }
        }
        return output.toString();
    }

    /** 对非当前 stream 的包回 CLSE/OKAY，保持 adbd 状态机正常。 */
    private static void handleStalePacket(InputStream in, OutputStream out, int[] msg, int currentLocalId) throws IOException {
        if (msg[0] == CMD_CLSE) {
            sendMessage(out, CMD_CLSE, msg[2], msg[1], new byte[0]);
        } else if (msg[0] == CMD_WRTE) {
            readData(in, msg[3]);
            sendMessage(out, CMD_OKAY, msg[2], msg[1], new byte[0]);
        } else if (msg[0] == CMD_OKAY) {
            // 无需处理
        }
    }

    private static void sendMessage(OutputStream out, int cmd, int arg0, int arg1, byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(24);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(cmd);
        buf.putInt(arg0);
        buf.putInt(arg1);
        buf.putInt(data.length);
        int checksum = 0;
        for (byte b : data) checksum += (b & 0xFF);
        buf.putInt(checksum);
        buf.putInt(~cmd);
        out.write(buf.array());
        if (data.length > 0) {
            out.write(data);
        }
        out.flush();
    }

    private static int[] readMessage(InputStream in) throws IOException {
        byte[] header = readExact(in, 24);
        if (header == null) return null;

        ByteBuffer buf = ByteBuffer.wrap(header);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        int cmd = buf.getInt();
        int arg0 = buf.getInt();
        int arg1 = buf.getInt();
        int length = buf.getInt();
        int checksum = buf.getInt();
        int magic = buf.getInt();

        if (magic != ~cmd) {
            Log.w(TAG, "消息 magic 校验失败: cmd=0x" + Integer.toHexString(cmd)
                    + " magic=0x" + Integer.toHexString(magic));
        }
        if (length < 0 || length > MAX_PAYLOAD) {
            Log.e(TAG, "消息长度异常: " + length);
            return null;
        }
        return new int[]{cmd, arg0, arg1, length};
    }

    private static byte[] readData(InputStream in, int length) throws IOException {
        if (length == 0) return new byte[0];
        byte[] data = readExact(in, length);
        return data != null ? data : new byte[0];
    }

    private static byte[] readExact(InputStream in, int length) throws IOException {
        if (length == 0) return new byte[0];
        byte[] buf = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(buf, offset, length - offset);
            if (read == -1) return null;
            offset += read;
        }
        return buf;
    }
}
