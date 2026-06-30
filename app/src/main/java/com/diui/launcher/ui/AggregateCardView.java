package com.diui.launcher.ui;

import android.view.View;
import android.widget.TextView;

import com.diui.launcher.R;
import com.diui.launcher.model.VehicleStatus;

public class AggregateCardView {

    private final View rootView;
    private final TextView speed, gear, battery, evRange, fuel, fuelAmount;
    private final TextView tireFL, tireFR, tireRL, tireRR;
    private final TextView outsideTemp, power;

    public AggregateCardView(View rootView) {
        this.rootView = rootView;
        speed = rootView.findViewById(R.id.agg_speed);
        gear = rootView.findViewById(R.id.agg_gear);
        battery = rootView.findViewById(R.id.agg_battery);
        evRange = rootView.findViewById(R.id.agg_ev_range);
        fuel = rootView.findViewById(R.id.agg_fuel);
        fuelAmount = rootView.findViewById(R.id.agg_fuel_amount);
        tireFL = rootView.findViewById(R.id.agg_tire_fl);
        tireFR = rootView.findViewById(R.id.agg_tire_fr);
        tireRL = rootView.findViewById(R.id.agg_tire_rl);
        tireRR = rootView.findViewById(R.id.agg_tire_rr);
        outsideTemp = rootView.findViewById(R.id.agg_outside_temp);
        power = rootView.findViewById(R.id.agg_power);
    }

    public void updateStatus(VehicleStatus s) {
        speed.setText(s.getSpeedText());
        gear.setText(s.getGearText());
        battery.setText(s.getBatteryText());
        evRange.setText(s.getEvMileageText());
        if (s.fuelPercent >= 0) fuel.setText(s.fuelPercent + "%");
        fuelAmount.setText(s.getFuelText());

        if (s.tirePressureFL >= 0) {
            tireFL.setText("FL " + s.tirePressureFL);
            tireFR.setText("FR " + s.tirePressureFR);
            tireRL.setText("RL " + s.tirePressureRL);
            tireRR.setText("RR " + s.tirePressureRR);
        }

        outsideTemp.setText(s.getOutsideTempText());
        power.setText(s.getPowerKwText());
    }

    public View getView() { return rootView; }
}
