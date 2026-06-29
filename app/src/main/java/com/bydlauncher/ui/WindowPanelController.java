package com.bydlauncher.ui;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;
import com.bydlauncher.api.BydBodyworkApi;

public class WindowPanelController {

    private static final int STATE_CLOSED = 0;
    private static final int STATE_OPEN = 1;

    private static int[] windowStates = {STATE_CLOSED, STATE_CLOSED, STATE_CLOSED, STATE_CLOSED};
    private static boolean windowLocked = false;
    private static boolean trunkOpen = false;
    private static boolean hoodOpen = false;

    // 车门状态（用于车辆示意图）
    private static boolean doorLF, doorRF, doorLR, doorRR;

    private static BydBodyworkApi api;

    private static TextView[] statusViews;
    private static View[][] btnPairs;
    private static TextView trunkStatus, trunkToggle;
    private static TextView hoodStatus, hoodToggle;
    private static TextView lockSwitch;
    private static View allOpenBtn, allCloseBtn;
    private static VehicleDiagramView vehicleDiagram;

    public static void bind(View panel, BydBodyworkApi bodyworkApi) {
        api = bodyworkApi;

        statusViews = new TextView[]{
                panel.findViewById(R.id.win_fl_status),
                panel.findViewById(R.id.win_fr_status),
                panel.findViewById(R.id.win_rl_status),
                panel.findViewById(R.id.win_rr_status)
        };

        btnPairs = new View[][]{
                {panel.findViewById(R.id.win_fl_up), panel.findViewById(R.id.win_fl_down)},
                {panel.findViewById(R.id.win_fr_up), panel.findViewById(R.id.win_fr_down)},
                {panel.findViewById(R.id.win_rl_up), panel.findViewById(R.id.win_rl_down)},
                {panel.findViewById(R.id.win_rr_up), panel.findViewById(R.id.win_rr_down)},
        };

        trunkStatus = panel.findViewById(R.id.win_trunk_status);
        trunkToggle = panel.findViewById(R.id.win_trunk_toggle);
        hoodStatus = panel.findViewById(R.id.win_hood_status);
        hoodToggle = panel.findViewById(R.id.win_hood_toggle);
        lockSwitch = panel.findViewById(R.id.win_lock_switch);
        allOpenBtn = panel.findViewById(R.id.win_all_open);
        allCloseBtn = panel.findViewById(R.id.win_all_close);
        vehicleDiagram = panel.findViewById(R.id.win_vehicle_diagram);

        int[] areas = {
                BydBodyworkApi.DOOR_LEFT_FRONT,
                BydBodyworkApi.DOOR_RIGHT_FRONT,
                BydBodyworkApi.DOOR_LEFT_REAR,
                BydBodyworkApi.DOOR_RIGHT_REAR
        };

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final int area = areas[i];

            btnPairs[i][0].setOnClickListener(v -> {
                if (windowLocked) return;
                windowStates[idx] = STATE_CLOSED;
                if (api != null) api.closeWindow(area);
                refreshAll(panel);
            });

            btnPairs[i][1].setOnClickListener(v -> {
                if (windowLocked) return;
                windowStates[idx] = STATE_OPEN;
                if (api != null) api.openWindow(area);
                refreshAll(panel);
            });
        }

        trunkToggle.setOnClickListener(v -> {
            if (trunkOpen) {
                if (api != null) api.closeTrunk();
            } else {
                if (api != null) api.openTrunk();
            }
            trunkOpen = !trunkOpen;
            refreshTrunkHood(panel);
        });

        hoodToggle.setOnClickListener(v -> {
            if (hoodOpen) {
                if (api != null) api.closeHood();
            } else {
                if (api != null) api.openHood();
            }
            hoodOpen = !hoodOpen;
            refreshTrunkHood(panel);
        });

        lockSwitch.setOnClickListener(v -> {
            windowLocked = !windowLocked;
            refreshLockState(panel);
        });

        allOpenBtn.setOnClickListener(v -> {
            if (windowLocked) return;
            for (int i = 0; i < 4; i++) {
                windowStates[i] = STATE_OPEN;
                if (api != null) api.openWindow(areas[i]);
            }
            refreshAll(panel);
        });

        allCloseBtn.setOnClickListener(v -> {
            if (windowLocked) return;
            for (int i = 0; i < 4; i++) {
                windowStates[i] = STATE_CLOSED;
                if (api != null) api.closeWindow(areas[i]);
            }
            refreshAll(panel);
        });

        readInitialState();
        refreshAll(panel);
    }

    private static void readInitialState() {
        if (api == null) return;
        windowStates[0] = api.getWindowFL();
        windowStates[1] = api.getWindowFR();
        windowStates[2] = api.getWindowRL();
        windowStates[3] = api.getWindowRR();
        trunkOpen = api.isTrunkOpen();
        hoodOpen = api.isHoodOpen();
        doorLF = api.isDoorOpen(BydBodyworkApi.DOOR_LEFT_FRONT);
        doorRF = api.isDoorOpen(BydBodyworkApi.DOOR_RIGHT_FRONT);
        doorLR = api.isDoorOpen(BydBodyworkApi.DOOR_LEFT_REAR);
        doorRR = api.isDoorOpen(BydBodyworkApi.DOOR_RIGHT_REAR);
    }

    public static void updateFromStatus(int fl, int fr, int rl, int rr,
                                         boolean trunkIsOpen, boolean hoodIsOpen,
                                         boolean dLF, boolean dRF, boolean dLR, boolean dRR) {
        windowStates[0] = fl;
        windowStates[1] = fr;
        windowStates[2] = rl;
        windowStates[3] = rr;
        trunkOpen = trunkIsOpen;
        hoodOpen = hoodIsOpen;
        doorLF = dLF;
        doorRF = dRF;
        doorLR = dLR;
        doorRR = dRR;
    }

    private static void refreshAll(View panel) {
        for (int i = 0; i < 4; i++) {
            boolean open = windowStates[i] == STATE_OPEN;
            statusViews[i].setText(open ? "已开启" : "已关闭");
            statusViews[i].setTextColor(ContextCompat.getColor(panel.getContext(),
                    open ? R.color.status_fair : R.color.status_good));

            TextView upBtn = (TextView) btnPairs[i][0];
            TextView downBtn = (TextView) btnPairs[i][1];
            upBtn.setSelected(!open);
            downBtn.setSelected(open);
            upBtn.setTextColor(ContextCompat.getColor(panel.getContext(),
                    !open ? R.color.accent : R.color.text_primary));
            downBtn.setTextColor(ContextCompat.getColor(panel.getContext(),
                    open ? R.color.accent : R.color.text_primary));
        }
        refreshTrunkHood(panel);
        refreshLockState(panel);
        if (vehicleDiagram != null) {
            vehicleDiagram.setDoorStates(doorLF, doorRF, doorLR, doorRR, trunkOpen, hoodOpen);
        }
    }

    private static void refreshTrunkHood(View panel) {
        trunkStatus.setText(trunkOpen ? "已打开" : "已关闭");
        trunkStatus.setTextColor(ContextCompat.getColor(panel.getContext(),
                trunkOpen ? R.color.door_open : R.color.status_good));
        trunkToggle.setText(trunkOpen ? "关闭" : "打开");

        hoodStatus.setText(hoodOpen ? "已打开" : "已关闭");
        hoodStatus.setTextColor(ContextCompat.getColor(panel.getContext(),
                hoodOpen ? R.color.door_open : R.color.status_good));
        hoodToggle.setText(hoodOpen ? "关闭" : "打开");
    }

    private static void refreshLockState(View panel) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 2; j++) {
                btnPairs[i][j].setEnabled(!windowLocked);
                btnPairs[i][j].setAlpha(windowLocked ? 0.4f : 1.0f);
            }
        }
        allOpenBtn.setEnabled(!windowLocked);
        allOpenBtn.setAlpha(windowLocked ? 0.4f : 1.0f);
        allCloseBtn.setEnabled(!windowLocked);
        allCloseBtn.setAlpha(windowLocked ? 0.4f : 1.0f);

        lockSwitch.setText(windowLocked ? "ON" : "OFF");
        lockSwitch.setTextColor(ContextCompat.getColor(panel.getContext(),
                windowLocked ? R.color.accent : R.color.text_tertiary));
        lockSwitch.setTextSize(10);
    }
}
