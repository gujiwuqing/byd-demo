package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;
import com.bydlauncher.api.BydAcApi;
import com.bydlauncher.api.BydBodyworkApi;

public class NavBar {

    public interface TabListener {
        void onTabSelected(int tabIndex);
    }

    private final View rootView;
    private final View[] tabs;
    private final TextView acTemp, acWindLevel, acPower;
    private final TextView cycleText;
    private TabListener listener;
    private int currentTab = 0;
    private int currentAcTemp = 25;

    public NavBar(View rootView, BydAcApi acApi, BydBodyworkApi bodyworkApi) {
        this.rootView = rootView;

        // 4 个标签页（主页/控制/应用/设置）
        tabs = new View[]{
                rootView.findViewById(R.id.tab_status),
                rootView.findViewById(R.id.tab_controls),
                rootView.findViewById(R.id.tab_apps),
                rootView.findViewById(R.id.tab_settings)
        };

        acTemp = rootView.findViewById(R.id.nav_ac_temp);
        acWindLevel = rootView.findViewById(R.id.nav_ac_wind_level);
        acPower = rootView.findViewById(R.id.nav_ac_power);
        cycleText = rootView.findViewById(R.id.nav_cycle_text);

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> selectTab(index));
        }

        // AC 温度控制
        rootView.findViewById(R.id.nav_ac_down).setOnClickListener(v -> {
            currentAcTemp = Math.max(17, currentAcTemp - 1);
            acApi.setMainTemp(currentAcTemp);
            acTemp.setText(currentAcTemp + "°");
        });
        rootView.findViewById(R.id.nav_ac_up).setOnClickListener(v -> {
            currentAcTemp = Math.min(33, currentAcTemp + 1);
            acApi.setMainTemp(currentAcTemp);
            acTemp.setText(currentAcTemp + "°");
        });

        // AC 开关
        acPower.setOnClickListener(v -> acApi.toggle());

        // 快捷控制按钮
        rootView.findViewById(R.id.nav_btn_cycle).setOnClickListener(v -> acApi.toggleCycleMode());

        // 车窗控制弹窗
        WindowControlDialog windowDialog = new WindowControlDialog(rootView.getContext(), bodyworkApi);
        rootView.findViewById(R.id.nav_btn_window).setOnClickListener(v -> windowDialog.show());

        selectTab(0);
    }

    public void setTabListener(TabListener listener) {
        this.listener = listener;
    }

    public void selectTab(int index) {
        currentTab = index;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setSelected(i == index);
        }
        if (listener != null) {
            listener.onTabSelected(index);
        }
    }

    public void updateAcState(boolean acOn, int temp, int windLevel) {
        if (acOn) {
            currentAcTemp = temp;
            acTemp.setText(temp + "°");
            acWindLevel.setText(String.valueOf(windLevel));
            acPower.setTextColor(ContextCompat.getColor(rootView.getContext(), R.color.accent));
            rootView.findViewById(R.id.nav_ac_wind_icon).setVisibility(View.VISIBLE);
            acWindLevel.setVisibility(View.VISIBLE);
        } else {
            acTemp.setText("OFF");
            acPower.setTextColor(ContextCompat.getColor(rootView.getContext(), R.color.text_tertiary));
            rootView.findViewById(R.id.nav_ac_wind_icon).setVisibility(View.GONE);
            acWindLevel.setVisibility(View.GONE);
        }
    }

    public void updateCycleState(int mode) {
        cycleText.setText(mode == 0 ? "内循环" : "外循环");
    }
}
