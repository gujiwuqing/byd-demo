package com.bydlauncher;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.bydlauncher.api.BydVehicleManager;
import com.bydlauncher.model.VehicleStatus;
import com.bydlauncher.theme.ThemeManager;
import com.bydlauncher.ui.AppsPage;
import com.bydlauncher.ui.ControlsPage;
import com.bydlauncher.ui.NavBar;
import com.bydlauncher.ui.SettingsPage;
import com.bydlauncher.ui.StatusPage;
import com.bydlauncher.ui.TopBar;

public class MainActivity extends AppCompatActivity
        implements BydVehicleManager.VehicleStatusListener, NavBar.TabListener {

    private BydVehicleManager vehicleManager;

    private TopBar topBar;
    private NavBar navBar;
    private StatusPage statusPage;
    private ControlsPage controlsPage;
    private AppsPage appsPage;
    private SettingsPage settingsPage;

    private View[] pages;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();
        setContentView(R.layout.activity_main);

        vehicleManager = BydVehicleManager.getInstance(this);
        vehicleManager.setListener(this);

        View pageStatusView = findViewById(R.id.page_status);
        View pageControlsView = findViewById(R.id.page_controls);
        View pageAppsView = findViewById(R.id.page_apps);
        View pageSettingsView = findViewById(R.id.page_settings);

        // 4 个标签页：主页/控制/应用/设置
        pages = new View[]{pageStatusView, pageControlsView, pageAppsView, pageSettingsView};

        topBar = new TopBar(findViewById(R.id.top_bar));
        navBar = new NavBar(findViewById(R.id.nav_bar), vehicleManager.getAcApi(), vehicleManager.getBodyworkApi());
        navBar.setTabListener(this);

        statusPage = new StatusPage(pageStatusView);
        controlsPage = new ControlsPage(pageControlsView, vehicleManager.getAcApi(), vehicleManager.getBodyworkApi());
        appsPage = new AppsPage(pageAppsView);

        boolean isSimulation = !vehicleManager.getAcApi().isRealDevice();
        settingsPage = new SettingsPage(pageSettingsView, isSimulation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        vehicleManager.startPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        vehicleManager.stopPolling();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onTabSelected(int tabIndex) {
        currentTab = tabIndex;
        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisibility(i == tabIndex ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onStatusUpdated(VehicleStatus status) {
        runOnUiThread(() -> {
            statusPage.updateStatus(status);
            controlsPage.updateStatus(status);
            navBar.updateAcState(status.acOn, status.acTemp, status.acWindLevel);
            navBar.updateCycleState(status.acCycleMode);
            topBar.updateMediaSource("");
        });
    }

    @Override
    public void onBackPressed() {
        if (currentTab != 0) {
            navBar.selectTab(0);
        }
    }
}
