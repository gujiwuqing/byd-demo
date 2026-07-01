package com.diui.launcher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.diui.launcher.LogActivity;
import com.diui.launcher.R;
import com.diui.launcher.api.BydApiExplorer;
import com.diui.launcher.theme.ThemeManager;

public class SettingsPage {

    private static final String PREFS_NAME = "byd_launcher_prefs";
    private static final String KEY_CLOCK_24H = "clock_24h";

    public interface OnAdbAuthorizeListener {
        void onAdbAuthorize();
    }

    public interface OnSimModeChangedListener {
        void onSimModeChanged(boolean isSimulation);
    }

    private final View rootView;
    private final Context context;
    private final ThemeManager themeManager;
    private final SharedPreferences prefs;

    private OnAdbAuthorizeListener adbAuthorizeListener;
    private OnSimModeChangedListener simModeChangedListener;
    private AppSlotManager appSlotManager;
    private TextView btnLayoutStandard, btnLayoutUnbounded;

    public interface OnLayoutModeChangedListener {
        void onLayoutModeChanged(boolean isUnbounded);
    }
    private OnLayoutModeChangedListener layoutModeChangedListener;

    // 主题分段按钮
    private final TextView btnThemeSystem, btnThemeLight, btnThemeDark;
    // 时钟 Toggle Switch
    private final FrameLayout clockSwitch;
    private final View clockTrack, clockThumb;
    // 模拟模式 Toggle Switch
    private final FrameLayout simSwitch;
    private final View simTrack, simThumb;
    private final TextView simDesc;

    public SettingsPage(View rootView, boolean isSimulation) {
        this.rootView = rootView;
        this.context = rootView.getContext();
        this.themeManager = ThemeManager.getInstance(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        btnThemeSystem = rootView.findViewById(R.id.settings_theme_system);
        btnThemeLight = rootView.findViewById(R.id.settings_theme_light);
        btnThemeDark = rootView.findViewById(R.id.settings_theme_dark);

        clockSwitch = rootView.findViewById(R.id.settings_clock_switch);
        clockTrack = rootView.findViewById(R.id.settings_clock_track);
        clockThumb = rootView.findViewById(R.id.settings_clock_thumb);

        simSwitch = rootView.findViewById(R.id.settings_sim_switch);
        simTrack = rootView.findViewById(R.id.settings_sim_track);
        simThumb = rootView.findViewById(R.id.settings_sim_thumb);
        simDesc = rootView.findViewById(R.id.settings_sim_desc);

        initThemeButtons();
        initClockSwitch();
        initSimSwitch(isSimulation);
        initDefaultLauncher();
        initLogViewer();
        initAdbAuthorize();
        initApiProbe();
        initLayoutMode();
        initAppSlots();
    }

    public void setOnSimModeChangedListener(OnSimModeChangedListener listener) {
        this.simModeChangedListener = listener;
    }

    public void setOnAdbAuthorizeListener(OnAdbAuthorizeListener listener) {
        this.adbAuthorizeListener = listener;
    }

    public void setOnLayoutModeChangedListener(OnLayoutModeChangedListener listener) {
        this.layoutModeChangedListener = listener;
    }

    public void setAppSlotManager(AppSlotManager manager) {
        this.appSlotManager = manager;
        refreshSlotLabels();
    }

    // ── 主题 ──

    private void initThemeButtons() {
        btnThemeSystem.setOnClickListener(v -> setTheme(ThemeManager.MODE_SYSTEM));
        btnThemeLight.setOnClickListener(v -> setTheme(ThemeManager.MODE_LIGHT));
        btnThemeDark.setOnClickListener(v -> setTheme(ThemeManager.MODE_DARK));
        highlightTheme();
    }

    private void setTheme(int mode) {
        themeManager.setThemeMode(mode);
    }

    private void highlightTheme() {
        int current = themeManager.getThemeMode();
        setSegmentActive(btnThemeSystem, current == ThemeManager.MODE_SYSTEM);
        setSegmentActive(btnThemeLight, current == ThemeManager.MODE_LIGHT);
        setSegmentActive(btnThemeDark, current == ThemeManager.MODE_DARK);
    }

    // ── 时钟 Toggle Switch ──

    private void initClockSwitch() {
        boolean is24h = prefs.getBoolean(KEY_CLOCK_24H, true);
        clockSwitch.setOnClickListener(v -> setClock24h(!isClock24h()));
        updateClockSwitch(is24h);
    }

    private boolean isClock24h() {
        return prefs.getBoolean(KEY_CLOCK_24H, true);
    }

    private void setClock24h(boolean is24h) {
        prefs.edit().putBoolean(KEY_CLOCK_24H, is24h).apply();
        updateClockSwitch(is24h);
    }

    private void updateClockSwitch(boolean isOn) {
        clockTrack.setBackgroundResource(isOn
                ? R.drawable.bg_settings_switch_track_on
                : R.drawable.bg_settings_switch_track_off);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) clockThumb.getLayoutParams();
        if (isOn) {
            params.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END;
            params.setMarginEnd(dpToPx(2));
            params.setMarginStart(0);
        } else {
            params.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START;
            params.setMarginStart(dpToPx(2));
            params.setMarginEnd(0);
        }
        clockThumb.setLayoutParams(params);
    }

    // ── 模拟模式 Toggle Switch ──

    private boolean isSimMode() {
        return prefs.getBoolean("sim_mode", true);
    }

    private void initSimSwitch(boolean isSimulation) {
        updateSimSwitch(isSimulation);
        simSwitch.setOnClickListener(v -> {
            boolean newVal = !isSimMode();
            prefs.edit().putBoolean("sim_mode", newVal).apply();
            updateSimSwitch(newVal);
            if (simModeChangedListener != null) {
                simModeChangedListener.onSimModeChanged(newVal);
            }
        });
    }

    private void updateSimSwitch(boolean isOn) {
        simTrack.setBackgroundResource(isOn
                ? R.drawable.bg_settings_switch_track_on
                : R.drawable.bg_settings_switch_track_off);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) simThumb.getLayoutParams();
        if (isOn) {
            params.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END;
            params.setMarginEnd(dpToPx(2));
            params.setMarginStart(0);
        } else {
            params.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START;
            params.setMarginStart(dpToPx(2));
            params.setMarginEnd(0);
        }
        simThumb.setLayoutParams(params);

        simDesc.setText(isOn
                ? R.string.settings_sim_mode_on
                : R.string.settings_sim_mode_real);
    }

    public void updateSimulationState(boolean isSimulation) {
        updateSimSwitch(isSimulation);
    }

    // ── 默认桌面 ──

    private void initDefaultLauncher() {
        rootView.findViewById(R.id.settings_set_default).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_HOME_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });
    }

    // ── 日志查看器 ──

    private void initLogViewer() {
        rootView.findViewById(R.id.settings_view_log).setOnClickListener(v -> {
            Intent intent = new Intent(context, LogActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    // ── ADB 权限授权 ──

    private void initAdbAuthorize() {
        rootView.findViewById(R.id.settings_adb_authorize).setOnClickListener(v -> {
            if (adbAuthorizeListener != null) {
                adbAuthorizeListener.onAdbAuthorize();
            }
        });
    }

    // ── API 探测 ──

    private void initApiProbe() {
        View btnApiProbe = rootView.findViewById(R.id.btn_api_probe);
        if (btnApiProbe != null) {
            TextView probeTitle = rootView.findViewById(R.id.btn_api_probe_title);
            btnApiProbe.setOnClickListener(v -> {
                android.widget.Toast.makeText(context, "正在扫描 API...", android.widget.Toast.LENGTH_SHORT).show();
                BydApiExplorer.runFullProbe(context, new BydApiExplorer.ProbeProgressListener() {
                    @Override
                    public void onProgress(int current, int total, String message) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            if (probeTitle != null) {
                                probeTitle.setText(message + " (" + current + "/" + total + ")");
                            }
                        });
                    }

                    @Override
                    public void onComplete(String filePath) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            if (probeTitle != null) {
                                probeTitle.setText("API 探测");
                            }
                            android.widget.Toast.makeText(context,
                                    "探测完成，报告: " + filePath,
                                    android.widget.Toast.LENGTH_LONG).show();
                        });
                    }
                });
            });
        }
    }

    // ── 布局模式 ──

    private void initLayoutMode() {
        btnLayoutStandard = rootView.findViewById(R.id.settings_layout_standard);
        btnLayoutUnbounded = rootView.findViewById(R.id.settings_layout_unbounded);
        if (btnLayoutStandard == null || btnLayoutUnbounded == null) return;

        String mode = prefs.getString("layout_mode", "standard");
        highlightLayoutMode("unbounded".equals(mode));

        btnLayoutStandard.setOnClickListener(v -> {
            prefs.edit().putString("layout_mode", "standard").apply();
            highlightLayoutMode(false);
            if (layoutModeChangedListener != null) layoutModeChangedListener.onLayoutModeChanged(false);
        });
        btnLayoutUnbounded.setOnClickListener(v -> {
            prefs.edit().putString("layout_mode", "unbounded").apply();
            highlightLayoutMode(true);
            if (layoutModeChangedListener != null) layoutModeChangedListener.onLayoutModeChanged(true);
        });
    }

    private void highlightLayoutMode(boolean isUnbounded) {
        if (btnLayoutStandard == null) return;
        setSegmentActive(btnLayoutStandard, !isUnbounded);
        setSegmentActive(btnLayoutUnbounded, isUnbounded);
    }

    // ── 应用配置 ──

    private void initAppSlots() {
        setupSlot(R.id.settings_slot_nav, R.id.settings_slot_nav_value, AppSlotManager.SLOT_NAV);
        setupSlot(R.id.settings_slot_music, R.id.settings_slot_music_value, AppSlotManager.SLOT_MUSIC);
        setupSlot(R.id.settings_slot_video, R.id.settings_slot_video_value, AppSlotManager.SLOT_VIDEO);
        setupSlot(R.id.settings_slot_phone, R.id.settings_slot_phone_value, AppSlotManager.SLOT_PHONE);
    }

    private void setupSlot(int rowId, int valueId, String slot) {
        View row = rootView.findViewById(rowId);
        TextView value = rootView.findViewById(valueId);
        if (row == null || value == null) return;

        row.setOnClickListener(v -> {
            if (appSlotManager == null) return;
            AppPickerDialog.show(context, appSlotManager, (packageName, label) -> {
                appSlotManager.setPackageName(slot, packageName);
                value.setText(label);
            });
        });
    }

    private void refreshSlotLabels() {
        if (appSlotManager == null) return;
        updateSlotLabel(R.id.settings_slot_nav_value, AppSlotManager.SLOT_NAV);
        updateSlotLabel(R.id.settings_slot_music_value, AppSlotManager.SLOT_MUSIC);
        updateSlotLabel(R.id.settings_slot_video_value, AppSlotManager.SLOT_VIDEO);
        updateSlotLabel(R.id.settings_slot_phone_value, AppSlotManager.SLOT_PHONE);
    }

    private void updateSlotLabel(int viewId, String slot) {
        TextView tv = rootView.findViewById(viewId);
        if (tv != null && appSlotManager != null) {
            tv.setText(appSlotManager.getAppLabel(slot));
        }
    }

    // ── 工具方法 ──

    private void setSegmentActive(TextView btn, boolean active) {
        btn.setBackgroundResource(active
                ? R.drawable.bg_settings_segment_selected
                : R.drawable.bg_settings_segment_unselected);
        btn.setTextColor(ContextCompat.getColor(context,
                active ? R.color.text_on_accent : R.color.text_secondary));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    // ── 静态工具方法 ──

    public static boolean isClock24h(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_CLOCK_24H, true);
    }
}
