package com.diui.launcher.api;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;

public class BydPermissionContext extends ContextWrapper {

    public BydPermissionContext(Context base) {
        super(base);
    }

    @Override
    public void enforceCallingOrSelfPermission(String permission, String message) {
        if (isBydPermission(permission)) return;
        super.enforceCallingOrSelfPermission(permission, message);
    }

    @Override
    public int checkCallingOrSelfPermission(String permission) {
        if (isBydPermission(permission)) return PackageManager.PERMISSION_GRANTED;
        return super.checkCallingOrSelfPermission(permission);
    }

    @Override
    public void enforcePermission(String permission, int pid, int uid, String message) {
        if (isBydPermission(permission)) return;
        super.enforcePermission(permission, pid, uid, message);
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        if (isBydPermission(permission)) return PackageManager.PERMISSION_GRANTED;
        return super.checkPermission(permission, pid, uid);
    }

    private static boolean isBydPermission(String permission) {
        return permission != null && permission.startsWith("android.permission.BYDAUTO_");
    }
}
