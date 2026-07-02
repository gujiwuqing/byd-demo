package com.diui.launcher.ui;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.diui.launcher.R;
import com.diui.launcher.api.BydAcApi;
import com.diui.launcher.model.VehicleStatus;

public class ControlsPage {

    private final View rootView;
    private final BydAcApi acApi;

    private final TextView btnAcPower, tvAcTemp, tvAcStatus;
    private WindLevelView windView;
    private TextView windLabel;
    private int currentWindLevel = 0;
    private final TextView btnModeFace, btnModeFoot, btnModeDefrost;
    private final TextView btnCycleInner, btnCycleOuter;
    private final TextView btnAuto, btnManual;

    private int currentTemp = 25;

    public ControlsPage(View rootView, BydAcApi acApi) {
        this.rootView = rootView;
        this.acApi = acApi;

        btnAcPower = rootView.findViewById(R.id.ctrl_ac_power);
        tvAcTemp = rootView.findViewById(R.id.ctrl_ac_temp);
        tvAcStatus = rootView.findViewById(R.id.ctrl_ac_status);

        windView = rootView.findViewById(R.id.ctrl_wind_view);
        windLabel = rootView.findViewById(R.id.ctrl_wind_label);

        btnModeFace = rootView.findViewById(R.id.ctrl_mode_face);
        btnModeFoot = rootView.findViewById(R.id.ctrl_mode_foot);
        btnModeDefrost = rootView.findViewById(R.id.ctrl_mode_defrost);
        btnCycleInner = rootView.findViewById(R.id.ctrl_cycle_inner);
        btnCycleOuter = rootView.findViewById(R.id.ctrl_cycle_outer);
        btnAuto = rootView.findViewById(R.id.ctrl_auto);
        btnManual = rootView.findViewById(R.id.ctrl_manual);

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

        if (windView != null) {
            windView.setOnClickListener(v -> {
                int next = (currentWindLevel + 1) % 8;
                acApi.setWindLevel(next);
            });
        }

        btnModeFace.setOnClickListener(v -> { acApi.setWindMode(BydAcApi.WIND_MODE_FACE); highlightWindMode(BydAcApi.WIND_MODE_FACE); });
        btnModeFoot.setOnClickListener(v -> { acApi.setWindMode(BydAcApi.WIND_MODE_FOOT); highlightWindMode(BydAcApi.WIND_MODE_FOOT); });
        btnModeDefrost.setOnClickListener(v -> { acApi.setWindMode(BydAcApi.WIND_MODE_DEFROST); highlightWindMode(BydAcApi.WIND_MODE_DEFROST); });

        btnCycleInner.setOnClickListener(v -> { acApi.setCycleMode(BydAcApi.CYCLE_INNER); highlightCycle(BydAcApi.CYCLE_INNER); });
        btnCycleOuter.setOnClickListener(v -> { acApi.setCycleMode(BydAcApi.CYCLE_OUTER); highlightCycle(BydAcApi.CYCLE_OUTER); });

        btnAuto.setOnClickListener(v -> { acApi.setControlMode(BydAcApi.MODE_AUTO); highlightControl(BydAcApi.MODE_AUTO); });
        btnManual.setOnClickListener(v -> { acApi.setControlMode(BydAcApi.MODE_MANUAL); highlightControl(BydAcApi.MODE_MANUAL); });
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
    }

    private void highlightWind(int level) {
        currentWindLevel = level;
        if (windView != null) windView.setLevel(level);
        if (windLabel != null) windLabel.setText(level + " / 7");
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
