package com.bydlauncher.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.bydlauncher.R;

import java.util.List;

public class AppPickerDialog {

    public interface OnAppSelected {
        void onSelected(String packageName, String label);
    }

    public static void show(Context context, AppSlotManager slotManager, OnAppSelected callback) {
        List<AppSlotManager.AppInfo> allApps = slotManager.getInstalledApps();

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(context, 16), dp(context, 8), dp(context, 16), 0);

        EditText search = new EditText(context);
        search.setHint(R.string.settings_app_search_hint);
        search.setTextSize(14);
        search.setSingleLine(true);
        container.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(context);
        LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);
        container.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 400)));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.AppAlertDialog)
                .setTitle(R.string.settings_app_pick_title)
                .setView(container)
                .setNegativeButton(R.string.perm_btn_cancel, null)
                .create();

        Runnable refreshList = () -> {
            listContainer.removeAllViews();
            String query = search.getText().toString().toLowerCase();
            for (AppSlotManager.AppInfo app : allApps) {
                if (!query.isEmpty() && !app.label.toLowerCase().contains(query)
                        && !app.packageName.toLowerCase().contains(query)) {
                    continue;
                }
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(context, 8), 0, dp(context, 8));
                row.setClickable(true);
                row.setFocusable(true);

                ImageView icon = new ImageView(context);
                icon.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 36), dp(context, 36)));
                icon.setImageDrawable(app.icon);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                row.addView(icon);

                LinearLayout textCol = new LinearLayout(context);
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                textParams.setMarginStart(dp(context, 12));
                textCol.setLayoutParams(textParams);

                TextView labelTv = new TextView(context);
                labelTv.setText(app.label);
                labelTv.setTextSize(14);
                labelTv.setTextColor(context.getResources().getColor(R.color.text_primary));
                textCol.addView(labelTv);

                TextView pkgTv = new TextView(context);
                pkgTv.setText(app.packageName);
                pkgTv.setTextSize(10);
                pkgTv.setTextColor(context.getResources().getColor(R.color.text_tertiary));
                pkgTv.setSingleLine(true);
                textCol.addView(pkgTv);

                row.addView(textCol);

                row.setOnClickListener(v -> {
                    callback.onSelected(app.packageName, app.label);
                    dialog.dismiss();
                });

                listContainer.addView(row);
            }
        };

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { refreshList.run(); }
        });

        refreshList.run();

        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.6f);
        }
        dialog.show();
    }

    private static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
