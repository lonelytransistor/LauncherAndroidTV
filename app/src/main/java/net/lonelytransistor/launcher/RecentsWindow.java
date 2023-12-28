package net.lonelytransistor.launcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.leanback.widget.HorizontalGridView;
import androidx.recyclerview.widget.RecyclerView;

import net.lonelytransistor.launcher.generics.GenericWindow;
import net.lonelytransistor.launcher.repos.ApkRepo;

import java.util.List;

public class RecentsWindow extends GenericWindow {
    private static final String TAG = "RecentsWindow";

    private static final int SCALE_DURATION = 200;

    private final View.OnKeyListener mKeyListener = (v, keyCode, event) -> {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_ESCAPE:
                    hide();
                    return true;
            }
        }
        return false;
    };
    private final HorizontalGridView mRecentsView;
    public RecentsWindow(Context ctx) {
        super(ctx, R.layout.activity_recents);
        mRecentsView = (HorizontalGridView) findViewById(R.id.recents_bar);
        mRecentsView.setAdapter(new RecentsBarAdapter());
        getView().setOnKeyListener(mKeyListener);
        mRecentsView.setOnKeyListener(mKeyListener);
    }

    private class RecentsBarAdapter extends RecyclerView.Adapter<RecentsBarAdapter.ViewHolder> {
        private List<ApkRepo.App> apps;
        public void refresh() {
            apps = ApkRepo.getRecentApps();
            notifyDataSetChanged();
        }
        RecentsBarAdapter() {
            super();
            apps = ApkRepo.getRecentApps();
        }
        @NonNull
        @Override
        public RecentsBarAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout view = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recents_card_view, parent, false);
            view.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    int deltaX = (v.getLeft() + v.getMeasuredWidth()/2) - parent.getMeasuredWidth()/2;
                    if (deltaX != 0) {
                        ((HorizontalGridView) parent).smoothScrollBy(deltaX, 0);
                    }
                    v.animate().scaleX(1.5f).scaleY(1.5f).setDuration(SCALE_DURATION).start();
                } else {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(SCALE_DURATION).start();
                }
            });
            return new RecentsBarAdapter.ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull RecentsBarAdapter.ViewHolder holder, int position) {
            holder.setApp(apps.get(position));
        }
        @Override
        public int getItemCount() {
            return apps.size();
        }
        public class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView textView;
            private final ImageView imageView;
            public final View view;

            public ViewHolder(View view) {
                super(view);
                this.view = view;
                textView = view.findViewById(R.id.primary_text);
                imageView = view.findViewById(R.id.main_image);
            }
            public void setApp(String name, Drawable badge, Intent intent) {
                textView.setText(name);
                imageView.setImageDrawable(badge);
                view.setOnClickListener(v -> {
                    v.getContext().startActivity(intent);
                    hide();
                });
                view.setOnKeyListener(mKeyListener);
            }
            public void setApp(ApkRepo.App app) {
                setApp(app.name, app.icon, app.leanbackIntent != null ? app.leanbackIntent : app.launchIntent);
            }
        }
    }
    @Override
    public void onShow() {
        ((RecentsBarAdapter) mRecentsView.getAdapter()).refresh();
        mRecentsView.post(() -> {
            mRecentsView.setPadding(mRecentsView.getMeasuredWidth(), 0, mRecentsView.getMeasuredWidth(), 0);
            mRecentsView.post(() -> mRecentsView.scrollToPosition(0));
        });
    }
    @Override
    public void onHide() {
    }
}