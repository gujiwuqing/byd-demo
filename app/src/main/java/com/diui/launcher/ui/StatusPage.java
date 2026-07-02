package com.diui.launcher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.diui.launcher.R;
import com.diui.launcher.model.VehicleStatus;

public class StatusPage {

    private final View rootView;
    private final Context context;
    private AppSlotManager appSlotManager;

    public void setAppSlotManager(AppSlotManager manager) {
        this.appSlotManager = manager;
    }

    // 速度挡位功率温度
    private final TextView tvSpeed, tvOutsideTemp, tvPowerKw;
    private final TextView gearP, gearR, gearN, gearD;

    // 能耗
    private final TextView tvElecConsumption;

    // 行程里程
    private final TextView tvTripDistance, tvTripTime;
    private final TextView tvTotalMileage, tvHevMileage;

    // 车图(整合胎压 + 车门)
    private VehicleDiagramView vehicleDiagram;

    public StatusPage(View rootView) {
        this.rootView = rootView;
        this.context = rootView.getContext();

        tvSpeed = rootView.findViewById(R.id.tv_speed);
        tvOutsideTemp = rootView.findViewById(R.id.tv_outside_temp);
        tvPowerKw = rootView.findViewById(R.id.tv_power_kw);
        gearP = rootView.findViewById(R.id.gear_p);
        gearR = rootView.findViewById(R.id.gear_r);
        gearN = rootView.findViewById(R.id.gear_n);
        gearD = rootView.findViewById(R.id.gear_d);

        tvElecConsumption = rootView.findViewById(R.id.tv_elec_consumption);

        tvTripDistance = rootView.findViewById(R.id.tv_trip_distance);
        tvTripTime = rootView.findViewById(R.id.tv_trip_time);
        tvTotalMileage = rootView.findViewById(R.id.tv_total_mileage);
        tvHevMileage = rootView.findViewById(R.id.tv_hev_mileage);

        vehicleDiagram = rootView.findViewById(R.id.vehicle_diagram);

        initPipArea();
    }

    private void initPipArea() {
        rootView.findViewById(R.id.pip_map).setOnClickListener(v -> {
            if (appSlotManager != null && appSlotManager.isConfigured(AppSlotManager.SLOT_NAV)) {
                appSlotManager.launch(AppSlotManager.SLOT_NAV);
            } else {
                launchApp("com.autonavi.minimap", "高德地图");
            }
        });
        rootView.findViewById(R.id.pip_music).setOnClickListener(v -> {
            if (appSlotManager != null && appSlotManager.isConfigured(AppSlotManager.SLOT_MUSIC)) {
                appSlotManager.launch(AppSlotManager.SLOT_MUSIC);
            } else {
                launchApp("com.tencent.qqmusic", "QQ音乐");
            }
        });
        rootView.findViewById(R.id.pip_video).setOnClickListener(v -> {
            if (appSlotManager != null && appSlotManager.isConfigured(AppSlotManager.SLOT_VIDEO)) {
                appSlotManager.launch(AppSlotManager.SLOT_VIDEO);
            } else {
                launchApp("com.tencent.qqlive", "腾讯视频");
            }
        });
        rootView.findViewById(R.id.pip_phone).setOnClickListener(v -> {
            if (appSlotManager != null && appSlotManager.isConfigured(AppSlotManager.SLOT_PHONE)) {
                appSlotManager.launch(AppSlotManager.SLOT_PHONE);
            } else {
                launchApp("com.android.dialer", "电话");
            }
        });
    }

    private void launchApp(String packageName, String appName) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, appName + " 未安装", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateStatus(VehicleStatus s) {
        // 速度
        tvSpeed.setText(s.getSpeedText());
        tvOutsideTemp.setText(s.getOutsideTempText());

        // 挡位
        TextView[] gears = {gearP, gearR, gearN, gearD};
        for (int i = 0; i < gears.length; i++) {
            gears[i].setTextColor(ContextCompat.getColor(context,
                    i == s.gear ? R.color.accent : R.color.gear_inactive));
        }

        // 功率
        tvPowerKw.setText(s.getPowerKwText());

        // 能耗
        if (s.currentElecConsumption >= 0) {
            tvElecConsumption.setText(String.format("%.1f度", s.currentElecConsumption));
        }

        // 行程
        tvTripDistance.setText(String.format("%.1fkm", s.tripDistance));
        tvTripTime.setText(s.tripTime);

        // 里程
        tvTotalMileage.setText(s.getTotalMileageText());
        tvHevMileage.setText(s.getHevMileageText());

        // 车图:胎压 + 车门(VehicleStatus 字段为 public,直接访问)
        if (vehicleDiagram != null) {
            vehicleDiagram.setTireData(
                    s.tirePressureFL, s.tireTempFL,
                    s.tirePressureFR, s.tireTempFR,
                    s.tirePressureRL, s.tireTempRL,
                    s.tirePressureRR, s.tireTempRR);
            vehicleDiagram.setDoorStates(
                    s.doorLeftFrontOpen, s.doorRightFrontOpen,
                    s.doorLeftRearOpen, s.doorRightRearOpen,
                    s.trunkOpen, s.hoodOpen);
        }
    }
}
