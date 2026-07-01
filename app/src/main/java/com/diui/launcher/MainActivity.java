package com.diui.launcher;

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

import com.diui.launcher.api.AdbHelper;
import com.diui.launcher.api.AdbKeyManager;
import com.diui.launcher.api.BydApiExplorer;
import com.diui.launcher.api.BydEnvironmentDetector;
import com.diui.launcher.api.BydPermissionHelper;
import com.diui.launcher.api.BydVehicleManager;
import com.diui.launcher.model.VehicleStatus;
import com.diui.launcher.theme.ThemeManager;
import com.diui.launcher.ui.AppsPage;
import com.diui.launcher.ui.ControlsPage;
import com.diui.launcher.ui.NavBar;
import com.diui.launcher.ui.SettingsPage;
import com.diui.launcher.ui.StatusPage;
import com.diui.launcher.ui.TopBar;
import com.diui.launcher.ui.UnboundedPage;
import com.diui.launcher.ui.AppSlotManager;
import com.diui.launcher.ui.WindowPanelController;

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
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit().putBoolean(KEY_ADB_GRANTED, false).apply();
            if (detectedEnv == BydEnvironmentDetector.Environment.SIMULATOR) {
                android.widget.Toast.makeText(this,
                        "模拟器环境，ADB 授权仅在真实车机上有效",
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
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
                    showStandardContainerForSettings();
                }
            });
            unboundedPage.setAcApi(vehicleManager.getAcApi());
            unboundedPage.setAppSlotManager(appSlotManager);
        }

        // 设置页布局模式回调（唯一的模式切换入口）
        settingsPage.setOnLayoutModeChangedListener(isUnbounded -> {
            if (isUnbounded) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit().putString("layout_mode", "unbounded").apply();
                // 立即切换到无界模式
                isUnboundedMode = true;
                unboundedContainer.setVisibility(View.VISIBLE);
                standardContainer.setVisibility(View.GONE);
                unboundedContainer.setAlpha(1f);
            } else {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit().putString("layout_mode", "standard").apply();
                isUnboundedMode = false;
                // 已在标准容器中，无需切换视图
            }
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
            AdbHelper.startHelperDaemon(this, () ->
                    Log.i(TAG, "HelperDaemon restarted on launch"));
        } else if (detectedEnv == BydEnvironmentDetector.Environment.PERMISSION_NEEDED
                || detectedEnv == BydEnvironmentDetector.Environment.REAL_DEVICE) {
            if (fromBoot) {
                scheduleAdbGrantWithRetry(5, 0);
            } else {
                autoAdbGrant();
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
     * 开机后延迟重试 ADB 授权（ADB 服务需要时间初始化）
     */
    private void scheduleAdbGrantWithRetry(int retriesLeft, int retryIndex) {
        long delay = retryIndex < RETRY_DELAYS.length ? RETRY_DELAYS[retryIndex] : 10000;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            if (AdbHelper.isAdbAvailable()) {
                Log.i(TAG, "ADB available after boot delay, auto granting...");
                autoAdbGrant();
            } else if (retriesLeft > 1) {
                Log.i(TAG, "ADB not ready, retrying... (" + (retriesLeft - 1) + " left)");
                scheduleAdbGrantWithRetry(retriesLeft - 1, retryIndex + 1);
            } else {
                Log.w(TAG, "ADB not available after all retries");
            }
        }, delay);
    }

    /**
     * 自动执行 ADB 授权，不弹应用内对话框。
     * 系统的"允许 USB 调试？"对话框仍会由 adbd 自动弹出（首次需用户点允许）。
     */
    private void autoAdbGrant() {
        if (!AdbHelper.isAdbAvailable()) {
            Log.w(TAG, "ADB not available, skip auto grant");
            return;
        }
        Log.i(TAG, "Auto ADB grant starting...");
        startAdbGrant();
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
        try {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
        }
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
        if (detectedEnv == BydEnvironmentDetector.Environment.SIMULATOR) {
            showDimDialog(new MaterialAlertDialogBuilder(this, R.style.AppAlertDialog)
                    .setTitle(R.string.perm_byd_title)
                    .setMessage("当前为模拟器环境，ADB 授权仅在真实 BYD 车机上有效。\n\n模拟器中请使用模拟模式体验功能。")
                    .setPositiveButton(R.string.perm_btn_ok, null));
            return;
        }
        showDimDialog(new MaterialAlertDialogBuilder(this, R.style.AppAlertDialog)
                .setTitle(R.string.perm_byd_title)
                .setMessage(R.string.perm_byd_adb_message)
                .setPositiveButton(R.string.perm_btn_auth, (d, w) -> startAdbGrant())
                .setNegativeButton(R.string.perm_btn_cancel, null)
                .setCancelable(false));
    }

    private void startAdbGrant() {
        showSystemUI();
        AdbKeyManager.clearKeys(this);
        AdbHelper.grantPermissions(this, (success, granted, failed, signature) -> runOnUiThread(() -> {
            boolean authSucceeded = !granted.isEmpty() || !failed.isEmpty() || !signature.isEmpty();
            if (authSucceeded) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.i(TAG, "ADB 认证成功: granted=" + granted.size()
                            + " failed=" + failed.size() + " signature=" + signature.size());

                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_ADB_GRANTED, true)
                            .remove("sim_mode_manual")
                            .apply();

                    AdbHelper.startHelperDaemon(this, () -> {
                        Log.i(TAG, "HelperDaemon started after ADB auth");
                    });

                    String msg;
                    if (!granted.isEmpty()) {
                        msg = "ADB授权成功，已授权 " + granted.size() + " 个权限";
                    } else {
                        msg = "ADB授权成功，车辆API通过系统服务访问";
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

                    hideSystemUI();
                }, 500);
            } else {
                hideSystemUI();
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

    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onTabSelected(int tabIndex) {
        if (tabIndex == 0) {
            // 回主页：根据持久化的 layout_mode 决定显示哪个模式
            String mode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString("layout_mode", "standard");
            if ("unbounded".equals(mode)) {
                isUnboundedMode = true;
                unboundedContainer.setVisibility(View.VISIBLE);
                unboundedContainer.setAlpha(1f);
                standardContainer.setVisibility(View.GONE);
                return;
            }
        }
        // 标准模式下正常切换 tab（包括从无界模式临时看设置时切其他 tab）
        if (unboundedContainer.getVisibility() == View.GONE || tabIndex == 3) {
            currentTab = tabIndex;
            for (int i = 0; i < pages.length; i++) {
                pages[i].setVisibility(i == tabIndex ? View.VISIBLE : View.GONE);
            }
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

    private void showStandardContainerForSettings() {
        standardContainer.setVisibility(View.VISIBLE);
        standardContainer.setAlpha(1f);
        unboundedContainer.setVisibility(View.GONE);
        isUnboundedMode = false;
        navBar.selectTab(3);
    }

    @Override
    public void onBackPressed() {
        if (currentTab != 0) {
            navBar.selectTab(0);
        }
    }
}
