package net.lonelytransistor.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.lonelytransistor.commonlib.OutlinedTextView;
import net.lonelytransistor.commonlib.ProgressDrawable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LauncherBar extends FrameLayout {
    private static final String TAG = "LauncherActivity";

    private static final int DELAY = 200;
    private static final int ANIMATION_DURATION = 200;

    public LauncherBar(Context context) {
        super(context);
        constructor();
    }
    public LauncherBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        constructor();
    }
    public LauncherBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        constructor();
    }
    public LauncherBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        constructor();
    }

    private OnKeyListener keyListenerRoot = (v, keyCode, event) -> false;
    @Override
    public void setOnKeyListener(OnKeyListener l) {
        super.setOnKeyListener(l);
        keyListenerRoot = l;
    }
    private OnClickListener clickListenerRoot = (v) -> {};
    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        clickListenerRoot = l;
    }

    private static abstract class BarViewHolder extends RecyclerView.ViewHolder {
        BarViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
    private abstract class BarAdapter extends RecyclerView.Adapter<BarViewHolder> {
        BarAdapter(Context ctx, RecyclerView v) {
            super();
            uiExecutor = ctx.getMainExecutor();
            rootView = v;
            rootViews.add(v);
        }

        private Executor uiExecutor;
        private final RecyclerView rootView;
        private static final List<RecyclerView> rootViews = new ArrayList<>();
        View currentlyVisible = null;
        final ScheduledExecutorService delayedExecutor = Executors.newScheduledThreadPool(4);
        Map<View, ScheduledFuture<?>> delayedSchedule = new HashMap<>();

        public void onFocused(BarViewHolder vh, boolean hasFocus) {}
        private final Map<View, BarViewHolder> viewToViewHolder = new HashMap<>();
        private void animateFocus(View v, boolean hasFocus) {
            animatedViews.add(v);
            v.animate()
                    .scaleY(hasFocus ? 1.1f : 1.0f)
                    .scaleX(hasFocus ? 1.1f : 1.0f)
                    .setDuration(ANIMATION_DURATION)
                    .setListener(new AnimatorListenerAdapter() {
                        void reset() {
                            v.setScaleX(hasFocus ? 1.1f : 1.0f);
                            v.setScaleY(hasFocus ? 1.1f : 1.0f);
                            animatedViews.remove(v);
                            if (hasFocus && !v.hasFocus()) {
                                animateFocus(v, false);
                            }
                        }
                        @Override public void onAnimationCancel(Animator animation) { reset(); }
                        @Override public void onAnimationEnd(Animator animation) { reset(); }
                        @Override public void onAnimationPause(Animator animation) { reset(); }
                    })
                    .start();
            if ((hasFocus && v.getScaleY() == 1.0f) || (!hasFocus && v.getScaleY() == 1.1f))
                onFocused(viewToViewHolder.get(v), hasFocus);
        }
        private final List<View> animatedViews = new ArrayList<>();
        private final View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            int padding = v.getPaddingBottom();
            v.setBackgroundResource(hasFocus ? R.drawable.border_hover: R.drawable.border_normal);
            v.setPadding(padding,padding,padding,padding);

            if (!hasFocus) {
                ScheduledFuture<?> sch = delayedSchedule.get(v);
                if (sch != null)
                    sch.cancel(true);
                v.clearAnimation();
                if (!animatedViews.contains(v)) {
                    animateFocus(v, false);
                }
            } else {
                currentlyVisible = v;
                ScheduledFuture<?> sch = delayedSchedule.put(v, delayedExecutor.schedule(new FutureTask<>(
                                () -> {
                                    uiExecutor.execute(() -> animateFocus(v, true));
                                    return null;
                                }),
                        DELAY, TimeUnit.MILLISECONDS));
                if (sch != null)
                    sch.cancel(true);
            }
        };
        protected boolean onKey(View v, int keyCode, KeyEvent event) {
            return keyListenerRoot.onKey(v, keyCode, event);
        }

        private final Map<Integer, BarViewHolder> positionToViewHolder = new HashMap<>();
        public boolean requestSelection(int position) {
            for (RecyclerView v : rootViews) {
                v.setDescendantFocusability(v == rootView ? FOCUS_AFTER_DESCENDANTS : FOCUS_BLOCK_DESCENDANTS);
            }
            rootView.smoothScrollToPosition(position);
            BarViewHolder vh = positionToViewHolder.get(Math.min(position, getItemCount()-1));
            if (vh != null) {
                vh.itemView.requestFocus();
                return vh.itemView.hasFocus();
            } else {
                return false;
            }
        }
        @Override
        final public void onBindViewHolder(@NonNull BarViewHolder vh, int position) {
            positionToViewHolder.put(position, vh);
            onBindView(vh, position);
        }
        abstract public void onBindView(@NonNull BarViewHolder holder, int position);

        @NonNull
        @Override
        final public BarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            BarViewHolder vh = onCreateViewHolder(parent);
            vh.itemView.setOnFocusChangeListener(focusListener);
            viewToViewHolder.put(vh.itemView, vh);
            return vh;
        }
        @NonNull abstract public BarViewHolder onCreateViewHolder(@NonNull ViewGroup parent);
    }

    private static class TopBarViewHolder extends BarViewHolder {
        final ConstraintLayout parent;
        final ImageView mainImage;
        final ImageView statusIcon;
        final OutlinedTextView scoreText;
        final ImageView scoreIcon;
        final OutlinedTextView popularityText;
        final ImageView popularityIcon;
        final TextView primaryText;
        final TextView secondaryText;
        TopBarViewHolder(@NonNull View itemView) {
            super(itemView);
            mainImage = itemView.findViewById(R.id.main_image);
            statusIcon = itemView.findViewById(R.id.status_icon);
            scoreText = itemView.findViewById(R.id.score);
            scoreIcon = itemView.findViewById(R.id.score_icon);
            popularityText = itemView.findViewById(R.id.popularity);
            popularityIcon = itemView.findViewById(R.id.popularity_icon);
            primaryText = itemView.findViewById(R.id.primary_text);
            secondaryText = itemView.findViewById(R.id.secondary_text);
            parent = (ConstraintLayout) mainImage.getParent();
        }
    }
    private class TopBarAdapter extends BarAdapter {
        private final int foldedCardHeight;
        private final int unfoldedCardHeight;
        private final Map<Integer, List<MovieCard>> rows = new HashMap<>();
        private List<MovieCard> currentRow;
        private int currentRowIx = 0;
        private int globalCount = 0;
        private int lastActiveIx = 0;
        TopBarAdapter() {
            super(LauncherBar.this.getContext(), topBar);
            ConstraintLayout.LayoutParams lp;
            View card = LayoutInflater.from(LauncherBar.this.getContext())
                    .inflate(R.layout.top_card_view, null, false);

            lp = (ConstraintLayout.LayoutParams) card.findViewById(R.id.main_image).getLayoutParams();
            int mainImageHeight = lp.height;
            lp = (ConstraintLayout.LayoutParams) card.findViewById(R.id.primary_text).getLayoutParams();
            int primaryTextHeight = lp.height;
            lp = (ConstraintLayout.LayoutParams) card.findViewById(R.id.secondary_text).getLayoutParams();
            int secondaryTextHeight = lp.height;
            foldedCardHeight = mainImageHeight + primaryTextHeight + card.getPaddingTop() + card.getPaddingBottom();
            unfoldedCardHeight = foldedCardHeight + secondaryTextHeight;

            lp = (ConstraintLayout.LayoutParams) topBar.getLayoutParams();
            lp.height = (int) ((foldedCardHeight + secondaryTextHeight) * 1.1f);
            topBar.setLayoutParams(lp);
        }
        private final OnKeyListener keyListenerInternal = (v, keyCode, event) -> {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
                case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
                case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    bottomBarAdapter.restoreFocus();
                    return true;
            }
            return onKey(v, keyCode, event);
        };
        private final OnClickListener clickListenerInternal = v -> {
            MovieCard card = currentRow.get(lastActiveIx);
            if (card.clickIntent != null)
                LauncherBar.this.getContext().startActivity(card.clickIntent);
            if (card.cb != null)
                card.cb.onClicked(card);
            clickListenerRoot.onClick(v);
        };
        @NonNull
        @Override
        public BarViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            View card = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.top_card_view, parent, false);
            ViewGroup.LayoutParams lp = card.getLayoutParams();
            lp.height = foldedCardHeight;
            card.setLayoutParams(lp);
            card.setTranslationY(unfoldedCardHeight - foldedCardHeight);
            card.setOnKeyListener(keyListenerInternal);
            card.setOnClickListener(clickListenerInternal);
            return new TopBarViewHolder(card);
        }
        public boolean restoreFocus() {
            if (getItemCount() > 0) {
                requestSelection(lastActiveIx);
                return true;
            } else {
                return false;
            }
        }
        @Override
        public void onFocused(BarViewHolder vh_, boolean hasFocus) {
            TopBarViewHolder vh = (TopBarViewHolder) vh_;
            int ix = vh.getAdapterPosition();
            if (ix >= currentRow.size() || ix < 0)
                return;
            lastActiveIx = ix;

            ViewGroup.LayoutParams layoutParams = vh.itemView.getLayoutParams();
            ValueAnimator anim = ValueAnimator.ofInt(
                    hasFocus ? foldedCardHeight : unfoldedCardHeight,
                    hasFocus ? unfoldedCardHeight : foldedCardHeight);
            anim.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                layoutParams.height = val;
                vh.itemView.setLayoutParams(layoutParams);
                vh.itemView.setTranslationY(unfoldedCardHeight - val);
            });
            anim.setDuration(ANIMATION_DURATION);
            anim.start();

            vh.primaryText.setEllipsize(hasFocus ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END);
            vh.primaryText.setSelected(hasFocus);
            vh.secondaryText.setSelected(hasFocus);
            bottomBarAdapter.showChevron();
            MovieCard card = currentRow.get(lastActiveIx);
            if (card.cb != null)
                card.cb.onHovered(card, hasFocus);
        }
        @Override
        public void onBindView(@NonNull BarViewHolder holder, int position) {
            TopBarViewHolder vh = (TopBarViewHolder) holder;
            MovieCard card = currentRow.get(position);
            vh.mainImage.setImageURI(card.mainImage);
            vh.statusIcon.setImageDrawable(card.statusIcon);
            if (card.score > 0) {
                vh.scoreText.setText((card.score / 10) + "." + (card.score % 10));
                vh.scoreIcon.setVisibility(VISIBLE);
                vh.popularityText.setText(String.valueOf(card.popularity));
                vh.popularityIcon.setImageResource(card.popularityDelta);
            } else {
                vh.scoreText.setText("");
                vh.scoreIcon.setVisibility(INVISIBLE);
                vh.popularityText.setText("");
                vh.popularityIcon.setImageDrawable(null);
            }
            vh.primaryText.setText(card.title);
            vh.primaryText.setBackground(new ProgressDrawable(0xFF000000, 0x40FF8800, 0.01f * card.progressBar));
            vh.secondaryText.setText(card.desc);
        }
        public void add(int rowIx, MovieCard card) {
            List<MovieCard> row = rows.get(rowIx);
            if (row == null) {
                rows.put(rowIx, new ArrayList<>());
                row = rows.get(rowIx);
            }
            row.add(card);
            globalCount = Math.max(row.size(), globalCount);
            notifyItemInserted(row.size()-1);
        }
        private void selectRowPriv(int rowIx) {
            if (!rows.containsKey(rowIx)) {
                rows.put(rowIx, new ArrayList<>());
            }
            currentRowIx = rowIx;
            currentRow = rows.get(rowIx);
            lastActiveIx = 0;
        }
        public boolean selectRow(int rowIx) {
            selectRowPriv(rowIx);
            notifyDataSetChanged();
            return getItemCount() > 0;
        }
        public void clearRow(int rowIx) {
            if (!rows.containsKey(rowIx)) {
                rows.put(rowIx, new ArrayList<>());
            }
            rows.get(rowIx).clear();
            if (rowIx == currentRowIx) {
                notifyDataSetChanged();
            }
        }
        public void clear() {
            for (List<MovieCard> row : rows.values()) {
                row.clear();
            }
            notifyDataSetChanged();
        }
        @Override
        public int getItemCount() {
            if (currentRow == null) {
                selectRowPriv(0);
            }
            return currentRow.size();
        }
        public int getItemCount(int ix) {
            return rows.containsKey(ix) ? rows.get(ix).size() : 0;
        }
    }
    private static class TopBadgeBarViewHolder extends BarViewHolder {
        final ConstraintLayout parent;
        final ImageView mainImage;
        final ImageView badgeImage;
        final TextView badgeText;
        final TextView primaryText;
        TopBadgeBarViewHolder(@NonNull View itemView) {
            super(itemView);
            mainImage = itemView.findViewById(R.id.main_image);
            badgeImage = itemView.findViewById(R.id.badge_image);
            badgeText = itemView.findViewById(R.id.badge_text);
            primaryText = itemView.findViewById(R.id.primary_text);
            parent = (ConstraintLayout) mainImage.getParent();
        }
    }
    private class TopBadgeBarAdapter extends BarAdapter {
        private final int mainImageHeight;
        private final int cardHeight;
        private final int primaryTextHeight;
        private final Map<Integer, List<BadgeCard>> rows = new HashMap<>();
        private int lastActiveIx = 0;
        private List<BadgeCard> currentRow;
        private int currentRowIx = 0;
        private int globalCount = 0;
        TopBadgeBarAdapter() {
            super(LauncherBar.this.getContext(), topBadgeBar);
            ConstraintLayout.LayoutParams lp;
            View card = LayoutInflater.from(LauncherBar.this.getContext())
                    .inflate(R.layout.top_badge_card_view, null, false);
            lp = (ConstraintLayout.LayoutParams) card.findViewById(R.id.main_image).getLayoutParams();
            mainImageHeight = lp.height;
            lp = (ConstraintLayout.LayoutParams) card.findViewById(R.id.primary_text).getLayoutParams();
            primaryTextHeight = lp.height;
            cardHeight = mainImageHeight + primaryTextHeight + card.getPaddingTop() + card.getPaddingBottom();

            lp = (ConstraintLayout.LayoutParams) topBadgeBar.getLayoutParams();
            lp.height = (int) (cardHeight * 1.1f);
            topBadgeBar.setLayoutParams(lp);
        }
        private final OnKeyListener keyListenerInternal = (v, keyCode, event) -> {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
                case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
                case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                    bottomBarAdapter.restoreFocus();
                    return true;
            }
            return onKey(v, keyCode, event);
        };
        private final OnClickListener clickListenerInternal = v -> {
            BadgeCard card = currentRow.get(lastActiveIx);
            if (card.cb != null)
                card.cb.onClicked(card);
            clickListenerRoot.onClick(v);
        };
        @NonNull
        @Override
        public BarViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            View card = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.top_badge_card_view, parent, false);
            ViewGroup.LayoutParams lp = card.getLayoutParams();
            card.setTranslationY(-card.getPaddingBottom());
            card.setOnKeyListener(keyListenerInternal);
            card.setOnClickListener(clickListenerInternal);
            return new TopBadgeBarViewHolder(card);
        }
        public boolean restoreFocus() {
            if (getItemCount() > 0) {
                requestSelection(lastActiveIx);
                return true;
            } else {
                return false;
            }
        }
        @Override
        public void onFocused(BarViewHolder vh_, boolean hasFocus) {
            TopBadgeBarViewHolder vh = (TopBadgeBarViewHolder) vh_;
            int ix = vh.getAdapterPosition();
            if (ix >= currentRow.size() || ix < 0)
                return;
            lastActiveIx = vh.getAdapterPosition();

            vh.primaryText.setEllipsize(hasFocus ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END);
            vh.primaryText.setSelected(hasFocus);
            bottomBarAdapter.showChevron();
            BadgeCard card = currentRow.get(lastActiveIx);
            if (card.cb != null)
                card.cb.onHovered(card, hasFocus);
        }

        @Override
        public void onBindView(@NonNull BarViewHolder holder, int position) {
            TopBadgeBarViewHolder vh = (TopBadgeBarViewHolder) holder;
            BadgeCard card = currentRow.get(position);
            setImage(vh.mainImage, card.mainImage);
            setImage(vh.badgeImage, card.badgeImage);
            vh.badgeText.setText(card.badgeText);
            vh.primaryText.setText(card.title);
            vh.badgeImage.setImageTintList(ColorStateList.valueOf(card.badgeColor));
            if (card.backgroundColor == 0) {
                vh.mainImage.setBackgroundColor(0xFF000000);
            } else {
                vh.mainImage.setBackgroundColor(card.backgroundColor);
            }
        }
        public void add(int rowIx, BadgeCard card) {
            List<BadgeCard> row = rows.get(rowIx);
            if (row == null) {
                rows.put(rowIx, new ArrayList<>());
                row = rows.get(rowIx);
            }
            row.add(card);
            globalCount = Math.max(row.size(), globalCount);
            notifyItemInserted(row.size()-1);
        }
        public void clearRow(int rowIx) {
            if (!rows.containsKey(rowIx)) {
                rows.put(rowIx, new ArrayList<>());
            }
            rows.get(rowIx).clear();
            if (rowIx == currentRowIx) {
                notifyDataSetChanged();
            }
        }
        public void clear() {
            for (List<BadgeCard> row : rows.values()) {
                row.clear();
            }
            notifyDataSetChanged();
        }
        private void selectRowPriv(int rowIx) {
            if (!rows.containsKey(rowIx)) {
                rows.put(rowIx, new ArrayList<>());
            }
            currentRowIx = rowIx;
            currentRow = rows.get(rowIx);
            lastActiveIx = 0;
        }
        public boolean selectRow(int rowIx) {
            selectRowPriv(rowIx);
            notifyDataSetChanged();
            return getItemCount() > 0;
        }
        @Override
        public int getItemCount() {
            if (currentRow == null) {
                selectRowPriv(0);
            }
            return currentRow.size();
        }
        public int getItemCount(int ix) {
            return rows.containsKey(ix) ? rows.get(ix).size() : 0;
        }
    }

    private static class BottomBarViewHolder extends BarViewHolder {
        final ConstraintLayout parent;
        final ImageView mainImage;
        final ImageView badgeImage;
        final TextView badgeText;
        final View background;
        final View chevron;
        BottomBarViewHolder(@NonNull View itemView) {
            super(itemView);
            mainImage = itemView.findViewById(R.id.main_image);
            badgeImage = itemView.findViewById(R.id.badge_image);
            badgeText = itemView.findViewById(R.id.badge_text);
            background = itemView.findViewById(R.id.background);
            chevron = itemView.findViewById(R.id.chevron);
            parent = (ConstraintLayout) mainImage.getParent();
        }
    }
    private void setImage(ImageView v, Object d) {
        if (d instanceof Drawable) {
            v.setImageDrawable((Drawable) d);
        } else if (d instanceof Integer) {
            v.setImageResource((Integer) d);
        } else if (d instanceof String) {
            v.setImageURI(Uri.parse((String) d));
        } else if (d instanceof Uri) {
            v.setImageURI((Uri) d);
        } else {
            v.setImageDrawable(null);
        }
    }
    private class BottomBarAdapter extends BarAdapter {
        BottomBarAdapter() {
            super(LauncherBar.this.getContext(), bottomBar);
        }
        private final List<BadgeCard> cards = new ArrayList<>();
        @Override
        public void onBindView(@NonNull BarViewHolder holder, int position) {
            BottomBarViewHolder vh = (BottomBarViewHolder) holder;
            BadgeCard card = cards.get(position);
            setImage(vh.badgeImage, card.badgeImage);
            setImage(vh.mainImage, card.mainImage);
            vh.badgeText.setText(card.badgeText);
            vh.badgeText.setTextColor(card.badgeColor);
            vh.badgeImage.setImageTintList(ColorStateList.valueOf(card.badgeColor));
            if (card.backgroundColor == 0) {
                vh.background.setBackgroundColor(0xFF000000);
            } else {
                vh.background.setBackgroundColor(card.backgroundColor);
            }
            if (position == 0) {
                rowChevron = vh;
            }
        }
        private int topBarRow = 0;
        private RecyclerView activeTopBar = topBar;
        private BottomBarViewHolder rowChevron = null;

        private final AnimatorListenerAdapter topBarALHide = new AnimatorListenerAdapter() {
            private void reset() {
                topBarAdapter.selectRow(topBarRow);
                topBadgeBarAdapter.selectRow(topBarRow);
            }
            @Override public void onAnimationCancel(Animator animation) { reset(); }
            @Override public void onAnimationEnd(Animator animation) { reset(); }
            @Override public void onAnimationPause(Animator animation) { reset(); }
        };
        private final AnimatorListenerAdapter chevronALHide = new AnimatorListenerAdapter() {
            private void reset() {
                rowChevron.chevron.setAlpha(0);
            }
            @Override public void onAnimationCancel(Animator animation) { reset(); }
            @Override public void onAnimationEnd(Animator animation) { reset(); }
            @Override public void onAnimationPause(Animator animation) { reset(); }
        };
        private final AnimatorListenerAdapter chevronALShow = new AnimatorListenerAdapter() {
            private void reset() {
                rowChevron.chevron.setAlpha(1);
            }
            @Override public void onAnimationCancel(Animator animation) { reset(); }
            @Override public void onAnimationEnd(Animator animation) { reset(); }
            @Override public void onAnimationPause(Animator animation) { reset(); }
        };
        public void restoreFocus() {
            requestSelection(rowChevron != null ? rowChevron.getAdapterPosition() : 0);
        }
        public void showChevron() {
            if (rowChevron != null && rowChevron.chevron.getAlpha() != 1) {
                rowChevron.chevron.animate()
                        .alpha(1)
                        .setDuration(ANIMATION_DURATION)
                        .setListener(chevronALShow)
                        .start();
            }
        }
        @Override
        public void onFocused(BarViewHolder vh, boolean hasFocus) {
            int position = vh.getAdapterPosition();
            if (position >= cards.size() || position < 0)
                return;
            if (hasFocus) {
                if (rowChevron != null && rowChevron.chevron.getAlpha() != 0) {
                    rowChevron.chevron.animate()
                            .alpha(0)
                            .setDuration(ANIMATION_DURATION)
                            .setListener(chevronALHide)
                            .start();
                }
                if (topBarRow != position) {
                    topBarRow = position;
                    topBar.animate()
                            .alpha(0)
                            .translationY(25)
                            .setDuration(ANIMATION_DURATION)
                            .setListener(topBarALHide)
                            .start();
                    topBadgeBar.animate()
                            .alpha(0)
                            .translationY(25)
                            .setDuration(ANIMATION_DURATION)
                            .setListener(topBarALHide)
                            .start();
                    rowChevron = (BottomBarViewHolder) vh;
                }
            }
            BadgeCard card = cards.get(position);
            if (card.cb != null)
                card.cb.onHovered(card, hasFocus);
        }
        @Override
        public int getItemCount() {
            return cards.size();
        }
        public int add(BadgeCard card) {
            cards.add(card);
            notifyItemInserted(cards.size()-1);
            return cards.size()-1;
        }
        public void clear() {
            cards.clear();
            notifyDataSetChanged();
        }
        private final OnKeyListener keyListenerInternal = (v, keyCode, event) -> {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_UP_LEFT:
                case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
                case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                    if (topBarAdapter.restoreFocus()) {
                        activeTopBar = topBar;
                    } else if (topBadgeBarAdapter.restoreFocus()) {
                        activeTopBar = topBadgeBar;
                    }
                    return true;
            }
            return onKey(v, keyCode, event);
        };
        private final OnClickListener clickListenerInternal = v -> {
            BadgeCard card = cards.get(topBarRow);
            if (card.cb != null)
                card.cb.onClicked(card);
            clickListenerRoot.onClick(v);
        };
        @NonNull
        @Override
        public BarViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
            View card = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.bottom_card_view, parent, false);
            card.setOnKeyListener(keyListenerInternal);
            card.setOnClickListener(clickListenerInternal);
            return new BottomBarViewHolder(card);
        }
    }

    private TopBarAdapter topBarAdapter;
    private TopBadgeBarAdapter topBadgeBarAdapter;
    private BottomBarAdapter bottomBarAdapter;
    private RecyclerView topBar;
    private RecyclerView topBadgeBar;
    private RecyclerView bottomBar;
    private static class LManager extends LinearLayoutManager {
        LManager(Context ctx, RecyclerView bar) {
            super(ctx, LinearLayoutManager.HORIZONTAL, false);
            this.bar = bar;
        }
        private final RecyclerView bar;
        private final AnimatorListenerAdapter topBarALShow = new AnimatorListenerAdapter() {
            private void reset() {
                bar.setAlpha(1);
                bar.setTranslationY(0);
            }
            @Override public void onAnimationCancel(Animator animation) { reset(); }
            @Override public void onAnimationEnd(Animator animation) { reset(); }
            @Override public void onAnimationPause(Animator animation) { reset(); }
        };
        @Override
        public void onLayoutCompleted(RecyclerView.State state) {
            super.onLayoutCompleted(state);
            if (bar.getAlpha() < 1) {
                bar.post(() -> {
                    bar.animate()
                            .alpha(1)
                            .translationY(0)
                            .setDuration(ANIMATION_DURATION)
                            .setListener(topBarALShow)
                            .start();
                });
            }
        }
    }
    public int addItem(int row, Card item) {
        if (item instanceof BadgeCard) {
            topBadgeBarAdapter.add(row, (BadgeCard) item);
            return topBadgeBarAdapter.getItemCount(row)-1;
        } else if (item instanceof MovieCard) {
            topBarAdapter.add(row, (MovieCard) item);
            return topBarAdapter.getItemCount(row)-1;
        }
        throw new RuntimeException("Card can only be BadgeCard or MovieCard.");
    }
    public int addRow(BadgeCard item) {
        if (item != null) {
            bottomBarAdapter.add(item);
        }
        return bottomBarAdapter.getItemCount()-1;
    }
    public void clearRow(int row) {
        topBarAdapter.clearRow(row);
        topBadgeBarAdapter.clearRow(row);
    }
    public void clear() {
        topBarAdapter.clear();
        topBadgeBarAdapter.clear();
        bottomBarAdapter.clear();
    }
    public void resetSelection() {
        bottomBarAdapter.requestSelection(0);
    }
    private void constructor() {
        Context ctx = getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        inflater.inflate(R.layout.launcher_bar, this, true);

        topBar = findViewById(R.id.topBar);
        topBar.setLayoutManager(new LManager(ctx, topBar));
        topBarAdapter = new TopBarAdapter();
        topBar.setAdapter(topBarAdapter);

        topBadgeBar = findViewById(R.id.topBadgeBar);
        topBadgeBar.setLayoutManager(new LManager(ctx, topBadgeBar));
        topBadgeBarAdapter = new TopBadgeBarAdapter();
        topBadgeBar.setAdapter(topBadgeBarAdapter);

        bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setLayoutManager(new LManager(ctx, bottomBar));
        bottomBarAdapter = new BottomBarAdapter();
        bottomBar.setAdapter(bottomBarAdapter);
    }
}