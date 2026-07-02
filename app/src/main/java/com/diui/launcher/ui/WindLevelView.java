package com.diui.launcher.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.diui.launcher.R;

/**
 * 风量等级条(0-7),选中级别高亮。替代 8 个文字方块。
 */
public class WindLevelView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int level = 0;
    private final int maxLevel = 7;

    public WindLevelView(Context context) { super(context); init(); }
    public WindLevelView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public WindLevelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        barPaint.setStyle(Paint.Style.FILL);
    }

    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(level, maxLevel));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float gap = 6f;
        float barW = (w - gap * (maxLevel)) / (maxLevel + 1);
        for (int i = 0; i <= maxLevel; i++) {
            float left = i * (barW + gap);
            float barH = h * ((i + 1) / (float) (maxLevel + 1));
            barPaint.setColor(ContextCompat.getColor(getContext(),
                    i <= level ? R.color.accent : R.color.gauge_track));
            canvas.drawRect(left, h - barH, left + barW, h, barPaint);
        }
    }
}
