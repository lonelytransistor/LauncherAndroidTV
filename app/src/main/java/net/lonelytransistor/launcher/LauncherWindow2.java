package net.lonelytransistor.launcher;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.icu.util.Calendar;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.tvprovider.media.tv.PreviewChannel;
import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;
import androidx.tvprovider.media.tv.WatchNextProgram;

import net.lonelytransistor.commonlib.OutlinedTextView;
import net.lonelytransistor.launcher.generics.GenericWindow;
import net.lonelytransistor.launcher.repos.ApkRepo;
import net.lonelytransistor.launcher.repos.JustWatch;
import net.lonelytransistor.launcher.repos.MovieTitle;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

@SuppressLint("RestrictedApi")
public class LauncherWindow2 extends GenericWindow {
    private static final String TAG = "LauncherWindow";

    private final Drawable newIcon;
    private final Drawable pausedIcon;
    private final Drawable nextIcon;
    private final Drawable listIcon;
    private final Drawable androidIcon;
    private final Executor executor;
    private final List<ArrayObjectAdapter> rows = new ArrayList<>();

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
    private void appendRecommendedPlatformPriv(ArrayObjectAdapter row, JustWatch.Type type, ApkRepo.Platform platform, JustWatch.Callback cb, boolean separator) {
        JustWatch.Config cfg = new JustWatch.Config();
        cfg.count = 5;
        cfg.type = new JustWatch.Type[]{type};
        cfg.platform = new ApkRepo.Platform[]{platform};
        cfg.sortOrder = JustWatch.SortOrder.POPULAR;
        cfg.sortPostOrder = JustWatch.SortOrder.IMDB_SCORE;
        JustWatch.getPopularTitles(cfg, new JustWatch.Callback() {
            @Override
            public void onFailure(String error) {
                Log.e(TAG, error);
            }
            @Override
            public void onSuccess(List<MovieTitle> titles) {
                boolean addSeparator = separator;
                for (MovieTitle title : titles) {
                    StreamingServiceBar.Card card1 = new StreamingServiceBar.Card();
                    card1.title = title.title;
                    card1.desc = title.description;
                    card1.mainImage = title.imagePath;
                    card1.movieTitle = title;
                    card1.separator = addSeparator;
                    addSeparator = false;
                    String url = title.offers.values().toArray(new String[]{})[0];
                    card1.cb = new StreamingServiceBar.OnCardCallback() {
                        @Override
                        public void onClicked(StreamingServiceBar.Card title) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        }
                        @Override
                        public void onHovered(StreamingServiceBar.Card title, boolean hovered) {}
                    };
                    executor.execute(()-> row.add(card1));
                }
                cb.onSuccess(null);
            }
        });
    }
    private void appendRecommendedPlatform(ArrayObjectAdapter row, ApkRepo.Platform platform) {
        appendRecommendedPlatformPriv(row, JustWatch.Type.MOVIE, platform, new JustWatch.Callback() {
            @Override
            public void onFailure(String error) {}
            @Override
            public void onSuccess(List<MovieTitle> titles) {
                appendRecommendedPlatformPriv(row, JustWatch.Type.SERIES, platform, new JustWatch.Callback() {
                    @Override
                    public void onFailure(String error) {}
                    @Override
                    public void onSuccess(List<MovieTitle> titles) {}
                }, true);
            }
        },true);
    }
    private void appendTitle(ArrayObjectAdapter row, String title, String pkgName, long daysAgo, int watchNextType, Intent intent, Cursor cursor, boolean filter, List<MovieTitle> titlesBlacklist, boolean separator) {
        if (daysAgo > 30)
            return;
        JustWatch.Config cfg = new JustWatch.Config();
        int index = cursor != null ? cursor.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS) : 0;
        long timeNow = Math.max(0, index > 0 ? cursor.getLong(index) : 0);
        index = cursor != null ? cursor.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_DURATION_MILLIS) : 0;
        long timeTotal = Math.max(1, index > 0 ? cursor.getLong(index) : 1);

        int progressBar = (int) (100 * timeNow/timeTotal);
        if (watchNextType == TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE && filter && (progressBar < 5 || progressBar > 95))
            return;

        cfg.count = 1;
        cfg.query = title;
        cfg.platform = new ApkRepo.Platform[]{ApkRepo.getPlatform(pkgName)};
        JustWatch.getPopularTitles(cfg, new JustWatch.Callback() {
            @Override
            public void onFailure(String error) {
                Log.e(TAG, error);
            }
            @Override
            public void onSuccess(List<MovieTitle> titles) {
                for (MovieTitle title : titles) {
                    if (titlesBlacklist != null) {
                        if (!titlesBlacklist.contains(title)) {
                            titlesBlacklist.add(title);
                        } else {
                            continue;
                        }
                    }
                    StreamingServiceBar.Card card1 = new StreamingServiceBar.Card();
                    card1.separator = separator;
                    card1.title = title.title;
                    card1.desc = title.description;
                    card1.mainImage = title.getImage();
                    switch (watchNextType) {
                        case TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE:
                            card1.progressBar = progressBar;
                            card1.statusIcon = pausedIcon;
                            break;
                        case TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_NEXT:
                            card1.statusIcon = nextIcon;
                            break;
                        case TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_NEW:
                            card1.statusIcon = newIcon;
                            break;
                        case TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST:
                            card1.statusIcon = listIcon;
                            break;
                        default:
                            card1.statusIcon = ApkRepo.getAppIcon(pkgName);
                            break;
                    }
                    card1.movieTitle = title;
                    card1.cb = new StreamingServiceBar.OnCardCallback() {
                        @Override
                        public void onClicked(StreamingServiceBar.Card title) {
                            startActivity(intent);
                        }
                        @Override
                        public void onHovered(StreamingServiceBar.Card title, boolean hovered) {}
                    };
                    executor.execute(()->row.add(card1));
                }
            }
        });
    }
    private ArrayObjectAdapter createRecommended(StreamingServiceBar bar, StreamingServiceBar.Card card, JustWatch.Type type) {
        ArrayObjectAdapter row3 = bar.addItem(card.clone());
        JustWatch.Config cfg = new JustWatch.Config();
        cfg.count = 40;
        cfg.type = new JustWatch.Type[]{ type };
        cfg.sortOrder = JustWatch.SortOrder.POPULAR;
        cfg.sortPostOrder = JustWatch.SortOrder.IMDB_SCORE;
        JustWatch.getPopularTitles(cfg, new JustWatch.Callback() {
            @Override
            public void onFailure(String error) {
                Log.e(TAG, error);
            }
            @Override
            public void onSuccess(List<MovieTitle> titles) {
                boolean separator = true;
                for (MovieTitle title : titles) {
                    StreamingServiceBar.Card card1 = new StreamingServiceBar.Card();
                    card1.separator = separator;
                    separator = false;
                    card1.title = title.title;
                    card1.desc = title.description;
                    card1.mainImage = title.imagePath;
                    for (ApkRepo.Platform key : title.offers.keySet()) {
                        ApkRepo.App platformApp = ApkRepo.getPlatformApp(key);
                        card1.statusIcon = platformApp != null ? platformApp.icon : null;
                        if (card1.statusIcon != null) {
                            card1.movieTitle = title;
                            String url = title.offers.get(key);
                            card1.cb = new StreamingServiceBar.OnCardCallback() {
                                @Override
                                public void onClicked(StreamingServiceBar.Card title) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                                }
                                @Override
                                public void onHovered(StreamingServiceBar.Card title, boolean hovered) {}
                            };
                            break;
                        }
                    }
                    executor.execute(()->row3.add(card1));
                }
            }
        });
        return row3;
    }

    private static final List<String> CHANNEL_NONTV = Arrays.asList(
            TvContract.Channels.TYPE_OTHER,
            TvContract.Channels.TYPE_PREVIEW);
    private List<PreviewProgram> getChannelPrograms(ContentResolver resolver, long id) {
        List<PreviewProgram> programs = new ArrayList<>();
        Cursor cursor = resolver.query(
                TvContract.buildPreviewProgramsUriForChannel(id),
                PreviewProgram.PROJECTION,
                null, new String[]{}, null);
        assert cursor != null;
        if (cursor.moveToFirst()) do {
            PreviewProgram prog = PreviewProgram.fromCursor(cursor);
            if (ApkRepo.isPlatformApp(prog.getPackageName())) {
                programs.add(prog);
            }
        } while (cursor.moveToNext());
        cursor.close();

        return programs;
    }
    private List<PreviewProgram> getPreviewChannels(ContentResolver resolver) {
        List<PreviewProgram> programs = new ArrayList<>();
        Cursor cursor = resolver.query(
                TvContract.Channels.CONTENT_URI,
                PreviewChannel.Columns.PROJECTION,
                null, new String[]{}, null);
        assert cursor != null;
        if (cursor.moveToFirst()) do {
            if (CHANNEL_NONTV.contains(cursor.getString(PreviewChannel.Columns.COL_TYPE))) {
                List<PreviewProgram> prog = getChannelPrograms(resolver, cursor.getLong(PreviewChannel.Columns.COL_ID));
                programs.addAll(prog);
            }
        } while (cursor.moveToNext());
        cursor.close();
        return programs;
    }

    private final StreamingServiceBar launcherBar;
    public LauncherWindow2(Context ctx) {
        super(ctx, R.layout.activity_launcher);
        newIcon = getDrawable(R.drawable.icon_new_release);
        nextIcon = getDrawable(R.drawable.skip_next);
        listIcon = getDrawable(R.drawable.watch_list);
        pausedIcon = getDrawable(R.drawable.icon_pause);
        androidIcon = getDrawable(R.drawable.android);

        launcherBar = (StreamingServiceBar) findViewById(R.id.launcher_bar);
        getView().setOnKeyListener(mKeyListener);
        launcherBar.setOnKeyListener(mKeyListener);
        executor = ctx.getMainExecutor();
        ContentResolver resolver = getContentResolver();

        StreamingServiceBar.Card card = new StreamingServiceBar.Card();
        card.standardHeight = true;

        card.mainImage = new StreamingServiceBar.Badge(
                0xFF229C8F,
                R.drawable.icon_input, 0xFFFFFFFF,
                "Select\nInput", 0xFFFFFFFF);
        card.separator = true;
        {
            ArrayObjectAdapter row = launcherBar.addItem(card.clone());
            rows.add(row);

            TvInputManager inputManager = (TvInputManager) ctx.getSystemService(Context.TV_INPUT_SERVICE);
            TvView tvView = new TvView(ctx);
            for (TvInputInfo inputInfo : inputManager.getTvInputList()) {
                String id = inputInfo.getId();
                int state = inputManager.getInputState(id);
                if (state != TvInputManager.INPUT_STATE_DISCONNECTED && ApkRepo.isSystemApp(inputInfo.getServiceInfo().packageName)) {
                    StreamingServiceBar.Card card1 = new StreamingServiceBar.Card();
                    card1.separator = card.separator;
                    card.separator = false;
                    card1.cb = new StreamingServiceBar.OnCardCallback() {
                        @Override
                        public void onClicked(StreamingServiceBar.Card title) {
                            tvView.tune(inputInfo.getId(), TvContract.buildChannelUriForPassthroughInput(inputInfo.getId()));
                            hide();
                        }
                        @Override
                        public void onHovered(StreamingServiceBar.Card title, boolean hovered) {}
                    };
                    switch (state) {
                        case TvInputManager.INPUT_STATE_CONNECTED_STANDBY:
                            card1.statusIcon = getDrawable(R.drawable.power_off);
                            break;
                        case TvInputManager.INPUT_STATE_CONNECTED:
                            card1.statusIcon = getDrawable(R.drawable.running);
                            break;
                    }
                    card1.standardHeight = true;
                    card1.mainImage = inputInfo.loadIcon(ctx);
                    card1.title = String.valueOf(inputInfo.loadLabel(ctx));

                    row.add(card1.clone());
                }
            }
        }

        card.separator = true;
        card.mainImage = new StreamingServiceBar.Badge(
                0xFFBF8C00,
                R.drawable.icon_play_rounded, 0xFFFFFFFF,
                "Resume\nPlayback", 0xFFFFFFFF);
        {
            ArrayObjectAdapter row = launcherBar.addItem(card.clone());
            rows.add(row);

            Cursor cursor = resolver.query(
                    TvContract.WatchNextPrograms.CONTENT_URI,
                    WatchNextProgram.PROJECTION,
                    null, new String[]{}, null);
            assert cursor != null;
            if (cursor.moveToFirst()) do {
                WatchNextProgram prog = WatchNextProgram.fromCursor(cursor);
                String title = prog.getTitle();
                String pkgName = prog.getPackageName();
                long daysAgo = (System.currentTimeMillis() - prog.getLastEngagementTimeUtcMillis())/1000/3600/24;
                int watchNextType = prog.getWatchNextType();
                Intent intent = null;
                try { intent = prog.getIntent(); } catch (URISyntaxException ignored) {}
                appendTitle(row, title, pkgName, daysAgo, watchNextType, intent, cursor, true, null, card.separator);
                card.separator = false;
            } while (cursor.moveToNext());
            cursor.close();
        }
        card.separator = false;

        card.mainImage = new StreamingServiceBar.Badge(
                0xFFA6A300,
                R.drawable.icon_tv, 0xFF000000,
                "Your TV\nSuggests", 0xFF000000);
        {
            ArrayObjectAdapter row = launcherBar.addItem(card.clone());
            rows.add(row);

            List<MovieTitle> titles = new ArrayList<>();
            card.separator = true;
            for (PreviewProgram prog : getPreviewChannels(resolver)) {
                Intent intent = null;
                try { intent = prog.getIntent(); } catch (URISyntaxException ignored) {}
                appendTitle(row, prog.getTitle(), prog.getPackageName(), 0, -1, intent, null, false, titles, card.separator);
                card.separator = false;
            }
        }
        card.mainImage = new StreamingServiceBar.Badge(
                0xFFC01F1F,
                R.drawable.icon_recommend, 0xFFFFFFFF,
                "Popular\nMovies", 0xFFFFFFFF);
        {
            ArrayObjectAdapter row = createRecommended(launcherBar, card, JustWatch.Type.MOVIE);
            rows.add(row);
        }
        card.mainImage = new StreamingServiceBar.Badge(
                0xFF4F4FB0,
                R.drawable.icon_recommend, 0xFFFFFFFF,
                "Popular\nSeries", 0xFFFFFFFF);
        {
            ArrayObjectAdapter row = createRecommended(launcherBar, card, JustWatch.Type.SERIES);
            rows.add(row);
        }

        card.separator = true;
        List<ApkRepo.App> apps = new ArrayList<>(ApkRepo.getPlatformApps());
        for (ApkRepo.App app : apps) {
            StreamingServiceBar.Card card0 = card.clone();
            card.separator = false;
            card0.mainImage = app.badge;
            card0.cb = new StreamingServiceBar.OnCardCallback() {
                @Override
                public void onClicked(StreamingServiceBar.Card title) {
                    startActivity(app.defaultIntent);
                }
                @Override
                public void onHovered(StreamingServiceBar.Card title, boolean hovered) {}
            };
            ArrayObjectAdapter row = launcherBar.addItem(card0.clone());
            appendRecommendedPlatform(row, app.platform);
            rows.add(row);
        }

        card.separator = true;
        apps = new ArrayList<>(ApkRepo.getNonPlatformVideoApps());
        apps.addAll(new ArrayList<>(ApkRepo.getAudioApps()));
        apps.addAll(new ArrayList<>(ApkRepo.getGamingApps()));
        for (ApkRepo.App app : apps) {
            StreamingServiceBar.Card card0 = card.clone();
            card.separator = false;
            card0.mainImage = app.badge;
            card0.cb = new StreamingServiceBar.OnCardCallback() {
                @Override
                public void onClicked(StreamingServiceBar.Card title) {
                    startActivity(app.defaultIntent);
                }
                @Override
                public void onHovered(StreamingServiceBar.Card title, boolean hovered) {}
            };
            ArrayObjectAdapter row = launcherBar.addItem(card0.clone());
            rows.add(row);
        }

        card.separator = true;
        card.mainImage = new StreamingServiceBar.Badge(
                0xFF2CAAEE,
                R.drawable.icon_apps, 0xFFFFFFFF,
                "Other\nApps", 0xFFFFFFFF);
        {
            ArrayObjectAdapter row = launcherBar.addItem(card.clone());
            rows.add(row);

            apps.addAll(new ArrayList<>(ApkRepo.getPlatformApps()));
            for (ApkRepo.App app : ApkRepo.getApps(null, apps)) {
                StreamingServiceBar.Card card1 = new StreamingServiceBar.Card();
                card1.separator = card.separator;
                card.separator = false;
                card1.title = app.name;
                card1.standardHeight = true;
                card1.mainImage = app.icon;
                if (app.leanbackIntent == null) {
                    card1.statusIcon = androidIcon;
                }
                card1.cb = new StreamingServiceBar.OnCardCallback() {
                    @Override
                    public void onClicked(StreamingServiceBar.Card title) {
                        startActivity(app.defaultIntent);
                    }
                    @Override
                    public void onHovered(StreamingServiceBar.Card title, boolean hovered) {}
                };
                row.add(card1.clone());
            }
        }

        card.separator = false;
        card.mainImage = ApkRepo.getActionBadge(Settings.ACTION_SETTINGS);
        {
            StreamingServiceBar.Card card0 = card.clone();
            card0.cb = new StreamingServiceBar.OnCardCallback() {
                @Override
                public void onClicked(StreamingServiceBar.Card title) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
                @Override
                public void onHovered(StreamingServiceBar.Card title, boolean hovered) {}
            };
            ArrayObjectAdapter row = launcherBar.addItem(card0);
            rows.add(row);
        }/**/
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

    @Override
    public void onShow() {
        updateClock();
        launcherBar.forceFocusItem(0);
    }
    @Override
    public void onHide() {
    }
}