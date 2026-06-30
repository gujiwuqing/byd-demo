package com.bydlauncher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.bydlauncher.api.AdbHelper;
import com.bydlauncher.api.BydApiExplorer;
import com.bydlauncher.api.BydEnvironmentDetector;
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
import com.bydlauncher.ui.UnboundedPage;
import com.bydlauncher.ui.AppSlotManager;
import com.bydlauncher.ui.WindowPanelController;

public class MainActivity extends AppCompatActivity
        implements BydVehicleManager.VehicleStatusListener, NavBar.TabListener {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "byd_launcher_prefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_ADB_GRANTED = "adb_granted";
    private static final long[] RETRY_DELAYS = {2000, 3000, 5000, 8000, 10000};

    private BydVehicleManager vehicleManager;

    private TopBar topBar;
    private NavBar navBar;
    private StatusPage statusPage;
    private ControlsPage controlsPage;
    private AppsPage appsPage;
    private SettingsPage settingsPage;

    private View[] pages;
    private int currentTab = 0;
    private View standardContainer;
    private View unboundedContainer;
    private UnboundedPage unboundedPage;
    private AppSlotManager appSlotManager;
    private boolean isUnboundedMode = false;
    private boolean temporaryStandardVisit = false;
    private BydEnvironmentDetector.Environment detectedEnv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();
        setContentView(R.layout.activity_main);

        standardContainer = findViewById(R.id.standard_container);
        unboundedContainer = findViewById(R.id.page_unbounded);
        appSlotManager = new AppSlotManager(this);

        // 自动检测车机环境（用户手动设置优先）
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasManualOverride = prefs.contains("sim_mode_manual");

        if (hasManualOverride) {
            boolean forceSim = prefs.getBoolean("sim_mode_manual", true);
            BydVehicleManager.setForceSimulation(forceSim);
            this.detectedEnv = forceSim ? BydEnvironmentDetector.Environment.SIMULATOR
                    : BydEnvironmentDetector.Environment.REAL_DEVICE;
            Log.i(TAG, "Using manual override: simulation=" + forceSim);
        } else {
            this.detectedEnv = BydVehicleManager.detectAndConfigure(this);
        }

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
        statusPage.setAppSlotManager(appSlotManager);
        controlsPage = new ControlsPage(pageControlsView, vehicleManager.getAcApi());
        appsPage = new AppsPage(pageAppsView);

        // 模拟模式使用用户手动设置的值（默认开启）
        boolean isSimulation = BydVehicleManager.isForceSimulation();
        settingsPage = new SettingsPage(pageSettingsView, isSimulation);
        settingsPage.setOnAdbAuthorizeListener(() -> {
            // 重置已授权标记，重新触发授权流程
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putBoolean(KEY_ADB_GRANTED, false).apply();
            startAdbGrant();
        });
        settingsPage.setOnSimModeChangedListener(this::reinitializeWithSimMode);

        // 无界模式初始化
        if (unboundedContainer != null) {
            unboundedPage = new UnboundedPage(unboundedContainer);
            unboundedPage.setModeSwitch(new UnboundedPage.ModeSwitch() {
                @Override
                public void switchToStandard() {
                    MainActivity.this.switchToStandard();
                }
                @Override
                public void switchToStandardSettings() {
                    // 临时切到标准模式看设置，不修改持久化的布局模式
                    temporaryStandardVisit = true;
                    isUnboundedMode = false;

                    standardContainer.setVisibility(View.VISIBLE);
                    standardContainer.setAlpha(0f);
                    standardContainer.animate().alpha(1f).setDuration(300)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                    unboundedContainer.animate().alpha(0f).setDuration(300)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .withEndAction(() -> unboundedContainer.setVisibility(View.GONE))
                            .start();

                    navBar.selectTab(3);
                }
            });
            unboundedPage.setAppSlotManager(appSlotManager);
        }

        // 设置页布局模式回调
        settingsPage.setOnLayoutModeChangedListener(isUnbounded -> {
            if (isUnbounded) switchToUnbounded();
            else switchToStandard();
        });
        settingsPage.setAppSlotManager(appSlotManager);

        // 读取上次的布局模式
        String layoutMode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString("layout_mode", "standard");
        if ("unbounded".equals(layoutMode) && unboundedContainer != null) {
            switchToUnboundedImmediate();
        }

        // 权限检查
        checkPermissions();
    }

    private void checkPermissions() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean firstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        boolean fromBoot = getIntent().getBooleanExtra("from_boot", false);
        boolean adbAlreadyGranted = prefs.getBoolean(KEY_ADB_GRANTED, false);

        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog(firstLaunch);
        }

        // 检查 BYD 车辆 API 权限
        if (adbAlreadyGranted) {
            Log.i(TAG, "ADB permissions already granted, skipping check");
        } else if (detectedEnv == BydEnvironmentDetector.Environment.PERMISSION_NEEDED) {
            if (fromBoot) {
                scheduleAdbCheckWithRetry(5, 0);
            } else {
                showBydPermissionDialog();
            }
        } else if (detectedEnv == BydEnvironmentDetector.Environment.REAL_DEVICE) {
            if (!BydPermissionHelper.hasAllPermissions(this)) {
                if (fromBoot) {
                    scheduleAdbCheckWithRetry(5, 0);
                } else {
                    showBydPermissionDialog();
                }
            }
        }

        // 检查是否为默认桌面
        checkDefaultLauncher();

        // 标记非首次启动
        if (firstLaunch) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        }
    }

    /**
     * 延迟重试检测 ADB 可用性（开机后 ADB 服务需要时间初始化）
     */
    private void scheduleAdbCheckWithRetry(int retriesLeft, int retryIndex) {
        long delay = retryIndex < RETRY_DELAYS.length ? RETRY_DELAYS[retryIndex] : 10000;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            if (AdbHelper.isAdbAvailable()) {
                Log.i(TAG, "ADB detected after delay");
                showAdbAuthDialog();
            } else if (retriesLeft > 1) {
                Log.i(TAG, "ADB not ready, retrying... (" + (retriesLeft - 1) + " left)");
                scheduleAdbCheckWithRetry(retriesLeft - 1, retryIndex + 1);
            } else {
                Log.w(TAG, "ADB not available after all retries, showing manual dialog");
                showBydPermissionDialog();
            }
        }, delay);
    }

    /**
     * 切换模拟/真车模式：重建 VehicleManager 和所有依赖 API 的页面
     */
    private void reinitializeWithSimMode(boolean forceSim) {
        Log.i(TAG, "Switching to " + (forceSim ? "simulation" : "real") + " mode");

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putBoolean("sim_mode_manual", forceSim).apply();

        BydVehicleManager.setForceSimulation(forceSim);
        BydVehicleManager.resetInstance();
        vehicleManager = BydVehicleManager.getInstance(this);
        vehicleManager.setListener(this);
        vehicleManager.startPolling();

        // ControlsPage 持有旧的 acApi 引用，需要重建
        View pageControlsView = findViewById(R.id.page_controls);
        controlsPage = new ControlsPage(pageControlsView, vehicleManager.getAcApi());
    }

    private void checkDefaultLauncher() {
        if (!isDefaultLauncher()) {
            showDefaultLauncherDialog();
        }
    }

    private boolean isDefaultLauncher() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    private void enableHomeLauncher() {
        ComponentName stub = new ComponentName(this, HomeStubActivity.class);
        int currentState = getPackageManager().getComponentEnabledSetting(stub);
        if (currentState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            getPackageManager().setComponentEnabledSetting(stub,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    private void disableHomeLauncher() {
        ComponentName stub = new ComponentName(this, HomeStubActivity.class);
        getPackageManager().setComponentEnabledSetting(stub,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP);
    }

    private void showDefaultLauncherDialog() {
        showDimDialog(new MaterialAlertDialogBuilder(this, R.style.AppAlertDialog)
                .setTitle(R.string.perm_launcher_title)
                .setMessage(R.string.perm_launcher_message)
                .setPositiveButton(R.string.perm_btn_settings, (d, w) -> {
                    enableHomeLauncher();
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
        if (AdbHelper.isAdbAvailable()) {
            showAdbAuthDialog();
        } else {
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
    }

    private void showAdbAuthDialog() {
        showDimDialog(new MaterialAlertDialogBuilder(this, R.style.AppAlertDialog)
                .setTitle(R.string.perm_byd_title)
                .setMessage(R.string.perm_byd_adb_message)
                .setPositiveButton(R.string.perm_btn_auth, (d, w) -> startAdbGrant())
                .setNegativeButton(R.string.perm_btn_cancel, null)
                .setCancelable(false));
    }

    private void startAdbGrant() {
        AdbHelper.grantPermissions(this, (success, granted, failed, signature) -> runOnUiThread(() -> {
            if (!granted.isEmpty()) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    int grantedCount = BydPermissionHelper.getGrantedCount(this);
                    int total = BydPermissionHelper.getAllPermissions().length;

                    Log.i(TAG, "授权验证: " + grantedCount + "/" + total
                            + " (signature级别: " + signature.size() + ")");

                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_ADB_GRANTED, true)
                            .remove("sim_mode_manual")
                            .apply();

                    String msg;
                    if (grantedCount == total) {
                        msg = "权限授权完成 (" + grantedCount + "/" + total + ")";
                    } else if (!signature.isEmpty()) {
                        msg = "已授权 " + grantedCount + "/" + total
                                + "，" + signature.size() + " 个需要平台签名（不影响基本功能）";
                    } else {
                        msg = "已授权 " + grantedCount + "/" + total;
                    }
                    android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();

                    BydVehicleManager.setForceSimulation(false);
                    BydVehicleManager.resetInstance();
                    vehicleManager = BydVehicleManager.getInstance(this);
                    vehicleManager.setListener(this);
                    vehicleManager.startPolling();

                    View pageControlsView = findViewById(R.id.page_controls);
                    controlsPage = new ControlsPage(pageControlsView, vehicleManager.getAcApi());

                    if (settingsPage != null) {
                        settingsPage.updateSimulationState(false);
                    }
                }, 500);
            } else {
                android.widget.Toast.makeText(this,
                        getString(R.string.perm_byd_adb_fail),
                        android.widget.Toast.LENGTH_LONG).show();
            }
        }));
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
        // 从无界模式临时访问设置页后，点击主页/控制/应用 tab 时回到无界模式
        if (temporaryStandardVisit && tabIndex != 3) {
            temporaryStandardVisit = false;
            switchToUnbounded();
            return;
        }
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
            if (unboundedPage != null && isUnboundedMode) {
                unboundedPage.updateStatus(status);
                unboundedPage.refreshMusic();
            }
        });
    }

    public void switchToUnbounded() {
        if (isUnboundedMode) return;
        isUnboundedMode = true;
        temporaryStandardVisit = false;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putString("layout_mode", "unbounded").apply();

        unboundedContainer.setVisibility(View.VISIBLE);
        unboundedContainer.setAlpha(0f);
        unboundedContainer.animate().alpha(1f).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        standardContainer.animate().alpha(0f).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> standardContainer.setVisibility(View.GONE))
                .start();
    }

    public void switchToStandard() {
        if (!isUnboundedMode) return;
        isUnboundedMode = false;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putString("layout_mode", "standard").apply();

        standardContainer.setVisibility(View.VISIBLE);
        standardContainer.setAlpha(0f);
        standardContainer.animate().alpha(1f).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        unboundedContainer.animate().alpha(0f).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> unboundedContainer.setVisibility(View.GONE))
                .start();
    }

    private void switchToUnboundedImmediate() {
        isUnboundedMode = true;
        standardContainer.setVisibility(View.GONE);
        unboundedContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (temporaryStandardVisit) {
            temporaryStandardVisit = false;
            switchToUnbounded();
        } else if (isUnboundedMode) {
            // 无界模式下按返回键不切换模式
        } else if (currentTab != 0) {
            navBar.selectTab(0);
        }
    }
}
