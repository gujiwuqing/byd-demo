package com.diui.launcher.ui;

import android.view.View;
import android.widget.TextView;

import com.diui.launcher.R;
import com.diui.launcher.model.VehicleStatus;

public class TopBar {

    private final TextView tvBattery, tvEvRange, tvFuel, tvFuelAmount, tvTotalRange;

    public TopBar(View rootView) {
        tvBattery = rootView.findViewById(R.id.top_battery);
        tvEvRange = rootView.findViewById(R.id.top_ev_range);
        tvFuel = rootView.findViewById(R.id.top_fuel);
        tvFuelAmount = rootView.findViewById(R.id.top_fuel_amount);
        tvTotalRange = rootView.findViewById(R.id.top_total_range);
    }

    public void update(VehicleStatus s) {
        if (s == null) return;
        if (tvBattery != null) tvBattery.setText(s.getBatteryText());
        if (tvEvRange != null) tvEvRange.setText(s.getEvMileageText());
        if (tvFuel != null) tvFuel.setText(s.fuelPercent >= 0 ? s.fuelPercent + "%" : "N/A");
        if (tvFuelAmount != null) tvFuelAmount.setText(s.getFuelText());
        if (tvTotalRange != null) tvTotalRange.setText(s.getTotalRangeText());
    }
}
