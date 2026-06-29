package com.bydlauncher;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bydlauncher.theme.ThemeManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogActivity extends AppCompatActivity {

    private static final Set<String> BYD_TAGS = new HashSet<>(Arrays.asList(
            "BydReflection", "BydApiExplorer", "BydAcApi", "BydDriveApi",
            "BydTireApi", "BydVehicleManager", "BydBodyworkApi",
            "BydStatisticApi", "BydDoorLockApi"
    ));

    private TextView logContent;
    private ScrollView logScroll;
    private TextView btnAll, btnByd;

    private boolean bydOnly = true;
    private List<String> allLines = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance(this).applyTheme();
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_log);

        logContent = findViewById(R.id.log_content);
        logScroll = findViewById(R.id.log_scroll);
        btnAll = findViewById(R.id.log_btn_all);
        btnByd = findViewById(R.id.log_btn_byd);

        btnAll.setOnClickListener(v -> {
            bydOnly = false;
            updateFilterButtons();
            displayLogs();
        });

        btnByd.setOnClickListener(v -> {
            bydOnly = true;
            updateFilterButtons();
            displayLogs();
        });

        findViewById(R.id.log_btn_refresh).setOnClickListener(v -> loadLogs());
        findViewById(R.id.log_btn_clear).setOnClickListener(v -> {
            try {
                Runtime.getRuntime().exec("logcat -c");
            } catch (Exception ignored) {}
            allLines.clear();
            displayLogs();
        });
        findViewById(R.id.log_btn_back).setOnClickListener(v -> finish());

        loadLogs();
    }

    private void updateFilterButtons() {
        if (bydOnly) {
            btnByd.setBackgroundResource(R.drawable.bg_settings_segment_selected);
            btnByd.setTextColor(ContextCompat.getColor(this, R.color.text_on_accent));
            btnAll.setBackgroundResource(R.drawable.bg_settings_segment_unselected);
            btnAll.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            btnAll.setBackgroundResource(R.drawable.bg_settings_segment_selected);
            btnAll.setTextColor(ContextCompat.getColor(this, R.color.text_on_accent));
            btnByd.setBackgroundResource(R.drawable.bg_settings_segment_unselected);
            btnByd.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void loadLogs() {
        logContent.setText("加载中...");
        executor.execute(() -> {
            List<String> lines = new ArrayList<>();
            try {
                Process process = Runtime.getRuntime().exec("logcat -d -t 500 -v brief");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                reader.close();
                process.destroy();
            } catch (Exception e) {
                lines.add("读取日志失败: " + e.getMessage());
            }

            runOnUiThread(() -> {
                allLines = lines;
                displayLogs();
            });
        });
    }

    private void displayLogs() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int count = 0;

        for (String line : allLines) {
            if (bydOnly && !isBydLine(line)) continue;

            int start = sb.length();
            sb.append(line).append('\n');
            int color = getColorForLine(line);
            sb.setSpan(new ForegroundColorSpan(color), start, sb.length() - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            count++;
        }

        if (count == 0) {
            sb.append(bydOnly ? "暂无 BYD 相关日志" : "暂无日志");
        }

        logContent.setText(sb);
        logScroll.post(() -> logScroll.fullScroll(android.widget.ScrollView.FOCUS_DOWN));
    }

    private boolean isBydLine(String line) {
        for (String tag : BYD_TAGS) {
            if (line.contains(tag)) return true;
        }
        return false;
    }

    private int getColorForLine(String line) {
        if (line.startsWith("E/") || line.contains(" E/")) {
            return Color.parseColor("#FFFF5252");
        }
        if (line.startsWith("W/") || line.contains(" W/")) {
            return Color.parseColor("#FFFFAB40");
        }
        if (line.startsWith("I/") || line.contains(" I/")) {
            return Color.parseColor("#FF00C8F0");
        }
        return Color.parseColor("#FF8899AA");
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
}
