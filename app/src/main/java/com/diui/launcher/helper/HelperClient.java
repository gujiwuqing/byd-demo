package com.diui.launcher.helper;

import android.util.Log;

import com.diui.launcher.api.AutoserviceClient;
import com.diui.launcher.api.FidRegistry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class HelperClient {

    private static final String TAG = "HelperClient";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 19876;
    private static final int TIMEOUT = 3000;

    private boolean available = false;

    public HelperClient() {
        checkAvailability();
    }

    public void checkAvailability() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(HOST, PORT), 1000);
            available = true;
        } catch (Exception e) {
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public int getInt(int dev, int fid) {
        String resp = sendCommand("GET " + dev + " " + fid);
        return parseIntResponse(resp);
    }

    public float getFloat(int dev, int fid) {
        String resp = sendCommand("GETF " + dev + " " + fid);
        String parcel = extractParcel(resp);
        int bits = AutoserviceClient.parseParcelInt(parcel);
        return bits != -1 ? Float.intBitsToFloat(bits) : -1.0f;
    }

    public boolean setInt(int dev, int fid, int val) {
        String resp = sendCommand("SET " + dev + " " + fid + " " + val);
        return resp != null && resp.startsWith("OK");
    }

    private String sendCommand(String cmd) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);
            socket.setSoTimeout(TIMEOUT);
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            out.println(cmd);
            return in.readLine();
        } catch (Exception e) {
            Log.w(TAG, "Command failed: " + cmd, e);
            available = false;
            return null;
        }
    }

    private int parseIntResponse(String resp) {
        String parcel = extractParcel(resp);
        return AutoserviceClient.parseParcelInt(parcel);
    }

    private String extractParcel(String resp) {
        if (resp == null) return null;
        if (resp.startsWith("OK ")) return resp.substring(3);
        return resp;
    }
}
