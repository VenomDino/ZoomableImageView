package imageview.zoomableimageview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.animation.ValueAnimator;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {

    private static final float DOUBLE_TAP_SCALE_FACTOR = 2.0f;

    private final ScaleGestureDetector mScaleGestureDetector;
    private final GestureDetector mGestureDetector;
    private float mScaleFactor = 1.0f;
    private float mFocusX = 0.0f;
    private float mFocusY = 0.0f;
    private final Matrix mMatrix = new Matrix();
    private ScaleType mInitialScaleType;

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());
        mInitialScaleType = getScaleType();
        super.setScaleType(ScaleType.MATRIX);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        mInitialScaleType = scaleType;
        if (scaleType != ScaleType.MATRIX) {
            super.setScaleType(scaleType);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mScaleFactor = 1.0f;
        mFocusX = w / 2f;
        mFocusY = h / 2f;
        updateMatrix();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean isClick = ev.getAction() == MotionEvent.ACTION_UP &&
                ev.getDownTime() - ev.getEventTime() < ViewConfiguration.getTapTimeout();

        boolean handled = mScaleGestureDetector.onTouchEvent(ev);
        handled |= mGestureDetector.onTouchEvent(ev);

        if (isClick && !handled) {
            performClick();
            handled = true;
        }

        return handled || super.onTouchEvent(ev);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 10.0f));

            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();

            mFocusX += (focusX - mFocusX) * (1 - 1 / detector.getScaleFactor());
            mFocusY += (focusY - mFocusY) * (1 - 1 / detector.getScaleFactor());

            updateMatrix();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            if (mScaleFactor > 1.0f) {
                mFocusX -= distanceX;
                mFocusY -= distanceY;
                constrainPan();
                updateMatrix();
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float newScale;
            float focusX = e.getX();
            float focusY = e.getY();

            if (mScaleFactor > 1.0f) {
                newScale = 1.0f;
            } else {
                newScale = DOUBLE_TAP_SCALE_FACTOR;
            }

            zoomTo(newScale, focusX, focusY);
            return true;
        }
    }

    private void updateMatrix() {
        if (mScaleFactor == 1.0f) {
            super.setScaleType(mInitialScaleType);
            return;
        } else {
            super.setScaleType(ScaleType.MATRIX);
        }

        mMatrix.reset();
        mMatrix.postTranslate((float) -getWidth() / 2, (float) -getHeight() / 2);
        mMatrix.postScale(mScaleFactor, mScaleFactor);
        mMatrix.postTranslate(mFocusX, mFocusY);
        setImageMatrix(mMatrix);
    }

    private void zoomTo(float targetScale, float focusX, float focusY) {
        final float startScale = mScaleFactor;
        final float startFocusX = mFocusX;
        final float startFocusY = mFocusY;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300);

        animator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            mScaleFactor = startScale + t * (targetScale - startScale);

            if (targetScale > startScale) {
                float newFocusX = (float) getWidth() / 2 - ((float) getWidth() / 2 - focusX) * targetScale / startScale;
                float newFocusY = (float) getHeight() / 2 - ((float) getHeight() / 2 - focusY) * targetScale / startScale;
                mFocusX = startFocusX + t * (newFocusX - startFocusX);
                mFocusY = startFocusY + t * (newFocusY - startFocusY);
            } else {
                mFocusX = startFocusX + t * ((float) getWidth() / 2 - startFocusX);
                mFocusY = startFocusY + t * ((float) getHeight() / 2 - startFocusY);
            }

            constrainPan();
            updateMatrix();
        });

        animator.start();
    }

    private void constrainPan() {
        Drawable drawable = getDrawable();
        if (drawable == null) return;

        float imageWidth = drawable.getIntrinsicWidth() * mScaleFactor;
        float imageHeight = drawable.getIntrinsicHeight() * mScaleFactor;

        float maxPanX = Math.max(0, (imageWidth - getWidth()) / 2);
        float maxPanY = Math.max(0, (imageHeight - getHeight()) / 2);

        mFocusX = Math.min(Math.max(mFocusX, (float) getWidth() / 2 - maxPanX), (float) getWidth() / 2 + maxPanX);
        mFocusY = Math.min(Math.max(mFocusY, (float) getHeight() / 2 - maxPanY), (float) getHeight() / 2 + maxPanY);
    }
}
