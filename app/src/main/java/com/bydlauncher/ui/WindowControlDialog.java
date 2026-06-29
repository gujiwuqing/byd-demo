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
    public static final int STATE_OPEN = 1;

    private final Context context;
    private final BydBodyworkApi bodyworkApi;
    private Dialog dialog;

    private int[] windowStates = {STATE_CLOSED, STATE_CLOSED, STATE_CLOSED, STATE_CLOSED};
    private boolean windowLocked = false;
    private boolean trunkOpen = false;
    private boolean hoodOpen = false;

    private TextView[] statusViews;
    private View[][] btnPairs; // [4][2] = up/down per window

    private TextView trunkStatus, trunkToggle;
    private TextView hoodStatus, hoodToggle;
    private View lockSwitch;

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
            dialog.getWindow().setDimAmount(0.5f);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.8),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        // 车窗状态和按钮
        statusViews = new TextView[]{
                view.findViewById(R.id.win_fl_status),
                view.findViewById(R.id.win_fr_status),
                view.findViewById(R.id.win_rl_status),
                view.findViewById(R.id.win_rr_status)
        };

        btnPairs = new View[][]{
                {view.findViewById(R.id.win_fl_up), view.findViewById(R.id.win_fl_down)},
                {view.findViewById(R.id.win_fr_up), view.findViewById(R.id.win_fr_down)},
                {view.findViewById(R.id.win_rl_up), view.findViewById(R.id.win_rl_down)},
                {view.findViewById(R.id.win_rr_up), view.findViewById(R.id.win_rr_down)},
        };

        // 车窗按钮绑定（升/降）
        int[] areas = {
                BydBodyworkApi.DOOR_LEFT_FRONT,
                BydBodyworkApi.DOOR_RIGHT_FRONT,
                BydBodyworkApi.DOOR_LEFT_REAR,
                BydBodyworkApi.DOOR_RIGHT_REAR
        };

        for (int i = 0; i < 4; i++) {
            final int windowIndex = i;
            final int area = areas[i];

            // 升 (close window = STATE_CLOSED)
            btnPairs[i][0].setOnClickListener(v -> {
                if (windowLocked) return;
                setWindowState(windowIndex, STATE_CLOSED);
                bodyworkApi.closeWindow(area);
            });

            // 降 (open window = STATE_OPEN)
            btnPairs[i][1].setOnClickListener(v -> {
                if (windowLocked) return;
                setWindowState(windowIndex, STATE_OPEN);
                bodyworkApi.openWindow(area);
            });
        }

        // 后备箱
        trunkStatus = view.findViewById(R.id.win_trunk_status);
        trunkToggle = view.findViewById(R.id.win_trunk_toggle);
        trunkToggle.setOnClickListener(v -> {
            if (trunkOpen) {
                bodyworkApi.closeTrunk();
            } else {
                bodyworkApi.openTrunk();
            }
            trunkOpen = !trunkOpen;
            refreshTrunkHood();
        });

        // 引擎盖
        hoodStatus = view.findViewById(R.id.win_hood_status);
        hoodToggle = view.findViewById(R.id.win_hood_toggle);
        hoodToggle.setOnClickListener(v -> {
            if (hoodOpen) {
                bodyworkApi.closeHood();
            } else {
                bodyworkApi.openHood();
            }
            hoodOpen = !hoodOpen;
            refreshTrunkHood();
        });

        // 车窗锁定开关
        lockSwitch = view.findViewById(R.id.win_lock_switch);
        lockSwitch.setOnClickListener(v -> {
            windowLocked = !windowLocked;
            refreshLockState();
        });

        // 全局控制
        view.findViewById(R.id.win_all_open).setOnClickListener(v -> {
            if (windowLocked) return;
            setAllWindows(STATE_OPEN);
            for (int i = 0; i < 4; i++) {
                bodyworkApi.openWindow(areas[i]);
            }
        });
        view.findViewById(R.id.win_all_close).setOnClickListener(v -> {
            if (windowLocked) return;
            setAllWindows(STATE_CLOSED);
            for (int i = 0; i < 4; i++) {
                bodyworkApi.closeWindow(areas[i]);
            }
        });

        // 关闭按钮
        view.findViewById(R.id.win_close_dialog).setOnClickListener(v -> dialog.dismiss());

        // 从 API 读取初始状态
        readInitialState();
        refreshUI();
        dialog.show();
    }

    private void readInitialState() {
        // 从 API 读取当前车窗状态
        windowStates[0] = bodyworkApi.getWindowFL();
        windowStates[1] = bodyworkApi.getWindowFR();
        windowStates[2] = bodyworkApi.getWindowRL();
        windowStates[3] = bodyworkApi.getWindowRR();

        trunkOpen = bodyworkApi.isTrunkOpen();
        hoodOpen = bodyworkApi.isHoodOpen();
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
        for (int i = 0; i < 4; i++) {
            boolean open = windowStates[i] == STATE_OPEN;
            statusViews[i].setText(open ? "已开启" : "已关闭");
            statusViews[i].setTextColor(ContextCompat.getColor(context,
                    open ? R.color.status_fair : R.color.status_good));

            // 高亮当前状态的按钮
            TextView upBtn = (TextView) btnPairs[i][0];
            TextView downBtn = (TextView) btnPairs[i][1];
            upBtn.setSelected(!open);
            downBtn.setSelected(open);
            upBtn.setTextColor(ContextCompat.getColor(context,
                    !open ? R.color.accent : R.color.text_primary));
            downBtn.setTextColor(ContextCompat.getColor(context,
                    open ? R.color.accent : R.color.text_primary));
        }

        refreshTrunkHood();
        refreshLockState();
    }

    private void refreshTrunkHood() {
        trunkStatus.setText(trunkOpen ? "已打开" : "已关闭");
        trunkStatus.setTextColor(ContextCompat.getColor(context,
                trunkOpen ? R.color.door_open : R.color.status_good));
        trunkToggle.setText(trunkOpen ? "关闭" : "打开");

        hoodStatus.setText(hoodOpen ? "已打开" : "已关闭");
        hoodStatus.setTextColor(ContextCompat.getColor(context,
                hoodOpen ? R.color.door_open : R.color.status_good));
        hoodToggle.setText(hoodOpen ? "关闭" : "打开");
    }

    private void refreshLockState() {
        // 锁定后所有车窗按钮变为禁用态（灰色）
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 2; j++) {
                btnPairs[i][j].setEnabled(!windowLocked);
                btnPairs[i][j].setAlpha(windowLocked ? 0.4f : 1.0f);
            }
        }
        // 全局按钮
        if (dialog != null) {
            View allOpen = dialog.findViewById(R.id.win_all_open);
            View allClose = dialog.findViewById(R.id.win_all_close);
            if (allOpen != null) {
                allOpen.setEnabled(!windowLocked);
                allOpen.setAlpha(windowLocked ? 0.4f : 1.0f);
            }
            if (allClose != null) {
                allClose.setEnabled(!windowLocked);
                allClose.setAlpha(windowLocked ? 0.4f : 1.0f);
            }
        }

        // 锁定开关视觉状态
        if (lockSwitch instanceof TextView) {
            TextView lockText = (TextView) lockSwitch;
            lockText.setText(windowLocked ? "ON" : "OFF");
            lockText.setTextColor(ContextCompat.getColor(context,
                    windowLocked ? R.color.accent : R.color.text_tertiary));
            lockText.setTextSize(10);
        }
    }

    public void updateFromStatus(int fl, int fr, int rl, int rr,
                                  boolean trunkIsOpen, boolean hoodIsOpen) {
        windowStates[0] = fl;
        windowStates[1] = fr;
        windowStates[2] = rl;
        windowStates[3] = rr;
        trunkOpen = trunkIsOpen;
        hoodOpen = hoodIsOpen;
        if (dialog != null && dialog.isShowing()) {
            refreshUI();
        }
    }
}
