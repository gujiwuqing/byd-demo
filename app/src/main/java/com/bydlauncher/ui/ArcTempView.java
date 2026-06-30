package com.bydlauncher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ArcTempView extends View {

    private static final int TEMP_MIN = 17;
    private static final int TEMP_MAX = 33;
    private static final float START_ANGLE = 180f;
    private static final float SWEEP_ANGLE = 180f;

    private final Paint bgArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activeDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tempTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint unitTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint offTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int currentTemp = 22;
    private boolean acOn = true;

    public ArcTempView(Context context) {
        super(context);
        init();
    }

    public ArcTempView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        bgArcPaint.setStyle(Paint.Style.STROKE);
        bgArcPaint.setStrokeWidth(8 * density);
        bgArcPaint.setStrokeCap(Paint.Cap.ROUND);
        bgArcPaint.setColor(0x33FFFFFF);

        progressArcPaint.setStyle(Paint.Style.STROKE);
        progressArcPaint.setStrokeWidth(10 * density);
        progressArcPaint.setStrokeCap(Paint.Cap.ROUND);
        progressArcPaint.setColor(0xFF00C8F0);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(0x55FFFFFF);

        activeDotPaint.setStyle(Paint.Style.FILL);
        activeDotPaint.setColor(0xFFFFFFFF);
        activeDotPaint.setShadowLayer(8 * density, 0, 0, 0x4400C8F0);

        tempTextPaint.setTextAlign(Paint.Align.CENTER);
        tempTextPaint.setTextSize(32 * density);
        tempTextPaint.setFakeBoldText(true);
        tempTextPaint.setColor(0xFFF0F4FF);

        unitTextPaint.setTextAlign(Paint.Align.CENTER);
        unitTextPaint.setTextSize(13 * density);
        unitTextPaint.setColor(0xFF00C8F0);

        offTextPaint.setTextAlign(Paint.Align.CENTER);
        offTextPaint.setTextSize(13 * density);
        offTextPaint.setColor(0x8080909A);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setTemp(int temp) {
        this.currentTemp = Math.max(TEMP_MIN, Math.min(TEMP_MAX, temp));
        invalidate();
    }

    public void setAcOn(boolean on) {
        this.acOn = on;
        progressArcPaint.setColor(on ? 0xFF00C8F0 : 0x3380909A);
        activeDotPaint.setColor(on ? 0xFFFFFFFF : 0x6680909A);
        tempTextPaint.setColor(on ? 0xFFF0F4FF : 0x8080909A);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float cx = getWidth() / 2f;
        float cy = getHeight() * 0.72f;
        float radius = Math.min(getWidth(), getHeight()) * 0.42f;

        RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        canvas.drawArc(oval, START_ANGLE, SWEEP_ANGLE, false, bgArcPaint);

        float progress = (float)(currentTemp - TEMP_MIN) / (TEMP_MAX - TEMP_MIN);
        canvas.drawArc(oval, START_ANGLE, SWEEP_ANGLE * progress, false, progressArcPaint);

        int totalSteps = TEMP_MAX - TEMP_MIN;
        for (int i = 0; i <= totalSteps; i++) {
            float angle = (float) Math.toRadians(START_ANGLE + (SWEEP_ANGLE * i / totalSteps));
            float dotX = (float)(cx + radius * Math.cos(angle));
            float dotY = (float)(cy + radius * Math.sin(angle));
            if (i == currentTemp - TEMP_MIN) {
                canvas.drawCircle(dotX, dotY, 6 * density, activeDotPaint);
            } else {
                canvas.drawCircle(dotX, dotY, 3 * density, dotPaint);
            }
        }

        float textY = cy - (tempTextPaint.descent() + tempTextPaint.ascent()) / 2f - 10 * density;
        canvas.drawText(String.valueOf(currentTemp), cx, textY, tempTextPaint);

        float unitY = textY + 28 * density;
        canvas.drawText(acOn ? "°C" : "已关闭", cx, unitY, acOn ? unitTextPaint : offTextPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, (int)(width * 0.55f));
    }
}
