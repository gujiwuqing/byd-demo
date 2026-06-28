package com.bydlauncher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Toast;

import com.bydlauncher.R;

public class MapPage {

    private static final String PKG_AMAP = "com.autonavi.minimap";
    private static final String PKG_BAIDU = "com.baidu.BaiduMap";
    private static final String PKG_GOOGLE = "com.google.android.apps.maps";

    private final View rootView;

    public MapPage(View rootView) {
        this.rootView = rootView;

        rootView.findViewById(R.id.map_amap).setOnClickListener(v -> launchMap(PKG_AMAP, "高德地图"));
        rootView.findViewById(R.id.map_baidu).setOnClickListener(v -> launchMap(PKG_BAIDU, "百度地图"));
        rootView.findViewById(R.id.map_google).setOnClickListener(v -> launchMap(PKG_GOOGLE, "Google Maps"));
    }

    private void launchMap(String packageName, String appName) {
        Context ctx = rootView.getContext();
        PackageManager pm = ctx.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        } else {
            Toast.makeText(ctx, appName + " 未安装", Toast.LENGTH_SHORT).show();
        }
    }
}
