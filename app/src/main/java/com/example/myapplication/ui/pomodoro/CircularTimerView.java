package com.example.myapplication.ui.pomodoro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularTimerView extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private RectF rectF;
    private float progressRatio = 1.0f; // 1.0 = Full 100%, 0.0 = Empty 0%

    public CircularTimerView(Context context) {
        super(context);
        init(context);
    }

    public CircularTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CircularTimerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        float strokeWidth = 8f * density;

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(0xFFE2E8F0);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(0xFF2563EB); // Màu xanh biển
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        rectF = new RectF();
    }

    public void setProgressRatio(float ratio) {
        this.progressRatio = Math.max(0.0f, Math.min(1.0f, ratio));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        float margin = 12f * density;
        rectF.set(margin, margin, getWidth() - margin, getHeight() - margin);

        // 1. Vẽ vòng xám nhạt nền
        canvas.drawOval(rectF, backgroundPaint);

        // 2. Vẽ vòng xanh đếm lùi rút ngắn dần theo thời gian còn lại (bắt đầu từ đỉnh 12h = -90 độ)
        float sweepAngle = progressRatio * 360f;
        if (sweepAngle > 0) {
            canvas.drawArc(rectF, -90, sweepAngle, false, progressPaint);
        }
    }
}
