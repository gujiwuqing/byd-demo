package com.bydlauncher.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class HelperDaemon {

    private static final String LOCK_FILE = "/data/local/tmp/bydui_helper.lock";
    private static final int PORT = 19876;
    private static final long IDLE_TIMEOUT_MS = 10 * 60 * 1000;

    private static volatile long lastRequestTime = System.currentTimeMillis();

    public static void main(String[] args) {
        System.out.println("[HelperDaemon] Starting...");

        try {
            RandomAccessFile lockFile = new RandomAccessFile(LOCK_FILE, "rw");
            FileChannel channel = lockFile.getChannel();
            FileLock lock = channel.tryLock();
            if (lock == null) {
                System.out.println("[HelperDaemon] Another instance running, exiting");
                return;
            }
        } catch (Exception e) {
            System.out.println("[HelperDaemon] Lock failed: " + e.getMessage());
        }

        Thread idleMonitor = new Thread(() -> {
            while (true) {
                try { Thread.sleep(60000); } catch (InterruptedException e) { return; }
                if (System.currentTimeMillis() - lastRequestTime > IDLE_TIMEOUT_MS) {
                    System.out.println("[HelperDaemon] Idle timeout, exiting");
                    System.exit(0);
                }
            }
        }, "idle-monitor");
        idleMonitor.setDaemon(true);
        idleMonitor.start();

        try (ServerSocket server = new ServerSocket(PORT, 5, InetAddress.getByName("127.0.0.1"))) {
            System.out.println("[HelperDaemon] Listening on 127.0.0.1:" + PORT);

            while (true) {
                Socket client = server.accept();
                client.setSoTimeout(5000);
                handleClient(client);
            }
        } catch (Exception e) {
            System.out.println("[HelperDaemon] Server error: " + e.getMessage());
        }
    }

    private static void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()), true)) {

            String line = in.readLine();
            if (line == null) return;
            lastRequestTime = System.currentTimeMillis();

            String[] parts = line.trim().split("\\s+");
            if (parts.length < 3) {
                out.println("ERR invalid command");
                return;
            }

            String cmd = parts[0];
            int dev, fid;
            try {
                dev = Integer.parseInt(parts[1]);
                fid = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                out.println("ERR invalid params");
                return;
            }

            if ("SET".equals(cmd)) {
                if (!WriteAllowlist.isWriteAllowed(dev)) {
                    out.println("ERR write banned for device " + dev);
                    return;
                }
                if (parts.length < 4) {
                    out.println("ERR missing value");
                    return;
                }
                int val = Integer.parseInt(parts[3]);
                String result = execServiceCall(6, dev, fid, val);
                out.println("OK " + result);
            } else if ("GET".equals(cmd)) {
                String result = execServiceCall(5, dev, fid, -1);
                out.println("OK " + result);
            } else if ("GETF".equals(cmd)) {
                String result = execServiceCall(7, dev, fid, -1);
                out.println("OK " + result);
            } else {
                out.println("ERR unknown command: " + cmd);
            }

        } catch (Exception e) {
            System.out.println("[HelperDaemon] Client error: " + e.getMessage());
        } finally {
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private static String execServiceCall(int tx, int dev, int fid, int val) {
        try {
            String cmd;
            if (tx == 6) {
                cmd = "service call autoservice " + tx + " i32 " + dev + " i32 " + fid + " i32 " + val;
            } else {
                cmd = "service call autoservice " + tx + " i32 " + dev + " i32 " + fid;
            }
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            p.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "ERR " + e.getMessage();
        }
    }
}
