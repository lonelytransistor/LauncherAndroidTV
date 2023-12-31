package net.lonelytransistor.launcher;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.GridLayout;

import net.lonelytransistor.commonlib.Preferences;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WidgetBar extends GridLayout {
    private static final String TAG = "WidgetBar";
    private static final String PREFERENCES_KEY = "WIDGET_BAR_SNAPSHOT";
    private static final String PREFERENCES_NAME = "WIDGETS";

    private Activity getActivity(Context ctx) {
        Context context = ctx;
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }
    private void constructor(Context context) {
        mContext = context.getApplicationContext();
        mPackageManager = mContext.getPackageManager();
        mAppWidgetManager = AppWidgetManager.getInstance(mContext);
        mAppWidgetHost = new AppWidgetHost(mContext, 0xDEADBEEF);
        mCellWidth = getMeasuredWidth()/getColumnCount();
        mCellHeight = getMeasuredHeight()/getRowCount();
        setFocusable(false);
        setFocusableInTouchMode(false);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

        mPreferences = new Preferences(mContext, PREFERENCES_KEY);
        onStart();
    }
    public WidgetBar(Context context) {
        super(context);
        constructor(context);
    }
    public WidgetBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        constructor(context);
    }
    public WidgetBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        constructor(context);
    }
    public WidgetBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        constructor(context);
    }

    private static class WidgetID implements Serializable {
        int id;
        int x, y, width, height;
        AppWidgetProviderInfo info;
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            id = in.readInt();
            x = in.readInt();
            y = in.readInt();
            width = in.readInt();
            height = in.readInt();
            info = mWidgetList.get((String) in.readObject());
            Log.i(TAG, "loading: " + info);
        }
        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeInt(id);
            out.writeInt(x);
            out.writeInt(y);
            out.writeInt(width);
            out.writeInt(height);
            out.writeObject(info.provider.flattenToString());
        }
        WidgetID() {}
    }
    private class WidgetHolder {
        WidgetID id = new WidgetID();
        AppWidgetHostView view;
        WidgetHolder(WidgetID id) {
            this.id = id;
            view = mAppWidgetHost.createView(mContext, id.id, id.info);
            view.setAppWidget(id.id, id.info);
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams(
                    GridLayout.spec(0, id.width), GridLayout.spec(0, id.height)
            );
            view.setLayoutParams(glp);
        }
        WidgetHolder(AppWidgetProviderInfo info) {
            id.info = info;
            id.id = mAppWidgetHost.allocateAppWidgetId();
            Log.i(TAG, "Bound: " + mAppWidgetManager.bindAppWidgetIdIfAllowed(id.id, info.provider));
            view = mAppWidgetHost.createView(mContext, id.id, info);
            view.setAppWidget(id.id, info);
            id.x = 0;
            id.y = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                id.width = info.targetCellWidth;
                id.height = info.targetCellHeight;
            } else {
                Bundle options = mAppWidgetManager.getAppWidgetOptions(id.id);
                DisplayMetrics m = getResources().getDisplayMetrics();
                id.width  = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
                id.height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
                id.width  = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, id.width, m) / mCellWidth;
                id.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, id.height, m) / mCellHeight;
            }
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams(
                    GridLayout.spec(0, id.width), GridLayout.spec(0, id.height)
            );
            view.setLayoutParams(glp);
        }
    }
    private final Map<AppWidgetProviderInfo, WidgetHolder> mWidgetHolderList = new HashMap<>();
    private final static HashMap<String, AppWidgetProviderInfo> mWidgetList = new HashMap<>();
    private final List<BadgeCard> mWidgetCardList = new ArrayList<>();
    private PackageManager mPackageManager;
    private AppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private Context mContext;
    private int mCellWidth;
    private int mCellHeight;
    private Preferences mPreferences;
    public List<BadgeCard> getAllWidgetCards() {
        if (!mWidgetCardList.isEmpty()) {
            return mWidgetCardList;
        }
        for (AppWidgetProviderInfo widget : mAppWidgetManager.getInstalledProviders()) {
            mWidgetList.put(widget.provider.flattenToString(), widget);
            mWidgetCardList.add(new BadgeCard(
                    String.valueOf(widget.loadLabel(mPackageManager)),
                    widget.loadPreviewImage(mContext, 0),
                    mWidgetHolderList.keySet().contains(widget) ? R.drawable.icon_tick : null, new Card.Callback() {
                @Override
                public boolean onClicked(Card card) {
                    if (!mWidgetHolderList.keySet().contains(widget)) {
                        addWidget(widget);
                    } else {
                        removeWidget(widget);
                    }
                    return true;
                }
                @Override
                public void onHovered(Card card, boolean hovered) {}
            }));
        }
        return mWidgetCardList;
    }
    public void onStart() {
        mAppWidgetHost.startListening();
        load();
    }
    public void onStop() {
        save();
        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
    }
    private void save() {
        getAllWidgetCards();
        List<WidgetID> ids = new ArrayList<>();
        for (WidgetHolder holder : mWidgetHolderList.values()) {
            ids.add(holder.id);
        }
        mPreferences.setList(PREFERENCES_NAME, ids);
    }
    private void load() {
        getAllWidgetCards();
        List<WidgetID> widgets = (List<WidgetID>) mPreferences.getList(PREFERENCES_NAME);
        for (WidgetID widget : widgets) {
            addWidget(widget);
        }
    }
    public void removeWidget(AppWidgetProviderInfo info) {
        WidgetHolder widget = mWidgetHolderList.get(info);
        if (widget != null) {
            removeView(widget.view);
            mAppWidgetHost.deleteAppWidgetId(widget.id.id);
            mWidgetHolderList.remove(info);
        }
    }
    public void addWidget(AppWidgetProviderInfo info) {
        WidgetHolder widget = new WidgetHolder(info);
        mWidgetHolderList.put(widget.id.info, widget);
        addView(widget.view);
    }
    public void addWidget(WidgetID id) {
        WidgetHolder widget = new WidgetHolder(id);
        mWidgetHolderList.put(widget.id.info, widget);
        addView(widget.view);
    }
}
