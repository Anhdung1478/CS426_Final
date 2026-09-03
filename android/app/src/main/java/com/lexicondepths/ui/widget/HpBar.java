package com.lexicondepths.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.lexicondepths.R;

/** The one justified custom View in the kit — segmented terminal HP bar, needs onDraw. */
public class HpBar extends View {

    private static final int SEGMENTS = 20;
    private static final float LOW_HEALTH_RATIO = 0.3f;

    private int max = 100;
    private int value = 100;
    private final Paint filledPaint = new Paint();
    private final Paint emptyPaint = new Paint();

    public HpBar(Context context) {
        this(context, null);
    }

    public HpBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        emptyPaint.setColor(ContextCompat.getColor(context, R.color.surface));
        updateFilledColor();
    }

    public void setValues(int value, int max) {
        this.value = Math.max(0, value);
        this.max = Math.max(1, max);
        updateFilledColor();
        // §7: a colour-coded state must never be the only way to read a value. The green-to-red
        // switch below is exactly that, so the bar carries its own numbers for TalkBack, and
        // every screen using it also prints HP as text.
        setContentDescription(getContext().getString(R.string.cd_hp_bar, this.value, this.max));
        invalidate();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(android.widget.ProgressBar.class.getName());
    }

    private void updateFilledColor() {
        float ratio = (float) value / max;
        int colorRes = ratio <= LOW_HEALTH_RATIO ? R.color.hp_low : R.color.hp_full;
        filledPaint.setColor(ContextCompat.getColor(getContext(), colorRes));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float ratio = (float) value / max;
        int filledSegments = Math.round(ratio * SEGMENTS);
        float segmentWidth = (float) getWidth() / SEGMENTS;
        float gap = segmentWidth * 0.12f;
        for (int i = 0; i < SEGMENTS; i++) {
            float left = i * segmentWidth + gap / 2;
            float right = (i + 1) * segmentWidth - gap / 2;
            canvas.drawRect(left, 0, right, getHeight(), i < filledSegments ? filledPaint : emptyPaint);
        }
    }
}
