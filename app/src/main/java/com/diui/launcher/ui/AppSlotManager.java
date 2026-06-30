package com.diui.launcher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.diui.launcher.R;

import java.util.ArrayList;
import java.util.List;

public class AppSlotManager {

    private static final String PREFS_NAME = "app_slots";

    public static final String SLOT_NAV = "app_nav";
    public static final String SLOT_MUSIC = "app_music";
    public static final String SLOT_VIDEO = "app_video";
    public static final String SLOT_PHONE = "app_phone";

    private final Context context;
    private final SharedPreferences prefs;

    public AppSlotManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getPackageName(String slot) {
        return prefs.getString(slot, null);
    }

    public void setPackageName(String slot, String packageName) {
        prefs.edit().putString(slot, packageName).apply();
    }

    public boolean isConfigured(String slot) {
        String pkg = getPackageName(slot);
        return pkg != null && isInstalled(pkg);
    }

    public boolean isInstalled(String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public String getAppLabel(String slot) {
        String pkg = getPackageName(slot);
        if (pkg == null) return context.getString(R.string.settings_app_not_set);
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return context.getString(R.string.unbounded_app_uninstalled);
        }
    }

    public Drawable getAppIcon(String slot) {
        String pkg = getPackageName(slot);
        if (pkg == null) return null;
        try {
            return context.getPackageManager().getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public boolean launch(String slot) {
        String pkg = getPackageName(slot);
        if (pkg == null) {
            Toast.makeText(context, R.string.unbounded_no_nav, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isInstalled(pkg)) {
            Toast.makeText(context, R.string.unbounded_app_uninstalled, Toast.LENGTH_SHORT).show();
            prefs.edit().remove(slot).apply();
            return false;
        }
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    public static class AppInfo {
        public final String packageName;
        public final String label;
        public final Drawable icon;

        public AppInfo(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    public List<AppInfo> getInstalledApps() {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        List<AppInfo> result = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue;
            if (app.packageName.equals(context.getPackageName())) continue;
            String label = pm.getApplicationLabel(app).toString();
            Drawable icon = pm.getApplicationIcon(app);
            result.add(new AppInfo(app.packageName, label, icon));
        }
        result.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        return result;
    }
}
