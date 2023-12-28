package net.lonelytransistor.launcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;

import net.lonelytransistor.commonlib.OutlinedTextView;
import net.lonelytransistor.launcher.generics.GenericWindow;
import net.lonelytransistor.launcher.repos.ApkRepo;
import net.lonelytransistor.launcher.repos.JustWatch;
import net.lonelytransistor.launcher.repos.MovieRepo;
import net.lonelytransistor.launcher.repos.MovieTitle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@SuppressLint("RestrictedApi")
public class LauncherWindow extends GenericWindow {
    private static final String TAG = "LauncherWindow";

    private final Executor executor;
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

    private static final String INPUTS_ROW = "INPUTS_ROW";
    private static final String RESUME_ROW = "RESUME_ROW";
    private static final String POPULAR_MOVIES_ROW = "POPULAR_MOVIES_ROW";
    private static final String POPULAR_SERIES_ROW = "POPULAR_SERIES_ROW";
    private static final String OTHER_APPS_ROW = "OTHER_APPS_ROW";
    private static final String SETTINGS_ROW = "SETTINGS_ROW";
    private final Map<String, Integer> rows = new HashMap<>();
    private final Map<String, Long> timestamps = new HashMap<>();
    private final LauncherBar launcherBar;
    public LauncherWindow(Context ctx) {
        super(ctx, R.layout.activity_launcher_new);

        launcherBar = (LauncherBar) findViewById(R.id.launcher_bar);
        getView().setOnKeyListener(mKeyListener);
        launcherBar.setOnKeyListener(mKeyListener);
        launcherBar.setOnClickListener(v -> hide());
        executor = ctx.getMainExecutor();

        int row;
        row = launcherBar.addRow(new BadgeCard("",
                "Select\nInput", R.drawable.icon_input,
                0xFFFFFFFF,0xFF229C8F,null));
        rows.put(INPUTS_ROW, row);
        timestamps.put(INPUTS_ROW, System.currentTimeMillis());
        {
            TvInputManager inputManager = (TvInputManager) ctx.getSystemService(Context.TV_INPUT_SERVICE);
            TvView tvView = new TvView(ctx);
            for (TvInputInfo inputInfo : inputManager.getTvInputList()) {
                String id = inputInfo.getId();
                int state = inputManager.getInputState(id);
                Drawable img = inputInfo.loadIcon(ctx);
                if (state != TvInputManager.INPUT_STATE_DISCONNECTED &&
                        ApkRepo.isSystemApp(inputInfo.getServiceInfo().packageName) &&
                        img != null) {
                    BadgeCard card = new BadgeCard(String.valueOf(inputInfo.loadLabel(ctx)),
                            "", null, 0, 0,
                            img, new Card.Callback() {
                        @Override
                        public void onClicked(Card card) {
                            tvView.tune(inputInfo.getId(), TvContract.buildChannelUriForPassthroughInput(inputInfo.getId()));
                            hide();
                        }
                        @Override
                        public void onHovered(Card card, boolean hovered) {}
                    });
                    /*switch (state) {
                        case TvInputManager.INPUT_STATE_CONNECTED_STANDBY:
                            card.statusIcon = getDrawable(R.drawable.power_off);
                            break;
                        case TvInputManager.INPUT_STATE_CONNECTED:
                            card.statusIcon = getDrawable(R.drawable.running);
                            break;
                    }*/
                    launcherBar.addItem(row, card);
                }
            }
        }
        row = launcherBar.addRow(new BadgeCard("",
                        "Resume\nPlayback", R.drawable.icon_play_rounded,
                        0xFFFFFFFF,0xFFBF8C00,null));
        rows.put(RESUME_ROW, row);
        timestamps.put(RESUME_ROW, 0L);
        row = launcherBar.addRow(new BadgeCard("",
                "Popular\nMovies", R.drawable.icon_recommend,
                0xFFFFFFFF,0xFFC01F1F,null));
        rows.put(POPULAR_MOVIES_ROW, row);
        timestamps.put(POPULAR_MOVIES_ROW, 0L);
        row = launcherBar.addRow(new BadgeCard("",
                "Popular\nSeries", R.drawable.icon_recommend,
                0xFFFFFFFF,0xFF4F4FB0,null));
        rows.put(POPULAR_SERIES_ROW, row);
        timestamps.put(POPULAR_SERIES_ROW, 0L);
        for (ApkRepo.App app : ApkRepo.getPlatformApps()) {
            row = launcherBar.addRow(new BadgeCard(app.name, app.badge, new Card.Callback() {
                @Override public void onClicked(Card card) { ctx.startActivity(app.defaultIntent); }
                @Override public void onHovered(Card card, boolean hovered) {}
            }));
            rows.put(app.name, row);
            timestamps.put(app.name, 0L);
        }
        for (ApkRepo.App app : ApkRepo.getNonPlatformVideoApps()) {
            row = launcherBar.addRow(new BadgeCard(app.name, app.badge, new Card.Callback() {
                @Override public void onClicked(Card card) { ctx.startActivity(app.defaultIntent); }
                @Override public void onHovered(Card card, boolean hovered) {}
            }));
            rows.put(app.name, row);
            timestamps.put(app.name, 0L);
        }

        for (ApkRepo.App app : ApkRepo.getAudioApps()) {
            row = launcherBar.addRow(new BadgeCard(app.name, app.badge, new Card.Callback() {
                @Override public void onClicked(Card card) { ctx.startActivity(app.defaultIntent); }
                @Override public void onHovered(Card card, boolean hovered) {}
            }));
            rows.put(app.name, row);
            timestamps.put(app.name, System.currentTimeMillis());
        }
        for (ApkRepo.App app : ApkRepo.getGamingApps()) {
            row = launcherBar.addRow(new BadgeCard(app.name, app.badge, new Card.Callback() {
                @Override public void onClicked(Card card) { ctx.startActivity(app.defaultIntent); }
                @Override public void onHovered(Card card, boolean hovered) {}
            }));
            rows.put(app.name, row);
            timestamps.put(app.name, System.currentTimeMillis());
        }
        row = launcherBar.addRow(new BadgeCard("",
                "Other\nApps", R.drawable.icon_apps,
                0xFFFFFFFF,0xFF2CAAEE,null));
        rows.put(OTHER_APPS_ROW, row);
        timestamps.put(OTHER_APPS_ROW, System.currentTimeMillis());
        {
            List<ApkRepo.App> apps = new ArrayList<>(ApkRepo.getPlatformApps());
            apps.addAll(ApkRepo.getNonPlatformVideoApps());
            apps.addAll(ApkRepo.getAudioApps());
            apps.addAll(ApkRepo.getGamingApps());
            for (ApkRepo.App app : ApkRepo.getApps(null, apps)) {
                launcherBar.addItem(row, new BadgeCard(app.name, app.badge != null ? app.badge : app.icon, new Card.Callback() {
                    @Override public void onClicked(Card card) { ctx.startActivity(app.defaultIntent); }
                    @Override public void onHovered(Card card, boolean hovered) {}
                }));
            }
        }
        row = launcherBar.addRow(
                new BadgeCard("Settings", ApkRepo.getActionBadge(Settings.ACTION_SETTINGS), new Card.Callback() {
                    @Override public void onClicked(Card card) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                    @Override public void onHovered(Card card, boolean hovered) {}
                })
        );
        rows.put(SETTINGS_ROW, row);
        timestamps.put(SETTINGS_ROW, System.currentTimeMillis());
        //clockInterval = new Utils.Interval(() -> launcherBar.post(() -> updateClock()), 1000);
    }

    private void updateClock() {
        Calendar calendar = Calendar.getInstance();
        OutlinedTextView clockView = (OutlinedTextView) findViewById(R.id.clock);
        int H = calendar.get(Calendar.HOUR_OF_DAY);
        int M = 100 + calendar.get(Calendar.MINUTE);
        String HHMM = H + ":" + String.valueOf(M).substring(1);
        clockView.setText(HHMM);
        clockView.invalidate();
    }
    private void updateRows() {
        long timeNow = System.currentTimeMillis();
        int row;
        if (MovieRepo.getWatchNextTimestamp() > timestamps.get(RESUME_ROW)) {
            row = rows.get(RESUME_ROW);
            timestamps.put(RESUME_ROW, timeNow);
            launcherBar.clearRow(row);
            long dayNow = timeNow/1000/3600/24;
            for (MovieTitle movie : MovieRepo.getWatchNext()) {
                if ((dayNow - movie.system.lastWatched/1000/3600/24) < 30) {
                    launcherBar.addItem(row, new MovieCard(movie));
                }
            }
        }
        if (MovieRepo.getRecommendedTimestamp() > timestamps.get(POPULAR_MOVIES_ROW)) {
            row = rows.get(POPULAR_MOVIES_ROW);
            timestamps.put(POPULAR_MOVIES_ROW, timeNow);
            launcherBar.clearRow(row);
            for (MovieTitle movie : MovieRepo.getRecommended(JustWatch.Type.MOVIE)) {
                launcherBar.addItem(row, new MovieCard(movie));
            }
        }
        if (MovieRepo.getRecommendedTimestamp() > timestamps.get(POPULAR_SERIES_ROW)) {
            row = rows.get(POPULAR_SERIES_ROW);
            timestamps.put(POPULAR_SERIES_ROW, timeNow);
            launcherBar.clearRow(row);
            for (MovieTitle movie : MovieRepo.getRecommended(JustWatch.Type.SERIES)) {
                launcherBar.addItem(row, new MovieCard(movie));
            }
        }
        for (ApkRepo.App app : ApkRepo.getPlatformApps()) {
            if (rows.containsKey(app.name) && app.recommendationsTimestamp > timestamps.get(app.name)) {
                row = rows.get(app.name);
                launcherBar.clearRow(row);
                for (MovieTitle movie : app.recommendations) {
                    launcherBar.addItem(row, new MovieCard(movie));
                }
            }
        }
        for (ApkRepo.App app : ApkRepo.getNonPlatformVideoApps()) {
            if (rows.containsKey(app.name) && app.recommendationsTimestamp > timestamps.get(app.name)) {
                row = rows.get(app.name);
                launcherBar.clearRow(row);
                for (MovieTitle movie : app.recommendations) {
                    launcherBar.addItem(row, new MovieCard(movie));
                }
            }
        }
    }

    @Override
    public void onShow() {
        launcherBar.resetSelection();
        updateClock();
        updateRows();
    }
    @Override
    public void onHide() {

    }
}