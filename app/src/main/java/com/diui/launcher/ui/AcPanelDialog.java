package com.diui.launcher.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.diui.launcher.R;
import com.diui.launcher.api.BydAcApi;
import com.diui.launcher.model.VehicleStatus;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AcPanelDialog {

    public static void show(Context context, BydAcApi acApi, VehicleStatus status) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_ac_panel, null);

        ArcTempView arcTemp = view.findViewById(R.id.ac_arc_temp);
        int initTemp = (status != null && status.acTemp > 0) ? status.acTemp : acApi.getMainTemp();
        boolean initOn = (status != null) ? status.acOn : acApi.isOn();
        arcTemp.setTemp(initTemp);
        arcTemp.setAcOn(initOn);

        final int[] currentTemp = {initTemp};
        final boolean[] currentOn = {initOn};

        TextView toggleBtn = view.findViewById(R.id.ac_toggle_btn);
        updateToggleBtn(toggleBtn, currentOn[0], context);

        toggleBtn.setOnClickListener(v -> {
            acApi.toggle();
            currentOn[0] = !currentOn[0];
            arcTemp.setAcOn(currentOn[0]);
            updateToggleBtn(toggleBtn, currentOn[0], context);
        });

        view.findViewById(R.id.ac_temp_down).setOnClickListener(v -> {
            if (currentTemp[0] > 17) {
                currentTemp[0]--;
                acApi.setMainTemp(currentTemp[0]);
                arcTemp.setTemp(currentTemp[0]);
            }
        });
        view.findViewById(R.id.ac_temp_up).setOnClickListener(v -> {
            if (currentTemp[0] < 33) {
                currentTemp[0]++;
                acApi.setMainTemp(currentTemp[0]);
                arcTemp.setTemp(currentTemp[0]);
            }
        });

        LinearLayout windRow = view.findViewById(R.id.ac_wind_row);
        TextView windLabel = view.findViewById(R.id.ac_wind_label);
        int initWind = (status != null) ? status.acWindLevel : acApi.getWindLevel();
        buildWindButtons(context, windRow, initWind, windLabel, acApi);

        TextView modeFace = view.findViewById(R.id.ac_mode_face);
        TextView modeFoot = view.findViewById(R.id.ac_mode_foot);
        TextView modeDefrost = view.findViewById(R.id.ac_mode_defrost);
        int initWindMode = (status != null) ? status.acWindMode : acApi.getWindMode();
        highlightWindMode(modeFace, modeFoot, modeDefrost, initWindMode, context);

        modeFace.setOnClickListener(v -> {
            acApi.setWindMode(BydAcApi.WIND_MODE_FACE);
            highlightWindMode(modeFace, modeFoot, modeDefrost, BydAcApi.WIND_MODE_FACE, context);
        });
        modeFoot.setOnClickListener(v -> {
            acApi.setWindMode(BydAcApi.WIND_MODE_FOOT);
            highlightWindMode(modeFace, modeFoot, modeDefrost, BydAcApi.WIND_MODE_FOOT, context);
        });
        modeDefrost.setOnClickListener(v -> {
            acApi.setWindMode(BydAcApi.WIND_MODE_DEFROST);
            highlightWindMode(modeFace, modeFoot, modeDefrost, BydAcApi.WIND_MODE_DEFROST, context);
        });

        TextView cycleInner = view.findViewById(R.id.ac_cycle_inner);
        TextView cycleOuter = view.findViewById(R.id.ac_cycle_outer);
        int initCycle = (status != null) ? status.acCycleMode : acApi.getCycleMode();
        highlightTwo(cycleInner, cycleOuter, initCycle == BydAcApi.CYCLE_INNER, context);

        cycleInner.setOnClickListener(v -> {
            acApi.setCycleMode(BydAcApi.CYCLE_INNER);
            highlightTwo(cycleInner, cycleOuter, true, context);
        });
        cycleOuter.setOnClickListener(v -> {
            acApi.setCycleMode(BydAcApi.CYCLE_OUTER);
            highlightTwo(cycleInner, cycleOuter, false, context);
        });

        TextView ctrlAuto = view.findViewById(R.id.ac_ctrl_auto);
        TextView ctrlManual = view.findViewById(R.id.ac_ctrl_manual);
        int initMode = (status != null) ? status.acControlMode : acApi.getControlMode();
        highlightTwo(ctrlAuto, ctrlManual, initMode == BydAcApi.MODE_AUTO, context);

        ctrlAuto.setOnClickListener(v -> {
            acApi.setControlMode(BydAcApi.MODE_AUTO);
            highlightTwo(ctrlAuto, ctrlManual, true, context);
        });
        ctrlManual.setOnClickListener(v -> {
            acApi.setControlMode(BydAcApi.MODE_MANUAL);
            highlightTwo(ctrlAuto, ctrlManual, false, context);
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.AppAlertDialog)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.6f);

            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int dialogWidth = (int)(dm.widthPixels * 0.65);
            dialog.getWindow().setLayout(dialogWidth,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        view.findViewById(R.id.dialog_ac_close).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static void updateToggleBtn(TextView btn, boolean on, Context ctx) {
        if (on) {
            btn.setText("❄  已开启");
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.accent));
            btn.setSelected(true);
        } else {
            btn.setText("○  已关闭");
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.text_tertiary));
            btn.setSelected(false);
        }
    }

    private static void buildWindButtons(Context ctx, LinearLayout row, int initLevel,
                                          TextView label, BydAcApi acApi) {
        int count = 8;
        TextView[] buttons = new TextView[count];
        float density = ctx.getResources().getDisplayMetrics().density;

        for (int i = 0; i < count; i++) {
            TextView btn = new TextView(ctx);
            btn.setGravity(Gravity.CENTER);
            btn.setText(String.valueOf(i));
            btn.setTextSize(12);
            btn.setBackgroundResource(R.drawable.bg_switch_btn);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    (int)(36 * density), 1f);
            lp.setMargins((int)(2 * density), 0, (int)(2 * density), 0);
            btn.setLayoutParams(lp);

            updateBtnHighlight(btn, i == initLevel, ctx);
            buttons[i] = btn;

            final int level = i;
            btn.setOnClickListener(v -> {
                acApi.setWindLevel(level);
                for (int j = 0; j < count; j++) updateBtnHighlight(buttons[j], j == level, ctx);
                label.setText(level == 0 ? "自动" : level + "级");
            });
            row.addView(btn);
        }
        label.setText(initLevel == 0 ? "自动" : initLevel + "级");
    }

    private static void updateBtnHighlight(TextView btn, boolean selected, Context ctx) {
        btn.setSelected(selected);
        btn.setTextColor(selected
                ? ContextCompat.getColor(ctx, R.color.accent)
                : ContextCompat.getColor(ctx, R.color.text_secondary));
    }

    private static void highlightWindMode(TextView face, TextView foot, TextView defrost,
                                           int mode, Context ctx) {
        updateBtnHighlight(face, mode == BydAcApi.WIND_MODE_FACE, ctx);
        updateBtnHighlight(foot, mode == BydAcApi.WIND_MODE_FOOT, ctx);
        updateBtnHighlight(defrost, mode == BydAcApi.WIND_MODE_DEFROST, ctx);
    }

    private static void highlightTwo(TextView a, TextView b, boolean aSelected, Context ctx) {
        updateBtnHighlight(a, aSelected, ctx);
        updateBtnHighlight(b, !aSelected, ctx);
    }
}
