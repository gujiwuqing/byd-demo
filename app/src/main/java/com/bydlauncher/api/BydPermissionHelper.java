package com.bydlauncher.api;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BydPermissionHelper {

    private static final String TAG = "BydPermissionHelper";

    private static final String[] BYD_PERMISSIONS = {
            "android.permission.BYDAUTO_AC_GET",
            "android.permission.BYDAUTO_AC_SET",
            "android.permission.BYDAUTO_AC_COMMON",
            "android.permission.BYDAUTO_BODYWORK_GET",
            "android.permission.BYDAUTO_BODYWORK_COMMON",
            "android.permission.BYDAUTO_DOOR_LOCK_GET",
            "android.permission.BYDAUTO_DOOR_LOCK_COMMON",
            "android.permission.BYDAUTO_POWER_GET",
            "android.permission.BYDAUTO_ENERGY_GET",
            "android.permission.BYDAUTO_PM2P5_GET",
            "android.permission.BYDAUTO_STATISTIC_GET",
            "android.permission.BYDAUTO_GEARBOX_GET",
            "android.permission.BYDAUTO_SPEED_GET",
            "android.permission.BYDAUTO_CHARGING_GET",
            "android.permission.BYDAUTO_TYRE_GET",
            "android.permission.BYDAUTO_LIGHT_GET",
            "android.permission.BYDAUTO_LIGHT_SET",
            "android.permission.BYDAUTO_SETTING_GET",
    };

    public static void diagnosePermissions(Context context) {
        int uid = context.getApplicationInfo().uid;
        String packageName = context.getPackageName();
        PackageManager pm = context.getPackageManager();

        Log.i(TAG, "========== BYD 权限诊断 ==========");
        Log.i(TAG, "应用 UID: " + uid);
        Log.i(TAG, "应用包名: " + packageName);

        int granted = 0;
        List<String> missing = new ArrayList<>();

        for (String perm : BYD_PERMISSIONS) {
            int status = pm.checkPermission(perm, packageName);
            String shortName = perm.substring("android.permission.BYDAUTO_".length());
            if (status == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "  ✓ " + shortName + " — 已授权");
                granted++;
            } else {
                Log.w(TAG, "  ✗ " + shortName + " — 未授权");
                missing.add(perm);
            }
        }

        Log.i(TAG, "已授权: " + granted + "/" + BYD_PERMISSIONS.length);

        if (!missing.isEmpty()) {
            Log.w(TAG, "-----------------------------------");
            Log.w(TAG, "检测到 " + missing.size() + " 个权限缺失");
            Log.w(TAG, "请在 adb shell 中执行以下命令:");
            Log.w(TAG, "adb connect 192.168.10.10:5555");
            Log.w(TAG, "adb shell");
            for (String perm : missing) {
                Log.w(TAG, "  pm grant " + packageName + " " + perm);
            }
            Log.w(TAG, "执行后重启应用即可");
        } else {
            Log.i(TAG, "所有 BYD 权限已授权");
        }

        Log.i(TAG, "==================================");
    }

    public static String[] getAllPermissions() {
        return BYD_PERMISSIONS;
    }

    public static boolean hasAllPermissions(Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        for (String perm : BYD_PERMISSIONS) {
            if (pm.checkPermission(perm, packageName) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static List<String> getMissingPermissions(Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        List<String> missing = new ArrayList<>();
        for (String perm : BYD_PERMISSIONS) {
            if (pm.checkPermission(perm, packageName) != PackageManager.PERMISSION_GRANTED) {
                missing.add(perm);
            }
        }
        return missing;
    }

    public static int getGrantedCount(Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();
        int count = 0;
        for (String perm : BYD_PERMISSIONS) {
            if (pm.checkPermission(perm, packageName) == PackageManager.PERMISSION_GRANTED) {
                count++;
            }
        }
        return count;
    }
}
