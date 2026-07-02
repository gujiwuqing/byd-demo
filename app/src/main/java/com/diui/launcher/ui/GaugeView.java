package com.diui.launcher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.diui.launcher.R;

/**
 * 环形进度 View,用于数据可视化(电量/油量/能耗等)。
 * setProgress(value, max) 设定进度;setColorByRange 自动按阈值变色。
 */
public class GaugeView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float progress = 0f;
    private float max = 100f;

    public GaugeView(Context context) { super(context); init(); }
    public GaugeView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public GaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.gauge_track));
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(12f);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint.setColor(ContextCompat.getColor(getContext(), R.color.gauge_progress));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(12f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(float value, float max) {
        this.progress = Math.max(0f, Math.min(value, max));
        this.max = max;
        // 按比例变色:<30% 红,<60% 黄,否则绿
        float ratio = max > 0 ? progress / max : 0f;
        int color;
        if (ratio < 0.3f) color = ContextCompat.getColor(getContext(), R.color.battery_low);
        else if (ratio < 0.6f) color = ContextCompat.getColor(getContext(), R.color.battery_mid);
        else color = ContextCompat.getColor(getContext(), R.color.battery_high);
        progressPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pad = 12f;
        RectF rect = new RectF(pad, pad, getWidth() - pad, getHeight() - pad);
        canvas.drawArc(rect, -90, 360, false, trackPaint);
        float sweep = max > 0 ? 360f * (progress / max) : 0f;
        canvas.drawArc(rect, -90, sweep, false, progressPaint);
    }
}
