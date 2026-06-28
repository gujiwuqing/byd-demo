package com.bydlauncher.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;
import com.bydlauncher.api.BydBodyworkApi;

public class WindowControlDialog {

    public static final int STATE_CLOSED = 0;
    public static final int STATE_HALF = 1;
    public static final int STATE_OPEN = 2;

    private final Context context;
    private final BydBodyworkApi bodyworkApi;
    private Dialog dialog;

    private int[] windowStates = {STATE_CLOSED, STATE_CLOSED, STATE_CLOSED, STATE_CLOSED};

    private TextView[] statusViews;
    private TextView[][] btnGroups;

    public WindowControlDialog(Context context, BydBodyworkApi bodyworkApi) {
        this.context = context;
        this.bodyworkApi = bodyworkApi;
    }

    public void show() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_window_control, null);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.6),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        statusViews = new TextView[]{
                view.findViewById(R.id.win_fl_status),
                view.findViewById(R.id.win_fr_status),
                view.findViewById(R.id.win_rl_status),
                view.findViewById(R.id.win_rr_status)
        };

        btnGroups = new TextView[][]{
                {view.findViewById(R.id.win_fl_open), view.findViewById(R.id.win_fl_half), view.findViewById(R.id.win_fl_close)},
                {view.findViewById(R.id.win_fr_open), view.findViewById(R.id.win_fr_half), view.findViewById(R.id.win_fr_close)},
                {view.findViewById(R.id.win_rl_open), view.findViewById(R.id.win_rl_half), view.findViewById(R.id.win_rl_close)},
                {view.findViewById(R.id.win_rr_open), view.findViewById(R.id.win_rr_half), view.findViewById(R.id.win_rr_close)},
        };

        for (int i = 0; i < 4; i++) {
            final int windowIndex = i;
            btnGroups[i][0].setOnClickListener(v -> setWindowState(windowIndex, STATE_OPEN));
            btnGroups[i][1].setOnClickListener(v -> setWindowState(windowIndex, STATE_HALF));
            btnGroups[i][2].setOnClickListener(v -> setWindowState(windowIndex, STATE_CLOSED));
        }

        view.findViewById(R.id.win_all_open).setOnClickListener(v -> setAllWindows(STATE_OPEN));
        view.findViewById(R.id.win_all_half).setOnClickListener(v -> setAllWindows(STATE_HALF));
        view.findViewById(R.id.win_all_close).setOnClickListener(v -> setAllWindows(STATE_CLOSED));
        view.findViewById(R.id.win_close_dialog).setOnClickListener(v -> dialog.dismiss());

        refreshUI();
        dialog.show();
    }

    private void setWindowState(int index, int state) {
        windowStates[index] = state;
        refreshUI();
    }

    private void setAllWindows(int state) {
        for (int i = 0; i < 4; i++) {
            windowStates[i] = state;
        }
        refreshUI();
    }

    private void refreshUI() {
        String[] stateTexts = {"已关闭", "半开", "已开启"};
        int[] stateColors = {R.color.status_good, R.color.status_fair, R.color.status_poor};

        for (int i = 0; i < 4; i++) {
            int state = windowStates[i];
            statusViews[i].setText(stateTexts[state]);
            statusViews[i].setTextColor(ContextCompat.getColor(context, stateColors[state]));

            for (int j = 0; j < 3; j++) {
                int btnState = 2 - j;
                boolean active = (state == btnState);
                btnGroups[i][j].setSelected(active);
                btnGroups[i][j].setTextColor(ContextCompat.getColor(context,
                        active ? R.color.accent : R.color.text_primary));
            }
        }
    }
}
