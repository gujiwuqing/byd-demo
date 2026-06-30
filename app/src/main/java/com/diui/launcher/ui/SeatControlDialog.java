package com.diui.launcher.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.diui.launcher.R;

public class SeatControlDialog {

    private static final int OFF = 0;
    private static final int LEVEL_1 = 1;
    private static final int LEVEL_2 = 2;

    private final Context context;
    private Dialog dialog;

    // 主驾状态
    private int driverVent = OFF;
    private int driverHeat = OFF;
    // 副驾状态
    private int passVent = OFF;
    private int passHeat = OFF;

    // 主驾通风按钮
    private View driverVentOff, driverVent1, driverVent2;
    private TextView driverVentStatus;
    // 主驾加热按钮
    private View driverHeatOff, driverHeat1, driverHeat2;
    private TextView driverHeatStatus;
    // 副驾通风按钮
    private View passVentOff, passVent1, passVent2;
    private TextView passVentStatus;
    // 副驾加热按钮
    private View passHeatOff, passHeat1, passHeat2;
    private TextView passHeatStatus;

    public SeatControlDialog(Context context) {
        this.context = context;
    }

    public void show() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_seat_control, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.45f);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.62),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        // 获取所有 View
        driverVentStatus = view.findViewById(R.id.seat_driver_vent_status);
        driverVentOff = view.findViewById(R.id.seat_driver_vent_off);
        driverVent1 = view.findViewById(R.id.seat_driver_vent_1);
        driverVent2 = view.findViewById(R.id.seat_driver_vent_2);

        driverHeatStatus = view.findViewById(R.id.seat_driver_heat_status);
        driverHeatOff = view.findViewById(R.id.seat_driver_heat_off);
        driverHeat1 = view.findViewById(R.id.seat_driver_heat_1);
        driverHeat2 = view.findViewById(R.id.seat_driver_heat_2);

        passVentStatus = view.findViewById(R.id.seat_pass_vent_status);
        passVentOff = view.findViewById(R.id.seat_pass_vent_off);
        passVent1 = view.findViewById(R.id.seat_pass_vent_1);
        passVent2 = view.findViewById(R.id.seat_pass_vent_2);

        passHeatStatus = view.findViewById(R.id.seat_pass_heat_status);
        passHeatOff = view.findViewById(R.id.seat_pass_heat_off);
        passHeat1 = view.findViewById(R.id.seat_pass_heat_1);
        passHeat2 = view.findViewById(R.id.seat_pass_heat_2);

        // 主驾通风
        driverVentOff.setOnClickListener(v -> { driverVent = OFF; refreshDriverVent(); });
        driverVent1.setOnClickListener(v -> { driverVent = LEVEL_1; refreshDriverVent(); });
        driverVent2.setOnClickListener(v -> { driverVent = LEVEL_2; refreshDriverVent(); });

        // 主驾加热
        driverHeatOff.setOnClickListener(v -> { driverHeat = OFF; refreshDriverHeat(); });
        driverHeat1.setOnClickListener(v -> { driverHeat = LEVEL_1; refreshDriverHeat(); });
        driverHeat2.setOnClickListener(v -> { driverHeat = LEVEL_2; refreshDriverHeat(); });

        // 副驾通风
        passVentOff.setOnClickListener(v -> { passVent = OFF; refreshPassVent(); });
        passVent1.setOnClickListener(v -> { passVent = LEVEL_1; refreshPassVent(); });
        passVent2.setOnClickListener(v -> { passVent = LEVEL_2; refreshPassVent(); });

        // 副驾加热
        passHeatOff.setOnClickListener(v -> { passHeat = OFF; refreshPassHeat(); });
        passHeat1.setOnClickListener(v -> { passHeat = LEVEL_1; refreshPassHeat(); });
        passHeat2.setOnClickListener(v -> { passHeat = LEVEL_2; refreshPassHeat(); });

        view.findViewById(R.id.seat_close_dialog).setOnClickListener(v -> dialog.dismiss());

        refreshAll();
        dialog.show();
    }

    private void refreshAll() {
        refreshDriverVent();
        refreshDriverHeat();
        refreshPassVent();
        refreshPassHeat();
    }

    private void refreshDriverVent() {
        driverVentStatus.setText(levelText(driverVent, false));
        driverVentStatus.setTextColor(levelColor(driverVent));
        applyGroup(driverVent, driverVentOff, driverVent1, driverVent2);
    }

    private void refreshDriverHeat() {
        driverHeatStatus.setText(levelText(driverHeat, true));
        driverHeatStatus.setTextColor(levelColor(driverHeat));
        applyGroup(driverHeat, driverHeatOff, driverHeat1, driverHeat2);
    }

    private void refreshPassVent() {
        passVentStatus.setText(levelText(passVent, false));
        passVentStatus.setTextColor(levelColor(passVent));
        applyGroup(passVent, passVentOff, passVent1, passVent2);
    }

    private void refreshPassHeat() {
        passHeatStatus.setText(levelText(passHeat, true));
        passHeatStatus.setTextColor(levelColor(passHeat));
        applyGroup(passHeat, passHeatOff, passHeat1, passHeat2);
    }

    private void applyGroup(int level, View off, View lvl1, View lvl2) {
        setActive(off, level == OFF);
        setActive(lvl1, level == LEVEL_1);
        setActive(lvl2, level == LEVEL_2);
    }

    private void setActive(View v, boolean active) {
        v.setBackground(ContextCompat.getDrawable(context,
                active ? R.drawable.bg_seat_btn_selected : R.drawable.bg_seat_btn));
    }

    private String levelText(int level, boolean isHeat) {
        if (level == OFF) return "已关闭";
        if (level == LEVEL_1) return isHeat ? "1挡·暖" : "1挡·低";
        return isHeat ? "2挡·热" : "2挡·高";
    }

    private int levelColor(int level) {
        if (level == OFF) return ContextCompat.getColor(context, R.color.text_tertiary);
        if (level == LEVEL_1) return ContextCompat.getColor(context, R.color.status_fair);
        return ContextCompat.getColor(context, R.color.status_poor);
    }
}
