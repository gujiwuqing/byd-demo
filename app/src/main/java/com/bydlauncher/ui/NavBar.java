package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;
import com.bydlauncher.api.BydAcApi;

public class NavBar {

    public interface TabListener {
        void onTabSelected(int tabIndex);
    }

    private final View rootView;
    private final View[] tabs;
    private final TextView acTemp;
    private final TextView acWindLevel;
    private int currentTab = 0;
    private TabListener listener;

    private int currentAcTemp = 25;

    public NavBar(View rootView, BydAcApi acApi) {
        this.rootView = rootView;

        tabs = new View[]{
                rootView.findViewById(R.id.tab_status),
                rootView.findViewById(R.id.tab_map),
                rootView.findViewById(R.id.tab_controls),
                rootView.findViewById(R.id.tab_apps),
                rootView.findViewById(R.id.tab_settings)
        };

        acTemp = rootView.findViewById(R.id.nav_ac_temp);
        acWindLevel = rootView.findViewById(R.id.nav_ac_wind_level);

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> selectTab(index));
        }

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
            rootView.findViewById(R.id.nav_ac_wind_icon).setVisibility(View.VISIBLE);
            acWindLevel.setVisibility(View.VISIBLE);
        } else {
            acTemp.setText(rootView.getContext().getString(R.string.ac_off_label));
            rootView.findViewById(R.id.nav_ac_wind_icon).setVisibility(View.GONE);
            acWindLevel.setVisibility(View.GONE);
        }
    }
}
