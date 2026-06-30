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

    // 电量油量续航
    private final TextView tvBattery, tvEvRange, tvFuel, tvFuelAmount, tvTotalRange;
    private final ProgressBar progressBattery;

    // 速度挡位功率温度
    private final TextView tvSpeed, tvOutsideTemp, tvPowerKw;
    private final TextView gearP, gearR, gearN, gearD;

    // 能耗
    private final TextView tvElecConsumption, tvFuelConsumption;

    // 胎压胎温
    private final TextView tireFLPressure, tireFRPressure, tireRLPressure, tireRRPressure;
    private final TextView tireFLTemp, tireFRTemp, tireRLTemp, tireRRTemp;

    // 行程里程
    private final TextView tvTripDistance, tvTripTime;
    private final TextView tvSmartCharge, tvRecoveryMode;
    private final TextView tvTotalMileage, tvHevMileage;

    public StatusPage(View rootView) {
        this.rootView = rootView;
        this.context = rootView.getContext();

        tvBattery = rootView.findViewById(R.id.tv_battery);
        tvEvRange = rootView.findViewById(R.id.tv_ev_range);
        progressBattery = rootView.findViewById(R.id.progress_battery);
        tvFuel = rootView.findViewById(R.id.tv_fuel);
        tvFuelAmount = rootView.findViewById(R.id.tv_fuel_amount);
        tvTotalRange = rootView.findViewById(R.id.tv_total_range);

        tvSpeed = rootView.findViewById(R.id.tv_speed);
        tvOutsideTemp = rootView.findViewById(R.id.tv_outside_temp);
        tvPowerKw = rootView.findViewById(R.id.tv_power_kw);
        gearP = rootView.findViewById(R.id.gear_p);
        gearR = rootView.findViewById(R.id.gear_r);
        gearN = rootView.findViewById(R.id.gear_n);
        gearD = rootView.findViewById(R.id.gear_d);

        tvElecConsumption = rootView.findViewById(R.id.tv_elec_consumption);
        tvFuelConsumption = rootView.findViewById(R.id.tv_fuel_consumption);

        tireFLPressure = rootView.findViewById(R.id.tv_tire_fl_pressure);
        tireFRPressure = rootView.findViewById(R.id.tv_tire_fr_pressure);
        tireRLPressure = rootView.findViewById(R.id.tv_tire_rl_pressure);
        tireRRPressure = rootView.findViewById(R.id.tv_tire_rr_pressure);
        tireFLTemp = rootView.findViewById(R.id.tv_tire_fl_temp);
        tireFRTemp = rootView.findViewById(R.id.tv_tire_fr_temp);
        tireRLTemp = rootView.findViewById(R.id.tv_tire_rl_temp);
        tireRRTemp = rootView.findViewById(R.id.tv_tire_rr_temp);

        tvTripDistance = rootView.findViewById(R.id.tv_trip_distance);
        tvTripTime = rootView.findViewById(R.id.tv_trip_time);
        tvSmartCharge = rootView.findViewById(R.id.tv_smart_charge);
        tvRecoveryMode = rootView.findViewById(R.id.tv_recovery_mode);
        tvTotalMileage = rootView.findViewById(R.id.tv_total_mileage);
        tvHevMileage = rootView.findViewById(R.id.tv_hev_mileage);

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
        // 电量
        tvBattery.setText(s.getBatteryText());
        progressBattery.setProgress(s.getBatteryValue());
        tvEvRange.setText(s.getEvMileageText());

        // 油量
        if (s.fuelPercent >= 0) {
            tvFuel.setText(s.fuelPercent + "%");
        }
        tvFuelAmount.setText(s.getFuelText());

        // 总续航
        tvTotalRange.setText(s.getTotalRangeText());

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
        if (s.currentFuelConsumption >= 0) {
            tvFuelConsumption.setText(String.format("当前：%.1f", s.currentFuelConsumption));
        }

        // 胎压胎温
        if (s.tirePressureFL >= 0) {
            tireFLPressure.setText(String.valueOf(s.tirePressureFL));
            tireFRPressure.setText(String.valueOf(s.tirePressureFR));
            tireRLPressure.setText(String.valueOf(s.tirePressureRL));
            tireRRPressure.setText(String.valueOf(s.tirePressureRR));
            tireFLTemp.setText(s.tireTempFL + "°C");
            tireFRTemp.setText(s.tireTempFR + "°C");
            tireRLTemp.setText(s.tireTempRL + "°C");
            tireRRTemp.setText(s.tireTempRR + "°C");
        }

        // 行程
        tvTripDistance.setText(String.format("%.1fkm", s.tripDistance));
        tvTripTime.setText(s.tripTime);

        // 能量模式
        tvSmartCharge.setText("智保" + s.smartChargePercent + "%");
        tvRecoveryMode.setText(s.recoveryMode);

        // 里程
        tvTotalMileage.setText(s.getTotalMileageText());
        tvHevMileage.setText(s.getHevMileageText());
    }
}
