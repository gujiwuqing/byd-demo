package com.diui.launcher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

    public interface OnDirectGrantListener {
        void onDirectGrant();
    }

    public interface OnSimModeChangedListener {
        void onSimModeChanged(boolean isSimulation);
    }

    private final View rootView;
    private final Context context;
    private final ThemeManager themeManager;
    private final SharedPreferences prefs;

    private OnAdbAuthorizeListener adbAuthorizeListener;
    private OnDirectGrantListener directGrantListener;
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
        initManualGrant();
        initAdbDiag();
        initVehicleDiag();
        initDaemonDiag();
        initFidScan();
        initContentProbe();
        initFidBruteScan();
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

    public void setOnDirectGrantListener(OnDirectGrantListener listener) {
        this.directGrantListener = listener;
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

    // ── 手动授权脚本（甲壳虫兜底） ──

    /**
     * 甲壳虫等外部 ADB 工具连上车机后，在 shell 里粘贴执行的一键授权脚本。
     * 以 shell uid 执行 pm grant，等价于 app 内自动授权的 pm grant 步骤。
     */
    private static final String MANUAL_GRANT_SCRIPT =
            "PKG=com.diui.launcher\n" +
            "for p in \\\n" +
            "  android.permission.BYDAUTO_AC_GET \\\n" +
            "  android.permission.BYDAUTO_AC_SET \\\n" +
            "  android.permission.BYDAUTO_AC_COMMON \\\n" +
            "  android.permission.BYDAUTO_BODYWORK_GET \\\n" +
            "  android.permission.BYDAUTO_BODYWORK_COMMON \\\n" +
            "  android.permission.BYDAUTO_DOOR_LOCK_GET \\\n" +
            "  android.permission.BYDAUTO_DOOR_LOCK_COMMON \\\n" +
            "  android.permission.BYDAUTO_POWER_GET \\\n" +
            "  android.permission.BYDAUTO_ENERGY_GET \\\n" +
            "  android.permission.BYDAUTO_PM2P5_GET \\\n" +
            "  android.permission.BYDAUTO_STATISTIC_GET \\\n" +
            "  android.permission.BYDAUTO_GEARBOX_GET \\\n" +
            "  android.permission.BYDAUTO_SPEED_GET \\\n" +
            "  android.permission.BYDAUTO_CHARGING_GET \\\n" +
            "  android.permission.BYDAUTO_TYRE_GET \\\n" +
            "  android.permission.BYDAUTO_LIGHT_GET \\\n" +
            "  android.permission.BYDAUTO_LIGHT_SET \\\n" +
            "  android.permission.BYDAUTO_SETTING_GET; do\n" +
            "  pm grant $PKG $p 2>&1\n" +
            "done";

    private void initManualGrant() {
        rootView.findViewById(R.id.settings_manual_grant).setOnClickListener(v -> {
            Context ctx = rootView.getContext();

            TextView hint = new TextView(ctx);
            hint.setText(R.string.settings_manual_grant_dialog_hint);
            hint.setTextSize(13f);
            int pad = dpToPx(16);
            hint.setPadding(pad, pad, pad, dpToPx(8));

            TextView script = new TextView(ctx);
            script.setTypeface(android.graphics.Typeface.MONOSPACE);
            script.setText(MANUAL_GRANT_SCRIPT);
            script.setTextSize(12f);
            script.setPadding(pad, 0, pad, pad);

            android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
            scroll.addView(script);

            android.widget.LinearLayout container = new android.widget.LinearLayout(ctx);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.addView(hint);
            container.addView(scroll);

            new MaterialAlertDialogBuilder(ctx, R.style.AppAlertDialog)
                    .setTitle(R.string.settings_manual_grant_dialog_title)
                    .setView(container)
                    .setPositiveButton(R.string.settings_manual_grant_copy, (d, w) -> copyScript(ctx))
                    .setNeutralButton(R.string.settings_manual_grant_run, (d, w) -> {
                        if (directGrantListener != null) directGrantListener.onDirectGrant();
                    })
                    .setNegativeButton(R.string.perm_btn_cancel, null)
                    .show();
        });
    }

    private void copyScript(Context ctx) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                    "byd_grant_script", MANUAL_GRANT_SCRIPT);
            if (cm != null) cm.setPrimaryClip(clip);
            android.widget.Toast.makeText(ctx,
                    R.string.settings_manual_grant_copied,
                    android.widget.Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(ctx, "复制失败: " + e.getMessage(),
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // ── ADB 网络诊断 ──

    private void initAdbDiag() {
        rootView.findViewById(R.id.settings_adb_diag).setOnClickListener(v -> {
            Context ctx = rootView.getContext();
            android.widget.Toast.makeText(ctx, "正在探测 ADB 网络...", android.widget.Toast.LENGTH_SHORT).show();

            com.diui.launcher.api.AdbHelper.runDiagnostics(ctx, report -> {
                TextView tv = new TextView(ctx);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                tv.setText(report);
                tv.setTextSize(12f);
                int pad = dpToPx(16);
                tv.setPadding(pad, pad, pad, pad);
                tv.setTextIsSelectable(true);

                android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
                scroll.addView(tv);

                new MaterialAlertDialogBuilder(ctx, R.style.AppAlertDialog)
                        .setTitle("ADB 网络诊断")
                        .setView(scroll)
                        .setPositiveButton("复制", (d, w) -> {
                            try {
                                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                                        ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                                if (cm != null) {
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("adb_diag", report));
                                    android.widget.Toast.makeText(ctx, "诊断结果已复制", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception ignored) {}
                        })
                        .setNegativeButton("关闭", null)
                        .show();
            });
        });
    }

    // ── 真车模式诊断 ──

    private void initVehicleDiag() {
        rootView.findViewById(R.id.settings_vehicle_diag).setOnClickListener(v -> {
            Context ctx = rootView.getContext();
            android.widget.Toast.makeText(ctx, "正在采集诊断数据...", android.widget.Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                com.diui.launcher.api.BydVehicleManager vm =
                        com.diui.launcher.api.BydVehicleManager.getInstance(ctx);
                String report = vm.buildDiagReport();
                android.util.Log.i("VehicleDiag", report);

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    TextView tv = new TextView(ctx);
                    tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                    tv.setText(report);
                    tv.setTextSize(11f);
                    int pad = dpToPx(16);
                    tv.setPadding(pad, pad, pad, pad);
                    tv.setTextIsSelectable(true);

                    android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
                    scroll.addView(tv);

                    new MaterialAlertDialogBuilder(ctx, R.style.AppAlertDialog)
                            .setTitle("真车模式诊断")
                            .setView(scroll)
                            .setPositiveButton("复制", (d, w) -> {
                                try {
                                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                                            ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                                    if (cm != null) {
                                        cm.setPrimaryClip(android.content.ClipData.newPlainText("vehicle_diag", report));
                                        android.widget.Toast.makeText(ctx, "诊断结果已复制", android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception ignored) {}
                            })
                            .setNegativeButton("关闭", null)
                            .show();
                });
            }).start();
        });
    }

    // ── HelperDaemon 诊断 ──

    private void initDaemonDiag() {
        rootView.findViewById(R.id.settings_daemon_diag).setOnClickListener(v -> {
            Context ctx = rootView.getContext();
            android.widget.Toast.makeText(ctx, "正在启动 HelperDaemon 诊断（约 10 秒）...", android.widget.Toast.LENGTH_LONG).show();

            com.diui.launcher.api.AdbHelper.startHelperDaemonWithDiag(ctx, report -> {
                TextView tv = new TextView(ctx);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                tv.setText(report);
                tv.setTextSize(11f);
                int pad = dpToPx(16);
                tv.setPadding(pad, pad, pad, pad);
                tv.setTextIsSelectable(true);

                android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
                scroll.addView(tv);

                new MaterialAlertDialogBuilder(ctx, R.style.AppAlertDialog)
                        .setTitle("HelperDaemon 诊断")
                        .setView(scroll)
                        .setPositiveButton("复制", (d, w) -> {
                            try {
                                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                                        ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                                if (cm != null) {
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("daemon_diag", report));
                                    android.widget.Toast.makeText(ctx, "诊断结果已复制", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception ignored) {}
                        })
                        .setNegativeButton("关闭", null)
                        .show();
            });
        });
    }

    // ── FID 全量扫描 ──

    private void initFidScan() {
        rootView.findViewById(R.id.settings_fid_scan).setOnClickListener(v -> {
            Context ctx = rootView.getContext();
            android.widget.Toast.makeText(ctx, "正在扫描所有 FID（约 15 秒）...", android.widget.Toast.LENGTH_LONG).show();

            com.diui.launcher.api.AutoserviceClient.scanAll(report -> {
                TextView tv = new TextView(ctx);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                tv.setText(report);
                tv.setTextSize(11f);
                int pad = dpToPx(16);
                tv.setPadding(pad, pad, pad, pad);
                tv.setTextIsSelectable(true);

                android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
                scroll.addView(tv);

                new MaterialAlertDialogBuilder(ctx, R.style.AppAlertDialog)
                        .setTitle("FID 全量扫描")
                        .setView(scroll)
                        .setPositiveButton("复制", (d, w) -> {
                            try {
                                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                                        ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                                if (cm != null) {
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("fid_scan", report));
                                    android.widget.Toast.makeText(ctx, "扫描结果已复制", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception ignored) {}
                        })
                        .setNegativeButton("关闭", null)
                        .show();
            });
        });
    }

    // ── Content Provider 探测 ──

    private void initContentProbe() {
        rootView.findViewById(R.id.settings_content_probe).setOnClickListener(v -> {
            Context ctx = rootView.getContext();
            android.widget.Toast.makeText(ctx, "正在查询 Content Provider（约5秒）...", android.widget.Toast.LENGTH_LONG).show();

            com.diui.launcher.api.AutoserviceClient.probeContentProviders(report ->
                    showReport(ctx, "Content Provider 探测", report));
        });
    }

    // ── 暴力 FID 发现 ──

    private void initFidBruteScan() {
        rootView.findViewById(R.id.settings_fid_brute).setOnClickListener(v -> {
            Context ctx = rootView.getContext();
            String[] options = {"快速（已知范围附近 ±512）", "中量（±8192，约3分钟）", "全量（所有设备 ×50000，约15分钟）"};
            new MaterialAlertDialogBuilder(ctx, R.style.AppAlertDialog)
                    .setTitle("选择扫描范围")
                    .setItems(options, (d, which) -> {
                        int[] devTypes = {1000, 1001, 1004, 1006, 1007, 1009, 1011, 1012, 1013, 1014, 1016, 1023, 1032};

                        int[] fids;
                        String label;
                        if (which == 0) {
                            fids = buildCandidates(512);
                            label = "快速扫描（约30秒）...";
                        } else if (which == 1) {
                            fids = buildCandidates(8192);
                            label = "中量扫描（约3分钟）...";
                        } else {
                            fids = buildCandidates(50000);
                            label = "全量扫描（约15分钟）...";
                        }

                        android.widget.Toast.makeText(ctx, label, android.widget.Toast.LENGTH_LONG).show();
                        com.diui.launcher.api.AutoserviceClient.bruteForceScan(devTypes, fids, report -> showReport(ctx, "FID 发现结果", report));
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    /** 生成候选 FID 列表：已知 FID ± range，步长 2（BYD FID 对齐方式）*/
    private int[] buildCandidates(int range) {
        // 已知锚点 FID
        int[] anchors = {
            1077936144, 1077936148, 1077936156, 1077936168, 1077936184,
            1031798832, 692060168, 692060184, 692060188, 947912728,
            1267728400, 947912736, 947912752, 1074790416, 1128267816,
            1246777400, 1246765072, 1145045032, 1148190752, 1148190736,
            1147142192, 1147142160, 1032871984, -1807745016, 555745336,
            339738656, 876609586, 876609592, 876609560, 666894360,
            315621408, 555745294, 874512420, 950009866, 1231040528,
            692060184, 1081081864, -1728052956, -1728052952, -1728052948, -1728052944
        };

        java.util.TreeSet<Integer> set = new java.util.TreeSet<>();
        for (int anchor : anchors) {
            for (int delta = -range; delta <= range; delta += 2) {
                set.add(anchor + delta);
            }
        }
        int[] result = new int[set.size()];
        int i = 0;
        for (int fid : set) result[i++] = fid;
        return result;
    }

    private void showReport(Context ctx, String title, String report) {
        TextView tv = new TextView(ctx);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setText(report);
        tv.setTextSize(11f);
        int pad = dpToPx(16);
        tv.setPadding(pad, pad, pad, pad);
        tv.setTextIsSelectable(true);

        android.widget.ScrollView scroll = new android.widget.ScrollView(ctx);
        scroll.addView(tv);

        new MaterialAlertDialogBuilder(ctx, R.style.AppAlertDialog)
                .setTitle(title)
                .setView(scroll)
                .setPositiveButton("复制", (d2, w2) -> {
                    try {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager)
                                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("fid", report));
                            android.widget.Toast.makeText(ctx, "已复制", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("关闭", null)
                .show();
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
                    public void onComplete(String report) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            if (probeTitle != null) {
                                probeTitle.setText("API 探测");
                            }

                            TextView tv = new TextView(context);
                            tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                            tv.setText(report);
                            tv.setTextSize(10f);
                            int pad = dpToPx(16);
                            tv.setPadding(pad, pad, pad, pad);
                            tv.setTextIsSelectable(true);

                            android.widget.ScrollView scroll = new android.widget.ScrollView(context);
                            scroll.addView(tv);

                            new MaterialAlertDialogBuilder(context, R.style.AppAlertDialog)
                                    .setTitle("API 探测报告")
                                    .setView(scroll)
                                    .setPositiveButton("复制", (d, w) -> {
                                        try {
                                            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                                                    context.getSystemService(Context.CLIPBOARD_SERVICE);
                                            if (cm != null) {
                                                cm.setPrimaryClip(android.content.ClipData.newPlainText("api_probe", report));
                                                android.widget.Toast.makeText(context, "报告已复制", android.widget.Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (Exception ignored) {}
                                    })
                                    .setNegativeButton("关闭", null)
                                    .show();
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
