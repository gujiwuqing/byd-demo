package com.diui.launcher.ui;

import android.view.MotionEvent;

public class ThreeFingerGestureDetector {

    public interface Callback {
        void onSwipeDown();
        void onSwipeUp();
        void onSwipeLeft();
        void onSwipeRight();
    }

    private static final float THRESHOLD_DP = 100f;

    private final Callback callback;
    private final float threshold;
    private boolean tracking = false;
    private float startX, startY;
    private boolean consumed = false;

    public ThreeFingerGestureDetector(Callback callback, float density) {
        this.callback = callback;
        this.threshold = THRESHOLD_DP * density;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerCount == 3 && !tracking) {
                    tracking = true;
                    consumed = false;
                    startX = avgX(event);
                    startY = avgY(event);
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (tracking && pointerCount >= 3 && !consumed) {
                    float dx = avgX(event) - startX;
                    float dy = avgY(event) - startY;

                    if (Math.abs(dy) > threshold && Math.abs(dy) > Math.abs(dx)) {
                        consumed = true;
                        if (dy > 0) callback.onSwipeDown();
                        else callback.onSwipeUp();
                        return true;
                    }
                    if (Math.abs(dx) > threshold && Math.abs(dx) > Math.abs(dy)) {
                        consumed = true;
                        if (dx > 0) callback.onSwipeRight();
                        else callback.onSwipeLeft();
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                consumed = false;
                break;
        }
        return false;
    }

    private float avgX(MotionEvent e) {
        float sum = 0;
        int count = Math.min(e.getPointerCount(), 3);
        for (int i = 0; i < count; i++) sum += e.getX(i);
        return sum / count;
    }

    private float avgY(MotionEvent e) {
        float sum = 0;
        int count = Math.min(e.getPointerCount(), 3);
        for (int i = 0; i < count; i++) sum += e.getY(i);
        return sum / count;
    }
}
