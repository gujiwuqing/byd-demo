package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;

public class SeatPanelController {

    private static final int OFF = 0;
    private static final int LEVEL_1 = 1;
    private static final int LEVEL_2 = 2;

    private static int driverVent = OFF;
    private static int driverHeat = OFF;
    private static int passVent = OFF;
    private static int passHeat = OFF;

    public static void bind(View panel) {
        TextView driverVentStatus = panel.findViewById(R.id.seat_driver_vent_status);
        View driverVentOff = panel.findViewById(R.id.seat_driver_vent_off);
        View driverVent1   = panel.findViewById(R.id.seat_driver_vent_1);
        View driverVent2   = panel.findViewById(R.id.seat_driver_vent_2);

        TextView driverHeatStatus = panel.findViewById(R.id.seat_driver_heat_status);
        View driverHeatOff = panel.findViewById(R.id.seat_driver_heat_off);
        View driverHeat1   = panel.findViewById(R.id.seat_driver_heat_1);
        View driverHeat2   = panel.findViewById(R.id.seat_driver_heat_2);

        TextView passVentStatus = panel.findViewById(R.id.seat_pass_vent_status);
        View passVentOff = panel.findViewById(R.id.seat_pass_vent_off);
        View passVent1   = panel.findViewById(R.id.seat_pass_vent_1);
        View passVent2   = panel.findViewById(R.id.seat_pass_vent_2);

        TextView passHeatStatus = panel.findViewById(R.id.seat_pass_heat_status);
        View passHeatOff = panel.findViewById(R.id.seat_pass_heat_off);
        View passHeat1   = panel.findViewById(R.id.seat_pass_heat_1);
        View passHeat2   = panel.findViewById(R.id.seat_pass_heat_2);

        driverVentOff.setOnClickListener(v -> { driverVent = OFF;     refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });
        driverVent1.setOnClickListener(v  -> { driverVent = LEVEL_1;  refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });
        driverVent2.setOnClickListener(v  -> { driverVent = LEVEL_2;  refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });

        driverHeatOff.setOnClickListener(v -> { driverHeat = OFF;     refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });
        driverHeat1.setOnClickListener(v  -> { driverHeat = LEVEL_1;  refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });
        driverHeat2.setOnClickListener(v  -> { driverHeat = LEVEL_2;  refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });

        passVentOff.setOnClickListener(v  -> { passVent = OFF;        refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });
        passVent1.setOnClickListener(v    -> { passVent = LEVEL_1;    refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });
        passVent2.setOnClickListener(v    -> { passVent = LEVEL_2;    refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });

        passHeatOff.setOnClickListener(v  -> { passHeat = OFF;        refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });
        passHeat1.setOnClickListener(v    -> { passHeat = LEVEL_1;    refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });
        passHeat2.setOnClickListener(v    -> { passHeat = LEVEL_2;    refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2); });

        refreshAll(panel, driverVentStatus, driverVentOff, driverVent1, driverVent2, driverHeatStatus, driverHeatOff, driverHeat1, driverHeat2, passVentStatus, passVentOff, passVent1, passVent2, passHeatStatus, passHeatOff, passHeat1, passHeat2);
    }

    private static void refreshAll(View ctx,
            TextView dvStat, View dvOff, View dv1, View dv2,
            TextView dhStat, View dhOff, View dh1, View dh2,
            TextView pvStat, View pvOff, View pv1, View pv2,
            TextView phStat, View phOff, View ph1, View ph2) {
        refreshGroup(ctx, dvStat, dvOff, dv1, dv2, driverVent, false);
        refreshGroup(ctx, dhStat, dhOff, dh1, dh2, driverHeat, true);
        refreshGroup(ctx, pvStat, pvOff, pv1, pv2, passVent, false);
        refreshGroup(ctx, phStat, phOff, ph1, ph2, passHeat, true);
    }

    private static void refreshGroup(View ctx, TextView status, View off, View l1, View l2, int level, boolean isHeat) {
        String[] texts = isHeat
                ? new String[]{"已关闭", "1挡·暖", "2挡·热"}
                : new String[]{"已关闭", "1挡·低", "2挡·高"};
        int[] colorRes = {R.color.text_tertiary, R.color.status_fair, R.color.status_poor};

        status.setText(texts[level]);
        status.setTextColor(ContextCompat.getColor(ctx.getContext(), colorRes[level]));

        setBtn(ctx, off, level == OFF);
        setBtn(ctx, l1,  level == LEVEL_1);
        setBtn(ctx, l2,  level == LEVEL_2);
    }

    private static void setBtn(View ctx, View btn, boolean active) {
        btn.setBackground(ContextCompat.getDrawable(ctx.getContext(),
                active ? R.drawable.bg_seat_btn_selected : R.drawable.bg_seat_btn));
    }
}
