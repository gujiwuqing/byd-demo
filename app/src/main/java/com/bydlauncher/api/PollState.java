package com.bydlauncher.api;

public enum PollState {
    DRIVING(1000),
    PARKED(5000),
    CHARGING(5000),
    IDLE(30000);

    public final long intervalMs;

    PollState(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public static PollState classify(int speed, int gear, int chargeGunState, boolean dataValid) {
        if (!dataValid) return IDLE;
        if (chargeGunState > 0) return CHARGING;
        if (speed > 0) return DRIVING;
        return PARKED;
    }
}
