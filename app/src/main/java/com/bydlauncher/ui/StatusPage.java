package com.bydlauncher.ui;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;
import com.bydlauncher.model.VehicleStatus;

public class StatusPage {

    private final View rootView;

    // ── 左侧区域 ──
    private final VehicleDiagramView vehicleDiagram;
    private final TextView tireFL, tireFR, tireRL, tireRR;
    private final TextView[] gears;
    private final TextView tvSpeed, tvBattery;
    private final ProgressBar progressBattery;

    // ── 右侧卡片：驾驶 ──
    private final TextView widgetSpeed;
    private final TextView[] widgetGears;

    // ── 右侧卡片：音乐 ──
    private final TextView widgetMusicTitle;
    private final TextView widgetMusicArtist;
    private final ImageView widgetMusicPrev;
    private final ImageView widgetMusicPlay;
    private final ImageView widgetMusicNext;

    // ── 右侧卡片：电量 ──
    private final TextView widgetBattery;
    private final ProgressBar widgetBatteryProgress;
    private final TextView widgetChargingStatus;

    // ── 右侧卡片：胎压 ──
    private final TextView widgetTireFL, widgetTireFR, widgetTireRL, widgetTireRR;

    // ── 右侧卡片：车门状态 ──
    private final VehicleDiagramView widgetDoorDiagram;
    private final TextView widgetDoorStatus;

    // ── 右侧卡片：应用 ──
    private final ImageView widgetApp1, widgetApp2, widgetApp3;
    private final TextView widgetAppAdd;

    public StatusPage(View rootView) {
        this.rootView = rootView;

        // ── 左侧区域 ──
        vehicleDiagram = rootView.findViewById(R.id.vehicle_diagram);
        tireFL = rootView.findViewById(R.id.tire_fl);
        tireFR = rootView.findViewById(R.id.tire_fr);
        tireRL = rootView.findViewById(R.id.tire_rl);
        tireRR = rootView.findViewById(R.id.tire_rr);

        gears = new TextView[]{
                rootView.findViewById(R.id.gear_p),
                rootView.findViewById(R.id.gear_r),
                rootView.findViewById(R.id.gear_n),
                rootView.findViewById(R.id.gear_d)
        };

        tvSpeed = rootView.findViewById(R.id.tv_speed);
        tvBattery = rootView.findViewById(R.id.tv_battery);
        progressBattery = rootView.findViewById(R.id.progress_battery);

        // ── 右侧卡片：驾驶 ──
        widgetSpeed = rootView.findViewById(R.id.widget_speed);
        widgetGears = new TextView[]{
                rootView.findViewById(R.id.widget_gear_p),
                rootView.findViewById(R.id.widget_gear_r),
                rootView.findViewById(R.id.widget_gear_n),
                rootView.findViewById(R.id.widget_gear_d)
        };

        // ── 右侧卡片：音乐 ──
        widgetMusicTitle = rootView.findViewById(R.id.widget_music_title);
        widgetMusicArtist = rootView.findViewById(R.id.widget_music_artist);
        widgetMusicPrev = rootView.findViewById(R.id.widget_music_prev);
        widgetMusicPlay = rootView.findViewById(R.id.widget_music_play);
        widgetMusicNext = rootView.findViewById(R.id.widget_music_next);

        // ── 右侧卡片：电量 ──
        widgetBattery = rootView.findViewById(R.id.widget_battery);
        widgetBatteryProgress = rootView.findViewById(R.id.widget_battery_progress);
        widgetChargingStatus = rootView.findViewById(R.id.widget_charging_status);

        // ── 右侧卡片：胎压 ──
        widgetTireFL = rootView.findViewById(R.id.widget_tire_fl);
        widgetTireFR = rootView.findViewById(R.id.widget_tire_fr);
        widgetTireRL = rootView.findViewById(R.id.widget_tire_rl);
        widgetTireRR = rootView.findViewById(R.id.widget_tire_rr);

        // ── 右侧卡片：车门状态 ──
        widgetDoorDiagram = rootView.findViewById(R.id.widget_door_diagram);
        widgetDoorStatus = rootView.findViewById(R.id.widget_door_status);

        // ── 右侧卡片：应用 ──
        widgetApp1 = rootView.findViewById(R.id.widget_app_1);
        widgetApp2 = rootView.findViewById(R.id.widget_app_2);
        widgetApp3 = rootView.findViewById(R.id.widget_app_3);
        widgetAppAdd = rootView.findViewById(R.id.widget_app_add);
    }

    public void updateStatus(VehicleStatus s) {
        // ── 左侧：车辆图 ──
        vehicleDiagram.setDoorStates(
                s.doorLeftFrontOpen, s.doorRightFrontOpen,
                s.doorLeftRearOpen, s.doorRightRearOpen,
                s.trunkOpen, s.hoodOpen);

        // ── 左侧：胎压（模拟数据） ──
        tireFL.setText("37");
        tireFR.setText("36");
        tireRL.setText("35");
        tireRR.setText("35");

        // ── 左侧：挡位（模拟：P 挡） ──
        int gearIndex = 0;
        for (int i = 0; i < gears.length; i++) {
            gears[i].setTextColor(ContextCompat.getColor(rootView.getContext(),
                    i == gearIndex ? R.color.gear_active : R.color.gear_inactive));
        }

        // ── 左侧：速度 ──
        tvSpeed.setText("0");

        // ── 左侧：电量 ──
        int batteryVal = s.getBatteryValue();
        tvBattery.setText(s.getBatteryText());
        progressBattery.setProgress(batteryVal);

        // ── 右侧卡片：驾驶（同步左侧数据） ──
        widgetSpeed.setText("0");
        for (int i = 0; i < widgetGears.length; i++) {
            widgetGears[i].setTextColor(ContextCompat.getColor(rootView.getContext(),
                    i == gearIndex ? R.color.gear_active : R.color.gear_inactive));
        }

        // ── 右侧卡片：电量（同步左侧数据） ──
        widgetBattery.setText(s.getBatteryText());
        widgetBatteryProgress.setProgress(batteryVal);
        widgetChargingStatus.setText(R.string.not_charging);

        // ── 右侧卡片：胎压（同步左侧数据） ──
        widgetTireFL.setText("37");
        widgetTireFR.setText("36");
        widgetTireRL.setText("35");
        widgetTireRR.setText("35");

        // ── 右侧卡片：车门状态 ──
        widgetDoorDiagram.setDoorStates(
                s.doorLeftFrontOpen, s.doorRightFrontOpen,
                s.doorLeftRearOpen, s.doorRightRearOpen,
                s.trunkOpen, s.hoodOpen);
        if (s.hasAnyDoorOpen()) {
            widgetDoorStatus.setText(R.string.unlocked_text);
            widgetDoorStatus.setTextColor(ContextCompat.getColor(rootView.getContext(), R.color.status_fair));
        } else {
            widgetDoorStatus.setText(R.string.all_closed);
            widgetDoorStatus.setTextColor(ContextCompat.getColor(rootView.getContext(), R.color.status_good));
        }
    }
}
