package net.lonelytransistor.launcher.generics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.WindowManager;

public class GenericWindow {
    private static final String TAG = "GenericWindow";

    private static final int FADE_DURATION = 500;

    private static WindowManager mWM = null;
    private static final WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT);

    private final View mView;
    private final Context mContext;
    public GenericWindow(Context ctx, int res) {
        mContext = ctx;
        if (mWM == null) {
            mWM = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        }
        mView = View.inflate(ctx, res, null);
    }
    public View getView() {
        return mView;
    }
    public View findViewById(int id) {
        return getView().findViewById(id);
    }
    public Drawable getDrawable(int res) {
        return mContext.getDrawable(res);
    }
    public ContentResolver getContentResolver() {
        return mContext.getContentResolver();
    }
    public void startActivity(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        hide();
    }

    public void onShow() {}
    public void onHide() {}
    public boolean isVisible() {
        return mView.isAttachedToWindow();
    }
    final public void show() {
        if (mView.isAttachedToWindow())
            return;
        mContext.getMainExecutor().execute(()-> {
            mView.setVisibility(View.VISIBLE);
            mView.setAlpha(0);
            mWM.addView(mView, wmParams);
            onShow();
            mView.animate()
                    .setDuration(FADE_DURATION)
                    .alpha(1)
                    .setListener(null)
                    .start();
        });
    }
    final protected void hide() {
        if (!mView.isAttachedToWindow())
            return;
        mContext.getMainExecutor().execute(()-> {
            mView.animate()
                    .setDuration(FADE_DURATION)
                    .alpha(0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mView.setVisibility(View.GONE);
                            mWM.removeView(mView);
                        }
                    }).start();
            onHide();
        });
    }
}