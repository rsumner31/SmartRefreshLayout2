package com.scwang.smartrefreshlayout.header.bezier;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * cjj
 */
public class RippleView extends View {

    private int mRadius;
    private Paint mPaint;
    private ValueAnimator mAnimator;

    public RippleView(Context context) {
        super(context);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(0xffffffff);
        mPaint.setStyle(Paint.Style.FILL);
    }

    public void setFrontColor(int color) {
        mPaint.setColor(color);
    }

    public void startReveal() {
        setVisibility(VISIBLE);
        if (mAnimator == null) {
            int bigRadius = (int) (Math.sqrt(Math.pow(getHeight(), 2) + Math.pow(getWidth(), 2)));
            mAnimator = ValueAnimator.ofInt(0, bigRadius);
            mAnimator.setDuration(400);
            mAnimator.addUpdateListener(animation -> {
                mRadius = (int) animation.getAnimatedValue();
                invalidate();
            });
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(GONE);
                }
            });
        }
        mAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, mRadius, mPaint);
    }

}
