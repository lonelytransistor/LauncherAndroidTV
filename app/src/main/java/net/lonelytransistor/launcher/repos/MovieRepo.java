package net.lonelytransistor.launcher.repos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.util.Log;

import androidx.tvprovider.media.tv.TvContractCompat;
import androidx.tvprovider.media.tv.WatchNextProgram;

import net.lonelytransistor.commonlib.Preferences;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class MovieRepo implements Serializable {
    private static final String TAG = "MovieRepo";
    private static final String PREFERENCE_KEY = "MOVIE_REPO";
    private static final String MOVIES_MAP = "MOVIES";
    private static final String ALIAS_MAP = "ALIASES";

    private static Preferences prefs = null;
    private static final Map<String,MovieTitle> movieRepoID = new HashMap<>();
    private static final Map<String,String> movieAliases = new HashMap<>();
    private static final ReentrantLock mutex = new ReentrantLock();
    private static final List<MovieTitle> watchNext = new ArrayList<>();
    private static long watchNextTimestamp = 0;
    private static final Map<JustWatch.Type, List<MovieTitle>> recommended = new HashMap<>();
    private static long recommendedTimestamp = 0;

    public static class ID {
        JustWatch.Type type = JustWatch.Type.ERROR;
        int id = 0;
        private String get() {
            return type.name() + "_" + id;
        }
        ID(JustWatch.Type type, int id) {
            this.type = type;
            this.id = id;
        }
        ID(String id) {
            if (id == null)
                return;
            String[] id_ = id.split("_", 2);
            this.type = JustWatch.Type.valueOf(id_[0]);
            this.id = Integer.parseInt(id_[1]);
        }
    }
    static MovieTitle put(ID id, MovieTitle title) {
        mutex.lock();
        MovieTitle ret = movieRepoID.put(id.get(), title);
        mutex.unlock();
        return ret;
    }
    public static MovieTitle get(ID id) {
        mutex.lock();
        MovieTitle ret = movieRepoID.get(id.get());
        mutex.unlock();
        return ret;
    }
    public static MovieTitle get(JustWatch.Type type, int id) {
        return get(new ID(type, id));
    }

    public static ID INVALID_ID = new ID(JustWatch.Type.ERROR, 0);
    public static MovieTitle INVALID_TITLE = new MovieTitle(0);
    static void putAlias(String title, ID id) {
        mutex.lock();
        movieAliases.put(title, id != null ? id.get() : INVALID_ID.get());
        mutex.unlock();
    }
    static MovieTitle getAlias(String title) {
        mutex.lock();
        String id = movieAliases.get(title);
        MovieTitle ret = null;
        if (Objects.equals(id, INVALID_ID.get())) {
            ret = INVALID_TITLE;
        } else if (id != null) {
            ret = get(new ID(id));
        }
        mutex.unlock();
        return ret;
    }

    public static void save() {
        mutex.lock();
        prefs.setMap(MOVIES_MAP, movieRepoID);
        prefs.setMap(ALIAS_MAP, movieAliases);
        mutex.unlock();
    }
    static void init(Context ctx) {
        if (prefs == null) {
            prefs = new Preferences(ctx, PREFERENCE_KEY);
        }
    }
    static void postInit(Context ctx) {
        load(ctx);
    }
    public static void load(Context ctx) {
        mutex.lock();
        movieRepoID.clear();
        movieRepoID.putAll((Map<String, MovieTitle>) prefs.getMap(MOVIES_MAP));
        movieAliases.clear();
        movieAliases.putAll((Map<String, String>) prefs.getMap(ALIAS_MAP));
        createWatchNext(ctx);
        createRecommended();
        mutex.unlock();
    }
    public static List<MovieTitle> getWatchNext() {
        return watchNext;
    }
    public static long getWatchNextTimestamp() {
        return watchNextTimestamp;
    }
    public static List<MovieTitle> getRecommended(JustWatch.Type type) {
        return recommended.getOrDefault(type, new ArrayList<>());
    }
    public static long getRecommendedTimestamp() {
        return recommendedTimestamp;
    }

    @SuppressLint("RestrictedApi") private static void createWatchNext(Context ctx) {
        Cursor cursor = ctx.getContentResolver().query(
                TvContract.WatchNextPrograms.CONTENT_URI,
                WatchNextProgram.PROJECTION,
                null, new String[]{}, null);
        assert cursor != null;
        AtomicInteger requests = new AtomicInteger(0);
        ReentrantLock mutexLocal = new ReentrantLock();
        if (cursor.moveToFirst()) do {
            WatchNextProgram prog = WatchNextProgram.fromCursor(cursor);
            String title = prog.getTitle();
            int index = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS);
            long timeNow = Math.max(0, index > 0 ? cursor.getLong(index) : 0);
            index = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_DURATION_MILLIS);
            long timeTotal = Math.max(1, index > 0 ? cursor.getLong(index) : 1);

            requests.incrementAndGet();
            JustWatch.findMovieTitle(title, new JustWatch.Callback() {
                @Override
                public void onFailure(String error) {
                    mutex.lock();
                    if (requests.decrementAndGet() == 0) {
                        watchNextTimestamp = System.currentTimeMillis();
                    }
                    mutex.unlock();
                }
                @Override
                public void onSuccess(List<MovieTitle> titles_) {
                    MovieTitle movie = titles_.get(0);
                    movie.system.pkgName = prog.getPackageName();
                    movie.system.lastWatched = prog.getLastEngagementTimeUtcMillis();
                    switch (prog.getWatchNextType()) {
                        case TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE:
                            movie.system.timeWatched = timeNow;
                            movie.system.state = (10*timeNow)/timeTotal > 9 ?
                                    MovieTitle.System.State.WATCHED : MovieTitle.System.State.WATCHING;
                            break;
                        case TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_NEXT:
                            movie.system.state = MovieTitle.System.State.NEXT;
                            break;
                        case TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_NEW:
                        case TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST:
                            movie.system.state = MovieTitle.System.State.NEW;
                            break;
                        default:
                            break;
                    }
                    MovieTitle title = titles_.get(0);
                    try {
                        Intent intent = prog.getIntent();
                        title.offers.put(ApkRepo.getPlatform(intent), intent.getDataString());
                    } catch (URISyntaxException ignored) {}

                    mutexLocal.lock();
                    watchNext.add(movie);
                    if (requests.decrementAndGet() == 0) {
                        watchNextTimestamp = System.currentTimeMillis();
                    }
                    mutexLocal.unlock();
                }
            });
        } while (cursor.moveToNext());
        cursor.close();
    }
    private static void createRecommended() {
        ReentrantLock mutexLocal = new ReentrantLock();
        for (JustWatch.Type type : JustWatch.Type.values()) {
            if (type == JustWatch.Type.ERROR) {
                continue;
            }
            if (!recommended.containsKey(type)) {
                recommended.put(type, new ArrayList<>());
            }
            JustWatch.Config cfg = new JustWatch.Config();
            cfg.count = 40;
            cfg.type = new JustWatch.Type[]{type};
            cfg.sortOrder = JustWatch.SortOrder.POPULAR;
            cfg.sortPostOrder = JustWatch.SortOrder.IMDB_SCORE;
            JustWatch.getPopularTitles(cfg, new JustWatch.Callback() {
                @Override
                public void onFailure(String error) {
                    Log.e(TAG, error);
                }
                @Override
                public void onSuccess(List<MovieTitle> titles) {
                    mutexLocal.lock();
                    recommended.get(type).addAll(titles);
                    recommendedTimestamp = System.currentTimeMillis();
                    mutexLocal.unlock();
                }
            });
        }
    }
}
