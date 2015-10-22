
package com.fengbin.panningview;

import java.lang.ref.WeakReference;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

public class PanningViewAttacher implements ViewTreeObserver.OnGlobalLayoutListener {

    public static final int DEFAULT_PANNING_DURATION_IN_MS = 5000;

    private static final String TAG = "PanningViewAttacher";

    private enum Way {
        R2L, L2R, T2B, B2T
    };

    private WeakReference<ImageView> mImageView;

    private int mIvTop, mIvRight, mIvBottom, mIvLeft;

    private ViewTreeObserver mViewTreeObserver;

    private Matrix mMatrix;

    private RectF mDisplayRect = new RectF();

    private ValueAnimator mCurrentAnimator;

    private LinearInterpolator mLinearInterpolator;// 以常量速率改变

    private boolean mIsPortrait;// 是否是竖向的

    private long mDuration;

    private long mCurrentPlayTime;

    private long mTotalTime;

    private Way mWay;

    private boolean mIsPanning;

    /**
     * 构造器
     * 
     * @param imageView
     * @param duration
     */
    public PanningViewAttacher(ImageView imageView, long duration) {
        if (imageView == null) {
            throw new IllegalArgumentException("imageView must not be null");
        }
        if (!hasDrawable(imageView)) {
            throw new IllegalArgumentException("drawable must not be null");
        }

        // LinearInterpolator 动画从开始到结束,变化率是线性变化,常量速率改变
        mLinearInterpolator = new LinearInterpolator();
        mDuration = duration;
        mImageView = new WeakReference<ImageView>(imageView);

        // 在oncreate中View.getWidth和View.getHeight无法获得一个view的高度和宽度，
        // 这是因为View组件布局要在onResume回调后完成。
        // 所以现在需要使用getViewTreeObserver().addOnGlobalLayoutListener()来获得宽度或者高度。
        // 这是获得一个view的宽度和高度的方法之一。
        // ViewTreeObserver不能直接实例化，而是通过getViewTreeObserver()获得。
        mViewTreeObserver = imageView.getViewTreeObserver();

        // OnGlobalLayoutListener 是ViewTreeObserver的内部类，
        // 当一个视图树的布局发生改变时，可以被ViewTreeObserver监听到，
        // 这里注册监听视图树的观察者(observer)，在视图树的全局事件改变时得到通知。
        mViewTreeObserver.addOnGlobalLayoutListener(this);

        // ScaleTypeMatrix 用矩阵来绘制图片(从左上角起始的矩阵区域)，
        setImageViewScaleTypeMatrix(imageView);

        // 如果用Matrix matrix = mImage.getImageMatrix(); matrix只是得到一个对象的引用
        // 应该用Matrix matrix = new Matrix (mImage.getImageMatrix());这样才是得到一个克隆对象
        mMatrix = imageView.getImageMatrix();
        if (mMatrix == null) {
            mMatrix = new Matrix();
        }

        // getConfiguration().orientation 获得当前资源的方向
        // getRequestedOrientation()获得当前请求的方向
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);设置屏幕方向
        mIsPortrait = imageView.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        update();
    }

    /**
	 *
	 */
    public void update() {
        mWay = null;
        mTotalTime = 0;
        mCurrentPlayTime = 0;

        // Causes the Runnable to be added to the message queue.
        getImageView().post(new Runnable() {
            @Override
            public void run() {
                scale();
                refreshDisplayRect();
            }
        });
    }

    // 是否在显示
    public boolean isPanning() {
        return mIsPanning;
    }

    /**
     * scale and start to pan the image background
     */
    public void startPanning() {
        if (mIsPanning) {
            return;
        }
        mIsPanning = true;
        final Runnable panningRunnable = new Runnable() {
            @Override
            public void run() {
                animate_();
            }
        };
        getImageView().post(panningRunnable);
    }

    /**
     * stop current panning
     */
    public void stopPanning() {
        if (!mIsPanning) {
            return;
        }
        mIsPanning = false;
        Log.d(TAG, "panning animation stopped by user");

        //
        if (mCurrentAnimator != null) {
            mCurrentAnimator.removeAllListeners();
            mCurrentAnimator.cancel();
            mCurrentAnimator = null;
        }
        mTotalTime += mCurrentPlayTime;
        Log.d(TAG, "mTotalTime : " + mTotalTime);
    }

    /**
     * Clean-up the resources attached to this object. This needs to be called when the ImageView is
     * no longer used. A good example is from {@link android.view.View#onDetachedFromWindow()} or
     * from {@link android.app.Activity#onDestroy()}. This is automatically called if you are using
     * {@link com.fourmob.panningview.PanningView}.
     */
    public final void cleanup() {
        if (null != mImageView) {
            getImageView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
        mViewTreeObserver = null;

        stopPanning();

        // Finally, clear ImageView
        mImageView = null;
    }

    public final ImageView getImageView() {
        ImageView imageView = null;

        if (null != mImageView) {
            imageView = mImageView.get();
        }

        // If we don't have an ImageView, call cleanup()
        if (null == imageView) {
            cleanup();
            throw new IllegalStateException(
                    "ImageView no longer exists. You should not use this PanningViewAttacher any more.");
        }

        return imageView;
    }

    private int getDrawableIntrinsicHeight() {
        return getImageView().getDrawable().getIntrinsicHeight();
    }

    private int getDrawableIntrinsicWidth() {
        return getImageView().getDrawable().getIntrinsicWidth();
    }

    private int getImageViewWidth() {
        return getImageView().getWidth();
    }

    private int getImageViewHeight() {
        return getImageView().getHeight();
    }

    /**
     * Set's the ImageView's ScaleType to Matrix.
     */
    private static void setImageViewScaleTypeMatrix(ImageView imageView) {
        if (null != imageView && !(imageView instanceof PanningView)) {
            imageView.setScaleType(ImageView.ScaleType.MATRIX);
        }
    }

    /**
     * @return true if the ImageView exists, and it's Drawable exists
     */
    private static boolean hasDrawable(ImageView imageView) {
        return null != imageView && null != imageView.getDrawable();
    }

    @Override
    public void onGlobalLayout() {
        ImageView imageView = getImageView();

        if (null != imageView) {
            final int top = imageView.getTop();
            final int right = imageView.getRight();
            final int bottom = imageView.getBottom();
            final int left = imageView.getLeft();

            /**
             * We need to check whether the ImageView's bounds have changed. This would be easier if
             * we targeted API 11+ as we could just use View.OnLayoutChangeListener. Instead we have
             * to replicate the work, keeping track of the ImageView's bounds and then checking if
             * the values change.
             */
            if (top != mIvTop || bottom != mIvBottom || left != mIvLeft || right != mIvRight) {
                update();

                // Update values as something has changed
                mIvTop = top;
                mIvRight = right;
                mIvBottom = bottom;
                mIvLeft = left;
            }
        }
    }

    private void animate_() {
        refreshDisplayRect();
        if (mWay == null) {
            mWay = mIsPortrait ? Way.R2L : Way.B2T;
        }

        Log.d(TAG, "mWay : " + mWay);
        Log.d(TAG, "mDisplayRect : " + mDisplayRect);

        long remainingDuration = mDuration - mTotalTime;
        if (mIsPortrait) {
            if (mWay == Way.R2L) {
                animate(mDisplayRect.left, mDisplayRect.left
                        - (mDisplayRect.right - getImageViewWidth()), remainingDuration);
            } else {
                animate(mDisplayRect.left, 0.0f, remainingDuration);
            }
        } else {
            if (mWay == Way.B2T) {
                animate(mDisplayRect.top, mDisplayRect.top
                        - (mDisplayRect.bottom - getImageViewHeight()), remainingDuration);
            } else {
                animate(mDisplayRect.top, 0.0f, remainingDuration);
            }
        }
    }

    private void changeWay() {
        if (mWay == Way.R2L) {
            mWay = Way.L2R;
        } else if (mWay == Way.L2R) {
            mWay = Way.R2L;
        } else if (mWay == Way.T2B) {
            mWay = Way.B2T;
        } else if (mWay == Way.B2T) {
            mWay = Way.T2B;
        }
        mCurrentPlayTime = 0;
        mTotalTime = 0;
    }

    private void animate(float start, float end, long duration) {
        Log.d(TAG, "startPanning : " + start + " to " + end + ", in " + duration + "ms");

        mCurrentAnimator = ValueAnimator.ofFloat(start, end);
        mCurrentAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                mMatrix.reset();
                applyScaleOnMatrix();
                if (mIsPortrait) {
                    mMatrix.postTranslate(value, 0);
                } else {
                    mMatrix.postTranslate(0, value);
                }
                refreshDisplayRect();
                mCurrentPlayTime = animation.getCurrentPlayTime();
                setCurrentImageMatrix();
            }
        });
        mCurrentAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "animation has finished, startPanning in the other way");
                changeWay();
                animate_();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                Log.d(TAG, "panning animation canceled");
            }
        });
        mCurrentAnimator.setDuration(duration);
        mCurrentAnimator.setInterpolator(mLinearInterpolator);
        mCurrentAnimator.start();
    }

    private void setCurrentImageMatrix() {
        getImageView().setImageMatrix(mMatrix);
        getImageView().invalidate();
        getImageView().requestLayout();
    }

    private void refreshDisplayRect() {
        mDisplayRect.set(0, 0, getDrawableIntrinsicWidth(), getDrawableIntrinsicHeight());
        mMatrix.mapRect(mDisplayRect);
    }

    private void scale() {
        mMatrix.reset();
        applyScaleOnMatrix();
        setCurrentImageMatrix();
    }

    private void applyScaleOnMatrix() {
        int drawableSize = mIsPortrait ? getDrawableIntrinsicHeight() : getDrawableIntrinsicWidth();
        int imageViewSize = mIsPortrait ? getImageViewHeight() : getImageViewWidth();
        float scaleFactor = (float) imageViewSize / (float) drawableSize;

        mMatrix.postScale(scaleFactor, scaleFactor);
    }

}
