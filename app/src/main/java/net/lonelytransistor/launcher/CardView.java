package net.lonelytransistor.launcher;

import android.content.Context;
import android.util.AttributeSet;

import androidx.leanback.widget.BaseCardView;

public class CardView extends BaseCardView {
    public CardView(Context context) {
        super(context);
    }
    public CardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public CardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    private OnFocusChangeListener primaryListener = null;
    private OnFocusChangeListener secondaryListener = null;
    private final OnFocusChangeListener listener = (v, f) -> {
        if (primaryListener != null)
            primaryListener.onFocusChange(v, f);
        if (secondaryListener != null)
            secondaryListener.onFocusChange(v, f);
    };
    @Override
    public OnFocusChangeListener getOnFocusChangeListener() {
        return primaryListener;
    }
    @Override
    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        super.setOnFocusChangeListener(listener);
        primaryListener = l;
    }
    public void setOnFocusChangeSecondaryListener(OnFocusChangeListener l) {
        secondaryListener = l;
    }
}
