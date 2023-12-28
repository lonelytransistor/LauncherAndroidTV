package net.lonelytransistor.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BaseCardView;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowPresenter;
import androidx.recyclerview.widget.LinearLayoutManager;

import net.lonelytransistor.launcher.repos.MovieTitle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StreamingServiceBar extends HorizontalGridView {
    private static final String TAG = "StreamingServiceBar";

    public static class Card {
        public String title;
        public String desc;
        public Drawable statusIcon;
        public Object mainImage;
        public OnCardCallback cb;
        public MovieTitle movieTitle;
        public boolean standardHeight;
        public int progressBar;
        public boolean separator = false;

        private OnCardCallback oldCb;
        private int ix = -1;

        public Card() {}
        public Card(Card c) {
            title = c.title;
            desc = c.desc;
            mainImage = c.mainImage;
            progressBar = c.progressBar;
            statusIcon = c.statusIcon;
            standardHeight = c.standardHeight;
            separator = c.separator;
            cb = c.cb;
            oldCb = c.oldCb;
            ix = c.ix;
            movieTitle = c.movieTitle != null ? new MovieTitle(c.movieTitle) : null;
        }
        public Card(MovieTitle tit, OnCardCallback callback) {
            title = tit.title;
            desc = tit.description;
            mainImage = tit.getImage();
            movieTitle = tit;
            cb = callback;
        }
        public static List<Card> fromMovieTitleList(List<MovieTitle> titles, OnCardCallback cb) {
            List<Card> ret = new ArrayList<>();
            for (MovieTitle title : titles) {
                Card card = new Card(title, cb);
                ret.add(card);
            }
            return ret;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Card card = (Card) o;
            return ix == card.ix && Objects.equals(title, card.title) && Objects.equals(desc, card.desc) && Objects.equals(mainImage, card.mainImage) && Objects.equals(cb, card.cb);
        }
        @Override
        public int hashCode() {
            return Objects.hash(ix, title, desc, mainImage, cb);
        }
        @NonNull
        @Override
        protected Card clone() {
            return new Card(this);
        }
    }
    public interface OnCardCallback {
        void onClicked(Card title);
        void onHovered(Card title, boolean hovered);
    }
    @Override
    public void setOnKeyListener(OnKeyListener l) {
        super.setOnKeyListener(l);
        keyListener = l;
    }
    private OnKeyListener keyListener = (v, keyCode, event) -> false;
    private final OnKeyListener keyListenerInternal = (v, keyCode, event) -> {
        return keyListener.onKey(v, keyCode, event);
    };
    public static class Badge {
        final int backgroundColor;
        final Object icon;
        final int iconColor;
        final Object text;
        final int textColor;
        Badge(int backgroundColor, Object icon, int iconColor, Object text, int textColor) {
            this.backgroundColor = backgroundColor;
            this.icon = icon;
            this.iconColor = iconColor;
            this.text = text;
            this.textColor = textColor;
        }
        Badge() {
            backgroundColor = 0;
            icon = null;
            iconColor = 0;
            text = "";
            textColor = 0;
        }
    }
    private class CardPresenter extends Presenter {
        private static int badgeHeight = 0;
        private static int badgeWidth = 0;
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            BaseCardView cardView = (BaseCardView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.bottom_bar_card_view, parent, false);
            cardView.setBackgroundResource(R.drawable.border);
            cardView.setCardType(ImageCardView.CARD_TYPE_INFO_UNDER);
            cardView.setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_SELECTED);
            ((TextView) cardView.findViewById(R.id.primary_text)).setSingleLine(true);
            cardView.setOnFocusChangeListener((v, hasFocus) -> {
                TextView title = v.findViewById(R.id.primary_text);
                title.setEllipsize(hasFocus ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END);
                title.setSelected(hasFocus);
            });
            cardView.setOnKeyListener(keyListenerInternal);
            return new ViewHolder(cardView);
        }
        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            CardView cardView = (CardView) viewHolder.view;
            ImageView badgeImage = cardView.findViewById(R.id.badge_image);
            TextView badgeText = cardView.findViewById(R.id.badge_text);
            ImageView mainImage = cardView.findViewById(R.id.main_image);
            ImageView statusIcon = cardView.findViewById(R.id.status_icon);
            View progressBar = cardView.findViewById(R.id.progress_bar);
            TextView popularity = cardView.findViewById(R.id.popularity);
            ImageView popularityIcon = cardView.findViewById(R.id.popularity_icon);
            TextView score = cardView.findViewById(R.id.score);
            ImageView scoreIcon = cardView.findViewById(R.id.score_icon);
            TextView title = cardView.findViewById(R.id.primary_text);
            TextView desc = cardView.findViewById(R.id.secondary_text);

            if (badgeWidth == 0 || badgeHeight == 0) {
                ViewGroup.LayoutParams params = cardView.findViewById(R.id.inner_layout).getLayoutParams();
                Drawable badge = viewHolder.view.getContext().getDrawable(R.mipmap.ic_banner);
                badgeWidth = params.width;
                badgeHeight = (int) ((1.0f * badge.getIntrinsicHeight()/badge.getIntrinsicWidth()) * badgeWidth);
            }

            if (item instanceof Card card) {
                FrameLayout.LayoutParams cardViewLayoutParams = (FrameLayout.LayoutParams) cardView.getLayoutParams();
                cardViewLayoutParams.setMarginStart(card.separator ? 15 : 1);
                cardView.setLayoutParams(cardViewLayoutParams);

                if (card.cb != null) {
                    cardView.setOnClickListener(v -> card.cb.onClicked(card));
                    cardView.setOnFocusChangeSecondaryListener((v, a) -> card.cb.onHovered(card, a));
                } else {
                    cardView.setOnClickListener(null);
                    cardView.setOnFocusChangeSecondaryListener(null);
                }
                if (card.statusIcon != null) {
                    statusIcon.setVisibility(VISIBLE);
                    statusIcon.setImageDrawable(card.statusIcon);
                } else {
                    statusIcon.setVisibility(INVISIBLE);
                }
                Badge badge = new Badge();
                mainImage.setBackgroundResource(0);
                if (card.mainImage instanceof Drawable) {
                    mainImage.setImageDrawable((Drawable) card.mainImage);
                } else if (card.mainImage instanceof String) {
                    mainImage.setImageURI(Uri.parse((String) card.mainImage));
                } else if (card.mainImage instanceof Integer) {
                    mainImage.setImageResource((Integer) card.mainImage);
                } else if (card.mainImage instanceof Badge) {
                    badge = (Badge) card.mainImage;
                    mainImage.setImageDrawable(null);
                    mainImage.setBackgroundColor(badge.backgroundColor);
                } else {
                    mainImage.setImageDrawable(null);
                }
                if (badge.icon instanceof Drawable) {
                    badgeImage.setImageDrawable((Drawable) badge.icon);
                } else if (badge.icon instanceof String) {
                    badgeImage.setImageURI(Uri.parse((String) badge.icon));
                } else if (badge.icon instanceof Integer) {
                    badgeImage.setImageResource((Integer) badge.icon);
                } else {
                    badgeImage.setImageDrawable(null);
                }
                badgeImage.setImageTintList(ColorStateList.valueOf(badge.iconColor));
                if (badge.text instanceof String) {
                    badgeText.setText((String) badge.text);
                } else if (badge.text instanceof Integer) {
                    badgeText.setText((Integer) badge.text);
                } else {
                    badgeText.setText("");
                }
                badgeText.setTextColor(badge.textColor);

                if (card.standardHeight || card.mainImage instanceof Badge) {
                    mainImage.setMaxHeight(badgeHeight);
                    mainImage.setMinimumHeight(badgeHeight);
                } else {
                    mainImage.setMaxHeight(1000);
                    mainImage.setMinimumHeight(0);
                }
                if (card.progressBar > 0 && card.progressBar < 100) {
                    progressBar.setVisibility(VISIBLE);
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) progressBar.getLayoutParams();
                    params.width = card.progressBar*badgeWidth/100;
                    progressBar.setLayoutParams(params);
                } else {
                    progressBar.setVisibility(INVISIBLE);
                }
                if (card.title != null && card.title.length()>0) {
                    title.setText(card.title);
                    title.setMaxLines(1);
                } else {
                    title.setMaxHeight(0);
                }
                if (card.desc != null && card.desc.length()>0) {
                    cardView.setCardType(ImageCardView.CARD_TYPE_INFO_UNDER);
                    desc.setText(card.desc);
                    desc.setMaxLines(5);
                } else {
                    cardView.setCardType(ImageCardView.CARD_TYPE_MAIN_ONLY);
                    desc.setMaxHeight(0);
                }

                if (card.movieTitle != null) {
                    popularityIcon.setImageDrawable(card.movieTitle.getPopularityDeltaImage());
                    popularity.setText(String.valueOf(card.movieTitle.popularity));
                    score.setText(String.valueOf(card.movieTitle.imdbScore));
                    popularityIcon.setVisibility(VISIBLE);
                    popularity.setVisibility(VISIBLE);
                    scoreIcon.setVisibility(VISIBLE);
                    score.setVisibility(VISIBLE);
                } else {
                    popularityIcon.setVisibility(INVISIBLE);
                    popularity.setVisibility(INVISIBLE);
                    scoreIcon.setVisibility(INVISIBLE);
                    score.setVisibility(INVISIBLE);
                }
            }
        }
        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }


    public class CardListRowPresenter extends ListRowPresenter {
        private static class CardListRow extends ListRow {
            public CardListRow(HeaderItem header, ObjectAdapter adapter) {
                super(header, adapter);
            }
            public CardListRow(long id, HeaderItem header, ObjectAdapter adapter) {
                super(id, header, adapter);
            }
            public CardListRow(ObjectAdapter adapter) {
                super(adapter);
            }
            public HorizontalGridView curGridview = null;
            public boolean visible = true;
        }
        CardListRowPresenter() {
            super();
            rows = new ArrayObjectAdapter(this);
            adapter = new ItemBridgeAdapter(rows);
        }
        private final ArrayObjectAdapter rows;
        private final ItemBridgeAdapter adapter;
        @Override
        protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
            super.initializeRowViewHolder(holder);
            ((ListRowPresenter.ViewHolder) holder).getGridView().setGravity(Gravity.BOTTOM);
        }
        @Override
        protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
            super.onBindRowViewHolder(holder, item);
            CardListRow it = (CardListRow) item;
            ViewHolder vh = (ListRowPresenter.ViewHolder) holder;
            it.curGridview = vh.getGridView();
            it.curGridview.setVisibility(it.visible ? VISIBLE : GONE);
        }
        public ItemBridgeAdapter getAdapter() {
            return adapter;
        }
        public ArrayObjectAdapter getRow(int ix) {
            return (ArrayObjectAdapter) ((CardListRow) rows.get(ix)).getAdapter();
        }
        public int size() {
            return rows.size();
        }
        public int getRowSize(int ix) {
            return getRow(ix).size();
        }
        public ArrayObjectAdapter addRow() {
            CardPresenter rowPresenter = new CardPresenter();
            ArrayObjectAdapter rowAdapter = new ArrayObjectAdapter(rowPresenter);
            CardListRow row = new CardListRow(rowAdapter);
            rows.add(row);
            return rowAdapter;
        }
        public void focusItem(int row, int col) {
            CardListRow itemRow = (CardListRow) rows.get(row);
            itemRow.visible = true;
            if (itemRow.curGridview != null) {
                itemRow.curGridview.setVisibility(VISIBLE);
                itemRow.curGridview.requestFocus();
                itemRow.curGridview.setSelectedPositionSmooth(col);
            }
        }
        public void showRow(int ix) {
            CardListRow row = (CardListRow) rows.get(ix);
            if (getRow(ix).size() <= 0)
                return;
            row.visible = true;
            if (row.curGridview != null) {
                row.curGridview.setAlpha(1);
                row.curGridview.setVisibility(VISIBLE);
            }
        }
        public void hideRow(int ix) {
            CardListRow row = (CardListRow) rows.get(ix);
            row.visible = false;
            if (row.curGridview != null) {
                row.curGridview.setAlpha(0);
                row.curGridview.setVisibility(GONE);
                row.curGridview.scrollToPosition(0);
            }
        }
        public void fadeToRow(int ixHide, int ixShow) {
            CardListRow rowHide = (CardListRow) rows.get(ixHide);
            CardListRow rowShow = (CardListRow) rows.get(ixShow);
            if (rowShow.curGridview != null && rowHide.curGridview != null) {
                ScheduledFuture<?> delayedSchedule = Executors.newScheduledThreadPool(1).
                        schedule(new FutureTask<>(
                                () -> StreamingServiceBar.this.post(() -> {
                                    hideRow(ixHide);
                                    showRow(ixShow);
                                })),
                        ANIMATION_DURATION*2+100, TimeUnit.MILLISECONDS);
                rowHide.curGridview.clearAnimation();
                rowHide.curGridview
                        .animate()
                        .alpha(0)
                        .translationY(50)
                        .setDuration(ANIMATION_DURATION)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                hideRow(ixHide);
                                rowShow.curGridview.setAlpha(0);
                                rowShow.curGridview.setTranslationY(50);
                                rowShow.curGridview.setVisibility(VISIBLE);
                                rowShow.curGridview.clearAnimation();
                                rowShow.curGridview
                                        .animate()
                                        .alpha(1)
                                        .translationY(0)
                                        .setDuration(ANIMATION_DURATION)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                showRow(ixShow);
                                                postInvalidate();
                                                delayedSchedule.cancel(false);
                                            }
                                        })
                                        .start();
                            }
                        })
                        .start();
            } else {
                hideRow(ixHide);
                showRow(ixShow);
            }
        }
    }
    public static final int ANIMATION_DURATION = 300;
    private final CardListRowPresenter presenter = new CardListRowPresenter();
    public StreamingServiceBar(Context context) {
        this(context, null);
    }
    public StreamingServiceBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public StreamingServiceBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        presenter.addRow();
        setAdapter(presenter.getAdapter());
        LinearLayoutManager llm = new LinearLayoutManager(context);
        llm.setReverseLayout(true);
        setLayoutManager(llm);
    }

    private final OnCardCallback cardCallback = new OnCardCallback() {
        final ScheduledExecutorService delayedExecutor = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> delayedSchedule = delayedExecutor.schedule(new FutureTask<>(()->null), 1, TimeUnit.SECONDS);
        Card currentHideCard = null;
        Card currentShowCard = null;
        private void delayedOnHovered() {
            if (currentHideCard == null) {
                presenter.showRow(currentShowCard.ix);
                return;
            }
            presenter.fadeToRow(currentHideCard.ix, currentShowCard.ix);
            if (currentShowCard.oldCb != null)
                currentShowCard.oldCb.onHovered(currentShowCard, true);
            currentHideCard = null;
        }
        @Override
        public void onClicked(Card card) {
            if (card.oldCb != null)
                card.oldCb.onClicked(card);
        }
        @Override
        public void onHovered(Card card, boolean hovered) {
            if (card.ix < 0) {
                currentHideCard = null;
                if (card.oldCb != null)
                    card.oldCb.onHovered(card, hovered);
            } else if (!hovered && currentHideCard == null) {
                currentHideCard = card;
                if (card.oldCb != null)
                    card.oldCb.onHovered(card, false);
            } else if (hovered && currentShowCard != card) {
                currentShowCard = card;
                delayedSchedule.cancel(true);
                delayedSchedule = delayedExecutor.schedule(new FutureTask<>(
                                () -> StreamingServiceBar.this.post(() -> delayedOnHovered())),
                        150, TimeUnit.MILLISECONDS);
            }
        }
    };
    public void forceFocusItem(int ix) {
        for (int i=1; i<presenter.size(); i++) {
            if (i == ix+1) {
                presenter.showRow(i);
            } else {
                presenter.hideRow(i);
            }
        }
        presenter.focusItem(0, ix);
    }
    public ArrayObjectAdapter addItem(Card item) {
        ArrayObjectAdapter bottomRow = presenter.getRow(0);
        ArrayObjectAdapter row = presenter.addRow();
        Card item2 = item.clone();
        item2.ix = presenter.size()-1;
        item2.oldCb = item2.cb;
        item2.cb = cardCallback;
        bottomRow.add(item2);
        presenter.showRow(0);
        return row;
    }
}
