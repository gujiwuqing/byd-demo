package com.diui.launcher.ui;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.diui.launcher.R;
import com.diui.launcher.api.BydAcApi;
import com.diui.launcher.api.BydBodyworkApi;

public class NavBar {

    public interface TabListener {
        void onTabSelected(int tabIndex);
    }

    private final View rootView;
    private final View[] tabs;
    private final TextView acTemp, acWindLevel, acPower;
    private final TextView cycleText;
    private final BydBodyworkApi bodyworkApi;
    private TabListener listener;
    private int currentTab = 0;
    private int currentAcTemp = 25;

    // 面板引用（Activity 视图层级中的内嵌面板）
    private View overlayMask;
    private View panelWindow;
    private View panelSeat;

    public NavBar(View rootView, BydAcApi acApi, BydBodyworkApi bodyworkApi) {
        this.rootView = rootView;
        this.bodyworkApi = bodyworkApi;

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

        acPower.setOnClickListener(v -> acApi.toggle());
        rootView.findViewById(R.id.nav_btn_cycle).setOnClickListener(v -> acApi.toggleCycleMode());

        rootView.findViewById(R.id.nav_btn_window).setOnClickListener(v -> showPanel(true));
        rootView.findViewById(R.id.nav_btn_seat).setOnClickListener(v -> showPanel(false));

        selectTab(0);
    }

    /** 由 MainActivity 在 setContentView 后注入面板引用 */
    public void setPanels(View mask, View window, View seat) {
        this.overlayMask = mask;
        this.panelWindow = window;
        this.panelSeat = seat;

        // 设置面板宽度为屏幕的 62%
        int panelWidth = (int) (mask.getContext().getResources().getDisplayMetrics().widthPixels * 0.62f);
        android.view.ViewGroup.LayoutParams wpLp = window.getLayoutParams();
        wpLp.width = panelWidth;
        window.setLayoutParams(wpLp);
        android.view.ViewGroup.LayoutParams spLp = seat.getLayoutParams();
        spLp.width = panelWidth;
        seat.setLayoutParams(spLp);

        // 点遮罩关闭所有面板
        mask.setOnClickListener(v -> closeAllPanels());

        // 绑定面板内的关闭按钮
        View winClose = window.findViewById(R.id.win_close_dialog);
        if (winClose != null) winClose.setOnClickListener(v -> closeAllPanels());

        View seatClose = seat.findViewById(R.id.seat_close_dialog);
        if (seatClose != null) seatClose.setOnClickListener(v -> closeAllPanels());

        // 初始化车窗面板逻辑
        WindowPanelController.bind(window, bodyworkApi);

        // 初始化座椅面板逻辑
        SeatPanelController.bind(seat);
    }

    private void showPanel(boolean isWindow) {
        if (overlayMask == null) return;
        overlayMask.setVisibility(View.VISIBLE);
        panelWindow.setVisibility(isWindow ? View.VISIBLE : View.GONE);
        panelSeat.setVisibility(isWindow ? View.GONE : View.VISIBLE);
    }

    public void closeAllPanels() {
        if (overlayMask == null) return;
        overlayMask.setVisibility(View.GONE);
        panelWindow.setVisibility(View.GONE);
        panelSeat.setVisibility(View.GONE);
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
