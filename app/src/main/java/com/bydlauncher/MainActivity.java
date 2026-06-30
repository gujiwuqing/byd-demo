package com.bydlauncher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.bydlauncher.api.BydApiExplorer;
import com.bydlauncher.api.BydPermissionHelper;
import com.bydlauncher.api.BydVehicleManager;
import com.bydlauncher.model.VehicleStatus;
import com.bydlauncher.theme.ThemeManager;
import com.bydlauncher.ui.AppsPage;
import com.bydlauncher.ui.ControlsPage;
import com.bydlauncher.ui.NavBar;
import com.bydlauncher.ui.SettingsPage;
import com.bydlauncher.ui.StatusPage;
import com.bydlauncher.ui.TopBar;
import com.bydlauncher.ui.WindowPanelController;

public class MainActivity extends AppCompatActivity
        implements BydVehicleManager.VehicleStatusListener, NavBar.TabListener {

    private static final String PREFS_NAME = "byd_launcher_prefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

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

        // 权限诊断：检查 BYD 权限授权状态，输出到 Logcat（过滤 "BydPermissionHelper"）
        BydPermissionHelper.diagnosePermissions(this);

        // 在真实车机上扫描可用 API，输出到 Logcat（过滤 "BydApiExplorer"）
        BydApiExplorer.exploreAll();

        View pageStatusView = findViewById(R.id.page_status);
        View pageControlsView = findViewById(R.id.page_controls);
        View pageAppsView = findViewById(R.id.page_apps);
        View pageSettingsView = findViewById(R.id.page_settings);

        // 4 个标签页：主页/控制/应用/设置
        pages = new View[]{pageStatusView, pageControlsView, pageAppsView, pageSettingsView};

        topBar = new TopBar(findViewById(R.id.top_bar));
        navBar = new NavBar(findViewById(R.id.nav_bar), vehicleManager.getAcApi(), vehicleManager.getBodyworkApi());
        navBar.setTabListener(this);

        // 注入 Activity 内嵌遮罩和面板
        navBar.setPanels(
                findViewById(R.id.overlay_mask),
                findViewById(R.id.panel_window),
                findViewById(R.id.panel_seat)
        );

        statusPage = new StatusPage(pageStatusView);
        controlsPage = new ControlsPage(pageControlsView, vehicleManager.getAcApi());
        appsPage = new AppsPage(pageAppsView);

        // 模拟模式判断：核心 API（AC、车身、统计）中任一不可用即视为模拟模式
        boolean isSimulation = !vehicleManager.getAcApi().isRealDevice()
                || !vehicleManager.getDriveApi().isRealDevice()
                || !vehicleManager.getTireApi().isRealDevice();
        settingsPage = new SettingsPage(pageSettingsView, isSimulation);

        // 权限检查
        checkPermissions();
    }

    private void checkPermissions() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean firstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);

        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog(firstLaunch);
        }

        // 检查 BYD 车辆 API 权限（仅在真实车机上检查）
        boolean hasAnyRealApi = vehicleManager.getAcApi().isRealDevice()
                || vehicleManager.getBodyworkApi().isAvailable()
                || vehicleManager.getStatisticApi().isAvailable();
        if (hasAnyRealApi) {
            boolean hasMissingApi = !vehicleManager.getAcApi().isRealDevice()
                    || !vehicleManager.getDriveApi().isRealDevice()
                    || !vehicleManager.getTireApi().isRealDevice();
            if (hasMissingApi) {
                showBydPermissionDialog();
            }
        }

        // 检查是否为默认桌面
        checkDefaultLauncher();

        // 标记非首次启动
        if (firstLaunch) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        }
    }

    private void checkDefaultLauncher() {
        if (!isDefaultLauncher()) {
            showDefaultLauncherDialog();
        }
    }

    private boolean isDefaultLauncher() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    private void showDefaultLauncherDialog() {
        showDimDialog(new MaterialAlertDialogBuilder(this, R.style.AppAlertDialog)
                .setTitle(R.string.perm_launcher_title)
                .setMessage(R.string.perm_launcher_message)
                .setPositiveButton(R.string.perm_btn_settings, (d, w) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        // 忽略
                    }
                })
                .setNegativeButton(R.string.perm_btn_cancel, null));
    }

    private void showOverlayPermissionDialog(boolean firstLaunch) {
        if (!firstLaunch) {
            // 非首次安装，静默跳过
            return;
        }
        showDimDialog(new MaterialAlertDialogBuilder(this, R.style.AppAlertDialog)
                .setTitle(R.string.perm_overlay_title)
                .setMessage(R.string.perm_overlay_message)
                .setPositiveButton(R.string.perm_btn_ok, (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton(R.string.perm_btn_cancel, null)
                .setCancelable(false));
    }

    private void showBydPermissionDialog() {
        showDimDialog(new MaterialAlertDialogBuilder(this, R.style.AppAlertDialog)
                .setTitle(R.string.perm_byd_title)
                .setMessage(R.string.perm_byd_message)
                .setPositiveButton(R.string.perm_btn_settings, (d, w) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        // 忽略
                    }
                })
                .setNegativeButton(R.string.perm_btn_cancel, null));
    }

    /**
     * 显示带半透明遮罩的对话框
     */
    private void showDimDialog(MaterialAlertDialogBuilder builder) {
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.6f);
        }
        dialog.show();
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
            WindowPanelController.updateFromStatus(
                    status.windowFL, status.windowFR, status.windowRL, status.windowRR,
                    status.trunkOpen, status.hoodOpen,
                    status.doorLeftFrontOpen, status.doorRightFrontOpen,
                    status.doorLeftRearOpen, status.doorRightRearOpen);
        });
    }

    @Override
    public void onBackPressed() {
        if (currentTab != 0) {
            navBar.selectTab(0);
        }
    }
}
