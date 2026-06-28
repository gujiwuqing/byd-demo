package com.bydlauncher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;
import com.bydlauncher.theme.ThemeManager;

public class SettingsPage {

    private static final String PREFS_NAME = "byd_launcher_prefs";
    private static final String KEY_CLOCK_24H = "clock_24h";
    private static final String KEY_TEMP_UNIT = "temp_unit";
    private static final String KEY_PRESSURE_UNIT = "pressure_unit";

    public static final int TEMP_C = 0;
    public static final int TEMP_F = 1;
    public static final int PRESSURE_PSI = 0;
    public static final int PRESSURE_KPA = 1;
    public static final int PRESSURE_BAR = 2;

    private final View rootView;
    private final Context context;
    private final ThemeManager themeManager;
    private final SharedPreferences prefs;

    // 主题分段按钮
    private final TextView btnThemeSystem, btnThemeLight, btnThemeDark;
    // 时钟 Toggle Switch
    private final FrameLayout clockSwitch;
    private final View clockTrack, clockThumb;
    // 温度分段按钮
    private final TextView btnTempC, btnTempF;
    // 胎压分段按钮
    private final TextView btnPsi, btnKpa, btnBar;
    // 状态
    private final TextView simStatus;

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

        btnTempC = rootView.findViewById(R.id.settings_temp_c);
        btnTempF = rootView.findViewById(R.id.settings_temp_f);

        btnPsi = rootView.findViewById(R.id.settings_psi);
        btnKpa = rootView.findViewById(R.id.settings_kpa);
        btnBar = rootView.findViewById(R.id.settings_bar);

        simStatus = rootView.findViewById(R.id.settings_sim_status);

        initThemeButtons();
        initClockSwitch();
        initTempUnitButtons();
        initPressureButtons();
        initSimStatus(isSimulation);
        initDefaultLauncher();
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

    // ── 温度 ──

    private void initTempUnitButtons() {
        int unit = prefs.getInt(KEY_TEMP_UNIT, TEMP_C);
        btnTempC.setOnClickListener(v -> setTempUnit(TEMP_C));
        btnTempF.setOnClickListener(v -> setTempUnit(TEMP_F));
        highlightTempUnit(unit);
    }

    private void setTempUnit(int unit) {
        prefs.edit().putInt(KEY_TEMP_UNIT, unit).apply();
        highlightTempUnit(unit);
    }

    private void highlightTempUnit(int unit) {
        setSegmentActive(btnTempC, unit == TEMP_C);
        setSegmentActive(btnTempF, unit == TEMP_F);
    }

    // ── 胎压 ──

    private void initPressureButtons() {
        int unit = prefs.getInt(KEY_PRESSURE_UNIT, PRESSURE_PSI);
        btnPsi.setOnClickListener(v -> setPressureUnit(PRESSURE_PSI));
        btnKpa.setOnClickListener(v -> setPressureUnit(PRESSURE_KPA));
        btnBar.setOnClickListener(v -> setPressureUnit(PRESSURE_BAR));
        highlightPressure(unit);
    }

    private void setPressureUnit(int unit) {
        prefs.edit().putInt(KEY_PRESSURE_UNIT, unit).apply();
        highlightPressure(unit);
    }

    private void highlightPressure(int unit) {
        setSegmentActive(btnPsi, unit == PRESSURE_PSI);
        setSegmentActive(btnKpa, unit == PRESSURE_KPA);
        setSegmentActive(btnBar, unit == PRESSURE_BAR);
    }

    // ── 模拟模式 ──

    private void initSimStatus(boolean isSimulation) {
        if (isSimulation) {
            simStatus.setText(R.string.settings_sim_on);
            simStatus.setTextColor(ContextCompat.getColor(context, R.color.status_fair));
        } else {
            simStatus.setText(R.string.settings_sim_off);
            simStatus.setTextColor(ContextCompat.getColor(context, R.color.status_good));
        }
    }

    // ── 默认桌面 ──

    private void initDefaultLauncher() {
        rootView.findViewById(R.id.settings_set_default).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
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

    public static int getTempUnit(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_TEMP_UNIT, TEMP_C);
    }

    public static int getPressureUnit(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_PRESSURE_UNIT, PRESSURE_PSI);
    }
}
