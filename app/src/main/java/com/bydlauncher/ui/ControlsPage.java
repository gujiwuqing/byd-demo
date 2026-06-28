package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;
import com.bydlauncher.api.BydAcApi;
import com.bydlauncher.api.BydBodyworkApi;
import com.bydlauncher.model.VehicleStatus;

public class ControlsPage {

    private final View rootView;
    private final BydAcApi acApi;
    private final BydBodyworkApi bodyworkApi;

    private final TextView btnAcPower, tvAcTemp, tvAcStatus;
    private final TextView[] windBtns;
    private final TextView btnModeFace, btnModeFoot, btnModeDefrost;
    private final TextView btnCycleInner, btnCycleOuter;
    private final TextView btnAuto, btnManual;
    private final TextView lockIcon, lockText, lockStatus, doorStatus;

    private int currentTemp = 25;

    public ControlsPage(View rootView, BydAcApi acApi, BydBodyworkApi bodyworkApi) {
        this.rootView = rootView;
        this.acApi = acApi;
        this.bodyworkApi = bodyworkApi;

        btnAcPower = rootView.findViewById(R.id.ctrl_ac_power);
        tvAcTemp = rootView.findViewById(R.id.ctrl_ac_temp);
        tvAcStatus = rootView.findViewById(R.id.ctrl_ac_status);

        windBtns = new TextView[]{
                rootView.findViewById(R.id.ctrl_wind_0), rootView.findViewById(R.id.ctrl_wind_1),
                rootView.findViewById(R.id.ctrl_wind_2), rootView.findViewById(R.id.ctrl_wind_3),
                rootView.findViewById(R.id.ctrl_wind_4), rootView.findViewById(R.id.ctrl_wind_5),
                rootView.findViewById(R.id.ctrl_wind_6), rootView.findViewById(R.id.ctrl_wind_7)
        };

        btnModeFace = rootView.findViewById(R.id.ctrl_mode_face);
        btnModeFoot = rootView.findViewById(R.id.ctrl_mode_foot);
        btnModeDefrost = rootView.findViewById(R.id.ctrl_mode_defrost);
        btnCycleInner = rootView.findViewById(R.id.ctrl_cycle_inner);
        btnCycleOuter = rootView.findViewById(R.id.ctrl_cycle_outer);
        btnAuto = rootView.findViewById(R.id.ctrl_auto);
        btnManual = rootView.findViewById(R.id.ctrl_manual);

        lockIcon = rootView.findViewById(R.id.ctrl_lock_icon);
        lockText = rootView.findViewById(R.id.ctrl_lock_text);
        lockStatus = rootView.findViewById(R.id.ctrl_lock_status);
        doorStatus = rootView.findViewById(R.id.ctrl_door_status);

        initClickListeners();
    }

    private void initClickListeners() {
        btnAcPower.setOnClickListener(v -> acApi.toggle());

        rootView.findViewById(R.id.ctrl_temp_down).setOnClickListener(v -> {
            currentTemp = Math.max(17, currentTemp - 1);
            acApi.setMainTemp(currentTemp);
            tvAcTemp.setText(currentTemp + "°C");
        });
        rootView.findViewById(R.id.ctrl_temp_up).setOnClickListener(v -> {
            currentTemp = Math.min(33, currentTemp + 1);
            acApi.setMainTemp(currentTemp);
            tvAcTemp.setText(currentTemp + "°C");
        });

        for (int i = 0; i < windBtns.length; i++) {
            final int level = i;
            windBtns[i].setOnClickListener(v -> {
                acApi.setWindLevel(level);
                highlightWind(level);
            });
        }

        btnModeFace.setOnClickListener(v -> { acApi.setWindMode(BydAcApi.WIND_MODE_FACE); highlightWindMode(BydAcApi.WIND_MODE_FACE); });
        btnModeFoot.setOnClickListener(v -> { acApi.setWindMode(BydAcApi.WIND_MODE_FOOT); highlightWindMode(BydAcApi.WIND_MODE_FOOT); });
        btnModeDefrost.setOnClickListener(v -> { acApi.setWindMode(BydAcApi.WIND_MODE_DEFROST); highlightWindMode(BydAcApi.WIND_MODE_DEFROST); });

        btnCycleInner.setOnClickListener(v -> { acApi.setCycleMode(BydAcApi.CYCLE_INNER); highlightCycle(BydAcApi.CYCLE_INNER); });
        btnCycleOuter.setOnClickListener(v -> { acApi.setCycleMode(BydAcApi.CYCLE_OUTER); highlightCycle(BydAcApi.CYCLE_OUTER); });

        btnAuto.setOnClickListener(v -> { acApi.setControlMode(BydAcApi.MODE_AUTO); highlightControl(BydAcApi.MODE_AUTO); });
        btnManual.setOnClickListener(v -> { acApi.setControlMode(BydAcApi.MODE_MANUAL); highlightControl(BydAcApi.MODE_MANUAL); });

        rootView.findViewById(R.id.ctrl_lock_btn).setOnClickListener(v -> bodyworkApi.toggleLock());
    }

    public void updateStatus(VehicleStatus s) {
        if (s.acOn) {
            btnAcPower.setText("ON");
            btnAcPower.setTextColor(ContextCompat.getColor(rootView.getContext(), R.color.accent));
            tvAcStatus.setText(s.getAcModeText() + " · " + s.getCycleModeText());
        } else {
            btnAcPower.setText("OFF");
            btnAcPower.setTextColor(ContextCompat.getColor(rootView.getContext(), R.color.text_tertiary));
            tvAcStatus.setText("已关闭");
        }

        if (s.acTemp > 0 && s.acTemp < 100) {
            currentTemp = s.acTemp;
            tvAcTemp.setText(s.acTemp + "°C");
        }

        highlightWind(s.acWindLevel);
        highlightWindMode(s.acWindMode);
        highlightCycle(s.acCycleMode);
        highlightControl(s.acControlMode);

        if (s.isLocked) {
            lockIcon.setText("🔒");
            lockText.setText("已锁车");
            lockStatus.setText("🔒 已锁车");
            lockStatus.setTextColor(ContextCompat.getColor(rootView.getContext(), R.color.status_good));
        } else {
            lockIcon.setText("🔓");
            lockText.setText("已解锁");
            lockStatus.setText("🔓 已解锁");
            lockStatus.setTextColor(ContextCompat.getColor(rootView.getContext(), R.color.status_fair));
        }

        if (s.hasAnyDoorOpen()) {
            StringBuilder doors = new StringBuilder("🚪 ");
            if (s.doorLeftFrontOpen) doors.append("左前 ");
            if (s.doorRightFrontOpen) doors.append("右前 ");
            if (s.doorLeftRearOpen) doors.append("左后 ");
            if (s.doorRightRearOpen) doors.append("右后 ");
            if (s.trunkOpen) doors.append("后备箱 ");
            if (s.hoodOpen) doors.append("引擎盖 ");
            doorStatus.setText(doors.toString().trim());
            doorStatus.setTextColor(ContextCompat.getColor(rootView.getContext(), R.color.door_open));
        } else {
            doorStatus.setText("🚪 全部关闭");
            doorStatus.setTextColor(ContextCompat.getColor(rootView.getContext(), R.color.status_good));
        }
    }

    private void highlightWind(int level) {
        for (int i = 0; i < windBtns.length; i++) {
            windBtns[i].setSelected(i == level);
            windBtns[i].setTextColor(ContextCompat.getColor(rootView.getContext(),
                    i == level ? R.color.accent : R.color.text_primary));
        }
    }

    private void highlightWindMode(int mode) {
        btnModeFace.setSelected(mode == BydAcApi.WIND_MODE_FACE);
        btnModeFoot.setSelected(mode == BydAcApi.WIND_MODE_FOOT);
        btnModeDefrost.setSelected(mode == BydAcApi.WIND_MODE_DEFROST);
        btnModeFace.setTextColor(ContextCompat.getColor(rootView.getContext(), mode == BydAcApi.WIND_MODE_FACE ? R.color.accent : R.color.text_primary));
        btnModeFoot.setTextColor(ContextCompat.getColor(rootView.getContext(), mode == BydAcApi.WIND_MODE_FOOT ? R.color.accent : R.color.text_primary));
        btnModeDefrost.setTextColor(ContextCompat.getColor(rootView.getContext(), mode == BydAcApi.WIND_MODE_DEFROST ? R.color.accent : R.color.text_primary));
    }

    private void highlightCycle(int mode) {
        btnCycleInner.setSelected(mode == BydAcApi.CYCLE_INNER);
        btnCycleOuter.setSelected(mode == BydAcApi.CYCLE_OUTER);
        btnCycleInner.setTextColor(ContextCompat.getColor(rootView.getContext(), mode == BydAcApi.CYCLE_INNER ? R.color.accent : R.color.text_primary));
        btnCycleOuter.setTextColor(ContextCompat.getColor(rootView.getContext(), mode == BydAcApi.CYCLE_OUTER ? R.color.accent : R.color.text_primary));
    }

    private void highlightControl(int mode) {
        btnAuto.setSelected(mode == BydAcApi.MODE_AUTO);
        btnManual.setSelected(mode == BydAcApi.MODE_MANUAL);
        btnAuto.setTextColor(ContextCompat.getColor(rootView.getContext(), mode == BydAcApi.MODE_AUTO ? R.color.accent : R.color.text_primary));
        btnManual.setTextColor(ContextCompat.getColor(rootView.getContext(), mode == BydAcApi.MODE_MANUAL ? R.color.accent : R.color.text_primary));
    }
}
