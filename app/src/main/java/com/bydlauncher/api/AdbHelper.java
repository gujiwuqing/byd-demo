package com.bydlauncher.api;

import android.content.Context;
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

/**
 * 本地 ADB 客户端，连接 127.0.0.1:5555 执行 shell 命令。
 * 用于在应用内自动授权 BYD 车辆数据权限（pm grant）。
 *
 * 流程：连接本机 ADB → 系统弹出授权弹窗 → 用户点"允许" → 执行 pm grant
 */
public class AdbHelper {

    private static final String TAG = "AdbHelper";
    private static final String ADB_HOST = "127.0.0.1";
    private static final int ADB_PORT = 5555;
    private static final int TIMEOUT = 3000;

    private static final int CMD_CNXN = 0x4e584e43;
    private static final int CMD_AUTH = 0x48545541;
    private static final int CMD_OPEN = 0x4e45504f;
    private static final int CMD_OKAY = 0x59414b4f;
    private static final int CMD_CLSE = 0x45534c43;
    private static final int CMD_WRTE = 0x45545257;

    private static final int AUTH_TOKEN = 1;
    private static final int AUTH_SIGNATURE = 2;
    private static final int AUTH_RSAPUBLICKEY = 3;

    private static final int VERSION = 0x01000000;
    private static final int MAX_PAYLOAD = 4096;

    public interface GrantCallback {
        void onResult(boolean success, List<String> granted, List<String> failed, List<String> signature);
    }

    public static void grantPermissions(Context context, GrantCallback callback) {
        new Thread(() -> {
            List<String> granted = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            Socket socket = null;

            try {
                socket = connect();
                if (socket == null) {
                    Log.e(TAG, "无法连接本地 ADB（ADB 可能未开启）");
                    callback.onResult(false, granted, failed, new ArrayList<>());
                    return;
                }

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                if (!authenticate(in, out, context)) {
                    Log.e(TAG, "ADB 认证失败");
                    callback.onResult(false, granted, failed, new ArrayList<>());
                    return;
                }

                Log.i(TAG, "ADB 认证成功，开始授权...");

                List<String> signature = new ArrayList<>();
                for (String perm : BydPermissionHelper.getAllPermissions()) {
                    String shortName = perm.substring("android.permission.BYDAUTO_".length());
                    String cmd = "pm grant " + context.getPackageName() + " " + perm;
                    String result = execShell(in, out, cmd);
                    if (result != null && !result.contains("Exception") && !result.contains("Error")) {
                        Log.i(TAG, "  ✓ " + shortName);
                        granted.add(shortName);
                    } else if (result != null && result.contains("not a changeable permission type")) {
                        Log.w(TAG, "  ⚠ " + shortName + ": signature 级别权限");
                        signature.add(shortName);
                    } else {
                        Log.w(TAG, "  ✗ " + shortName + (result != null ? ": " + result.trim() : ""));
                        failed.add(shortName);
                    }
                }

                Log.i(TAG, "授权完成: 成功 " + granted.size() + ", signature " + signature.size() + ", 失败 " + failed.size());
                callback.onResult(failed.isEmpty(), granted, failed, signature);

            } catch (Exception e) {
                Log.e(TAG, "ADB 授权异常", e);
                callback.onResult(false, granted, failed, new ArrayList<>());
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

    private static Socket connect() throws IOException {
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(ADB_HOST, ADB_PORT), TIMEOUT);
        socket.setSoTimeout(15000);
        return socket;
    }

    private static boolean authenticate(InputStream in, OutputStream out, Context context) throws Exception {
        sendMessage(out, CMD_CNXN, VERSION, MAX_PAYLOAD, "host::features=shell_v2");

        int[] msg = readMessage(in);
        if (msg == null) return false;

        if (msg[0] == CMD_CNXN) {
            return true;
        }

        if (msg[0] != CMD_AUTH || msg[1] != AUTH_TOKEN) {
            return false;
        }

        byte[] token = readData(in, msg[3]);
        AdbKeyManager keyManager = new AdbKeyManager(context);

        byte[] sig = keyManager.signToken(token);
        sendMessage(out, CMD_AUTH, AUTH_SIGNATURE, 0, sig);

        msg = readMessage(in);
        if (msg == null) return false;
        if (msg[0] == CMD_CNXN) return true;

        if (msg[0] == CMD_AUTH && msg[1] == AUTH_TOKEN) {
            String pubKey = keyManager.getAdbPublicKeyString();
            sendMessage(out, CMD_AUTH, AUTH_RSAPUBLICKEY, 0,
                    pubKey.getBytes(StandardCharsets.UTF_8));

            Log.i(TAG, "等待用户在系统弹窗中点击\"允许 USB 调试\"...");

            msg = readMessage(in);
            return msg != null && msg[0] == CMD_CNXN;
        }

        return false;
    }

    private static String execShell(InputStream in, OutputStream out, String command) throws IOException {
        int localId = 1;

        sendMessage(out, CMD_OPEN, localId, 0, "shell:" + command);

        int[] msg = readMessage(in);
        if (msg == null) return null;

        if (msg[0] == CMD_CLSE) {
            sendMessage(out, CMD_CLSE, localId, msg[2], new byte[0]);
            return "";
        }

        if (msg[0] != CMD_OKAY) return null;
        int remoteId = msg[2];

        StringBuilder output = new StringBuilder();

        for (int i = 0; i < 50; i++) {
            msg = readMessage(in);
            if (msg == null) break;

            if (msg[0] == CMD_WRTE) {
                byte[] data = readData(in, msg[3]);
                output.append(new String(data, StandardCharsets.UTF_8));
                sendMessage(out, CMD_OKAY, localId, remoteId, new byte[0]);
            } else if (msg[0] == CMD_CLSE) {
                sendMessage(out, CMD_CLSE, localId, remoteId, new byte[0]);
                break;
            }
        }

        return output.toString();
    }

    private static void sendMessage(OutputStream out, int cmd, int arg0, int arg1, String data) throws IOException {
        sendMessage(out, cmd, arg0, arg1, data.getBytes(StandardCharsets.UTF_8));
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
