package com.bydlauncher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.bydlauncher.R;

public class VehicleDiagramView extends View {

    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bodyStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint windowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint doorHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean doorLF, doorRF, doorLR, doorRR, trunk, hood;

    public VehicleDiagramView(Context context) {
        super(context);
        initPaints();
    }

    public VehicleDiagramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public VehicleDiagramView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    private void initPaints() {
        bodyPaint.setColor(ContextCompat.getColor(getContext(), R.color.vehicle_body));
        bodyPaint.setStyle(Paint.Style.FILL);

        bodyStrokePaint.setColor(ContextCompat.getColor(getContext(), R.color.vehicle_body_stroke));
        bodyStrokePaint.setStyle(Paint.Style.STROKE);
        bodyStrokePaint.setStrokeWidth(3f);

        windowPaint.setColor(ContextCompat.getColor(getContext(), R.color.vehicle_window));
        windowPaint.setStyle(Paint.Style.FILL);

        wheelPaint.setColor(ContextCompat.getColor(getContext(), R.color.vehicle_wheel));
        wheelPaint.setStyle(Paint.Style.FILL);

        doorHighlightPaint.setColor(ContextCompat.getColor(getContext(), R.color.door_open));
        doorHighlightPaint.setStyle(Paint.Style.STROKE);
        doorHighlightPaint.setStrokeWidth(4f);
    }

    public void setDoorStates(boolean lf, boolean rf, boolean lr, boolean rr, boolean trk, boolean hd) {
        this.doorLF = lf;
        this.doorRF = rf;
        this.doorLR = lr;
        this.doorRR = rr;
        this.trunk = trk;
        this.hood = hd;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        float carW = w * 0.45f;
        float carH = h * 0.75f;
        float left = cx - carW / 2f;
        float top = cy - carH / 2f;
        float right = cx + carW / 2f;
        float bottom = cy + carH / 2f;

        float radius = carW * 0.2f;

        // 车身
        RectF bodyRect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(bodyRect, radius, radius, bodyPaint);
        canvas.drawRoundRect(bodyRect, radius, radius, bodyStrokePaint);

        // 车窗区域
        float winMargin = carW * 0.12f;
        float winTop = top + carH * 0.18f;
        float winBottom = top + carH * 0.45f;
        RectF windowRect = new RectF(left + winMargin, winTop, right - winMargin, winBottom);
        canvas.drawRoundRect(windowRect, radius * 0.6f, radius * 0.6f, windowPaint);

        // 后窗
        float rWinTop = top + carH * 0.6f;
        float rWinBottom = top + carH * 0.78f;
        RectF rearWindowRect = new RectF(left + winMargin, rWinTop, right - winMargin, rWinBottom);
        canvas.drawRoundRect(rearWindowRect, radius * 0.6f, radius * 0.6f, windowPaint);

        // 4 个车轮
        float wheelW = carW * 0.12f;
        float wheelH = carH * 0.1f;
        float wheelOffset = carW * 0.08f;

        // 左前轮
        canvas.drawRoundRect(new RectF(left - wheelOffset, top + carH * 0.15f, left - wheelOffset + wheelW, top + carH * 0.15f + wheelH), 4, 4, wheelPaint);
        // 右前轮
        canvas.drawRoundRect(new RectF(right + wheelOffset - wheelW, top + carH * 0.15f, right + wheelOffset, top + carH * 0.15f + wheelH), 4, 4, wheelPaint);
        // 左后轮
        canvas.drawRoundRect(new RectF(left - wheelOffset, bottom - carH * 0.15f - wheelH, left - wheelOffset + wheelW, bottom - carH * 0.15f), 4, 4, wheelPaint);
        // 右后轮
        canvas.drawRoundRect(new RectF(right + wheelOffset - wheelW, bottom - carH * 0.15f - wheelH, right + wheelOffset, bottom - carH * 0.15f), 4, 4, wheelPaint);

        // 车门高亮
        float doorStrokeInset = 4f;
        if (doorLF) {
            canvas.drawLine(left + doorStrokeInset, top + carH * 0.2f, left + doorStrokeInset, cy, doorHighlightPaint);
        }
        if (doorRF) {
            canvas.drawLine(right - doorStrokeInset, top + carH * 0.2f, right - doorStrokeInset, cy, doorHighlightPaint);
        }
        if (doorLR) {
            canvas.drawLine(left + doorStrokeInset, cy, left + doorStrokeInset, bottom - carH * 0.2f, doorHighlightPaint);
        }
        if (doorRR) {
            canvas.drawLine(right - doorStrokeInset, cy, right - doorStrokeInset, bottom - carH * 0.2f, doorHighlightPaint);
        }
        if (hood) {
            canvas.drawLine(left + carW * 0.2f, top + doorStrokeInset, right - carW * 0.2f, top + doorStrokeInset, doorHighlightPaint);
        }
        if (trunk) {
            canvas.drawLine(left + carW * 0.2f, bottom - doorStrokeInset, right - carW * 0.2f, bottom - doorStrokeInset, doorHighlightPaint);
        }
    }
}
