package net.lonelytransistor.launcher.repos;

import android.annotation.SuppressLint;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.os.SystemClock;
import android.util.Log;
import android.view.WindowManager;

import androidx.tvprovider.media.tv.PreviewChannel;
import androidx.tvprovider.media.tv.PreviewProgram;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ApkRepo extends BroadcastReceiver {
    private static final String TAG = "ApkRepo";
    @Override
    public void onReceive(Context context, Intent intent) {
        packageRepo.clear();
        initPriv(context);
    }


    private static final String[] PlatformPackageNames = new String[]{
            "com.disney.disneyplus", "com.netflix.ninja", "com.apple.atve.androidtv.appletv", "com.apple.atve.androidtv.appletv", "com.google.android.videos",
            "com.amazon.amazonvideo.livingroom", "pl.tvn.avod.tv", "com.mubi", "com.nst.iptvhorizon", "com.liskovsoft.smartyoutubetv2.tv",
            "com.curiosity.curiositystream.androidtv", "", "com.yaddo.app", "pl.tvn.player.tv", "com.app.guidedoc.tv",
            "com.spamflix.tv", "tv.vhx.worldofwonder", "", "com.abide.magellantv", "com.twentyfouri.app.broadwayhdatvbigscreen",
            "com.filmzie.platform", "tv.vhx.dekkoo", "", "", "com.viewlift.hoichoi",
            "com.viaplay.android", "", "", "com.hbo.hbonow", "com.hbo.hbonow",
            "com.spiintl.tv.filmbox", "", "com.suntv.sunnxt", "", "com.showtime.standalone",
            "com.crunchyroll.crunchyroid", "com.amazon.amazonvideo.livingroom","com.amazon.amazonvideo.livingroom", "net.mbc.shahidTV", "com.amazon.amazonvideo.livingroom",
            "ERROR"
    };
    private static final String[] PlatformNames = {
            "Disney Plus", "Netflix", "Apple TV", "Apple TV Plus", "Google Play Movies",
            "Amazon Prime Video", "VOD Poland", "MUBI", "Horizon", "YouTube",
            "Curiosity Stream", "Chili", "DOCSVILLE", "Player", "GuideDoc",
            "Spamflix", "WOW Presents Plus", "IPLA", "Magellan TV", "BroadwayHD",
            "Filmzie", "Dekkoo", "True Story", "DocAlliance Films", "Hoichoi",
            "Viaplay", "Eventive", "Cultpix", "HBO Max", "HBO Max Free",
            "FilmBox+", "Takflix", "Sun Nxt", "Classix", "SkyShowtime",
            "Crunchyroll", "Amazon Video", "Film Total Amazon Channel", "Shahid VIP", "MGM Amazon Channel",
            "ERROR"
    };
    public enum Platform {
        DISNEY_PLUS, NETFLIX, APPLE_TV, APPLE_TV_PLUS, GOOGLE_PLAY_MOVIES,
        AMAZON_PRIME_VIDEO, VOD_POLAND, MUBI, HORIZON, YOUTUBE,
        CURIOSITY_STREAM, CHILI, DOCSVILLE, PLAYER, GUIDEDOC,
        SPAMFLIX, WOW_PRESENTS_PLUS, IPLA, MAGELLAN_TV, BROADWAY_HD,
        FILMZIE, DEKKOO, TRUE_STORY, DOCALLIANCE_FILMS, HOICHOI,
        VIAPLAY, EVENTIVE, CULTPIX, HBO_MAX, HBO_MAX_FREE,
        FILMBOX_PLUS, TAKFLIX, SUN_NXT, CLASSIX, SKYSHOWTIME,
        CRUNCHYROLL, AMAZON_VIDEO, FILM_TOTAL_AMAZON_CHANNEL, SHAHID_VIP, MGM_AMAZON_CHANNEL,
        ERROR
    };
    public static Platform getPlatform(String plaformStr) {
        for (int ix = 0; ix < PlatformPackageNames.length; ix++) {
            if (PlatformPackageNames[ix].equals(plaformStr) ||
                    PlatformNames[ix].contains(plaformStr)) {
                return Platform.values()[ix];
            }
        }
        return Platform.ERROR;
    }
    public static Platform getPlatform(Intent intent) {
        intent.getDataString();
        ComponentName info = intent.resolveActivity(mPM);
        return getPlatform(info.getPackageName());
    }
    public static String getPlatformName(Platform platform) {
        return PlatformNames[platform.ordinal()];
    }
    public static App getPlatformApp(Platform platform) {
        return platformPackageRepo.get(PlatformPackageNames[platform.ordinal()]);
    }

    public static class App {
        public Intent launchIntent;
        public Intent leanbackIntent;
        public Intent mainIntent;
        public Intent defaultIntent;
        public String pkgName;
        public String name;
        public Platform platform;

        public Drawable icon;
        public Drawable badge;
        public List<MovieTitle> recommendations = new ArrayList<>();
        public long recommendationsTimestamp = 0;
    }
    public static Collection<App> getAllApps() {
        return packageRepo.values();
    }
    public static List<App> getApps(List<?> whitelist, List<?> blacklist) {
        List<App> apps = new ArrayList<>();
        if (whitelist != null && !whitelist.isEmpty()) {
            if (whitelist.get(0) instanceof String) {
                for (String pkg : packageRepo.keySet()) {
                    if (whitelist.contains(pkg)) {
                        apps.add(packageRepo.get(pkg));
                    }
                }
            } else if (whitelist.get(0) instanceof App) {
                for (App pkg : packageRepo.values()) {
                    if (whitelist.contains(pkg)) {
                        apps.add(pkg);
                    }
                }
            }
        } else if (blacklist != null && !blacklist.isEmpty()) {
            if (blacklist.get(0) instanceof String) {
                for (String pkg : packageRepo.keySet()) {
                    if (!blacklist.contains(pkg)) {
                        apps.add(packageRepo.get(pkg));
                    }
                }
            } else if (blacklist.get(0) instanceof App) {
                for (App pkg : packageRepo.values()) {
                    if (!blacklist.contains(pkg)) {
                        apps.add(pkg);
                    }
                }
            }
        }
        return apps;
    }
    public static Drawable getAppBadge(String packageName) {
        if (packageRepo.containsKey(packageName)) {
            return packageRepo.get(packageName).badge;
        }
        return null;
    }
    public static Drawable getAppIcon(String packageName) {
        if (packageRepo.containsKey(packageName)) {
            return packageRepo.get(packageName).icon;
        }
        return null;
    }
    public static Drawable getActionBadge(String action) {
        Intent intent = new Intent(action);
        List<ResolveInfo> activities = mPM.queryIntentActivities(intent, 0);
        if (!activities.isEmpty()) {
            for (ResolveInfo info : activities) {
                Drawable badge = getAppBadge(info.activityInfo.packageName);
                if (badge != null) {
                    return badge;
                }
                badge = getAppIcon(info.activityInfo.packageName);
                if (badge != null) {
                    return badge;
                }
            }
        }
        return null;
    }


    private static UsageStatsManager mUSM = null;
    private static PackageManager mPM = null;
    private static WindowManager mWM = null;
    private static ContentResolver mCR = null;
    private static Map<String, App> packageRepo = new HashMap<>();
    private static void initPriv(Context ctx) {
        if (mWM == null) {
            mWM = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        }
        if (mUSM == null) {
            mUSM = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
        }
        if (mPM == null) {
            mPM = ctx.getPackageManager();
        }
        if (mCR == null) {
            mCR = ctx.getContentResolver();
        }
        if (packageRepo.isEmpty()) {
            packageRepo = new HashMap<>();

            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            //intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            //intent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<String> platformApps = Arrays.asList(PlatformPackageNames);
            List<String> videoApps = Arrays.asList("org.jellyfin.androidtv", "org.videolan.vlc", "tv.twitch.android.app", "com.plexapp.android", "com.peacocktv.peacockandroid", "com.sling", "in.startv.hotstar", "com.sonyliv", "com.graymatrix.did", "com.tru", "com.wemesh.android", "com.jio.media.ondemand", "com.stremio", "com.haystack.android", "com.espn.score_center",
                    "com.britbox.tv", "com.teamsmart.videomanager.tv", "com.google.android.youtube.tv");
            List<String> audioApps = Arrays.asList("com.spotify.tv.android", "com.soundcloud.android", "com.amazon.music.tv", "com.google.android.youtube.tvmusic", "com.aspiro.tidal", "tunein.player", "com.pandora.android.atv");
            List<String> gamingApps = Arrays.asList("com.valvesoftware.steamlink", "com.retroarch", "com.nvidia.geforcenow");
            List<ResolveInfo> packagesResolve = mPM.queryIntentActivities(intent, 0);
            for (ResolveInfo pkg : packagesResolve) {
                App app = new App();
                app.pkgName = pkg.activityInfo.packageName;
                app.name = String.valueOf(pkg.activityInfo.loadLabel(mPM));
                Drawable icon = pkg.activityInfo.loadBanner(mPM);
                if (icon == null)
                    icon = pkg.activityInfo.loadIcon(mPM);
                if (icon == null)
                    icon = pkg.activityInfo.loadLogo(mPM);
                app.icon = icon;
                app.launchIntent = mPM.getLaunchIntentForPackage(app.pkgName);
                if (app.launchIntent != null)
                    app.launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                app.leanbackIntent = mPM.getLeanbackLaunchIntentForPackage(app.pkgName);
                if (app.leanbackIntent != null)
                    app.leanbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                if (pkg.activityInfo.targetActivity != null) {
                    app.mainIntent = new Intent(Intent.ACTION_MAIN);
                    app.mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.setComponent(new ComponentName(app.pkgName, pkg.activityInfo.targetActivity));
                }
                app.defaultIntent = app.leanbackIntent != null ? app.leanbackIntent : (app.launchIntent != null ? app.launchIntent : app.mainIntent);
                try { app.badge = mPM.getApplicationBanner(app.pkgName); } catch (Exception ignored){}
                packageRepo.put(pkg.activityInfo.packageName, app);

                if (platformApps.contains(pkg.activityInfo.packageName)) {
                    app.platform = Platform.values()[platformApps.indexOf(pkg.activityInfo.packageName)];
                    platformPackageRepo.put(pkg.activityInfo.packageName, app);
                }
                if (platformApps.contains(pkg.activityInfo.packageName) || videoApps.contains(pkg.activityInfo.packageName))
                    videoPackageRepo.put(pkg.activityInfo.packageName, app);
                if (!platformApps.contains(pkg.activityInfo.packageName) && videoApps.contains(pkg.activityInfo.packageName))
                    nonplatformVideoPackageRepo.put(pkg.activityInfo.packageName, app);
                if (audioApps.contains(pkg.activityInfo.packageName))
                    audioPackageRepo.put(pkg.activityInfo.packageName, app);
                if (gamingApps.contains(pkg.activityInfo.packageName))
                    gamingPackageRepo.put(pkg.activityInfo.packageName, app);
            }
        }
    }
    private static final List<String> CHANNEL_NONTV = Arrays.asList(
            TvContract.Channels.TYPE_OTHER,
            TvContract.Channels.TYPE_PREVIEW);
    @SuppressLint("RestrictedApi") private static List<PreviewProgram> getChannelPrograms(ContentResolver resolver, long id) {
        List<PreviewProgram> programs = new ArrayList<>();
        Cursor cursor = resolver.query(
                TvContract.buildPreviewProgramsUriForChannel(id),
                PreviewProgram.PROJECTION,
                null, new String[]{}, null);
        assert cursor != null;
        if (cursor.moveToFirst()) do {
            PreviewProgram prog = PreviewProgram.fromCursor(cursor);
            programs.add(prog);
        } while (cursor.moveToNext());
        cursor.close();

        return programs;
    }
    static void postInit(Context ctx) {
        getPreviewChannels();
    }
    @SuppressLint("RestrictedApi") static void getPreviewChannels() {
        List<PreviewProgram> programs = new ArrayList<>();
        Cursor cursor = mCR.query(
                TvContract.Channels.CONTENT_URI,
                PreviewChannel.Columns.PROJECTION,
                null, new String[]{}, null);
        assert cursor != null;
        if (cursor.moveToFirst()) do {
            if (CHANNEL_NONTV.contains(cursor.getString(PreviewChannel.Columns.COL_TYPE))) {
                List<PreviewProgram> prog = getChannelPrograms(mCR, cursor.getLong(PreviewChannel.Columns.COL_ID));
                programs.addAll(prog);
            }
        } while (cursor.moveToNext());
        cursor.close();

        Map<String, AtomicInteger> requests = new HashMap<>();
        ReentrantLock mutexLocal = new ReentrantLock();
        for (PreviewProgram prog : programs) {
            String pkgName = prog.getPackageName();
            if (!packageRepo.containsKey(pkgName)) {
                Log.i(TAG, "No pkg: " + pkgName);
                continue;
            }
            if (!requests.containsKey(pkgName)) {
                requests.put(pkgName, new AtomicInteger(0));
            }
            requests.get(pkgName).incrementAndGet();
            JustWatch.Callback cb = new JustWatch.Callback() {
                @Override
                public void onFailure(String error) {
                    MovieTitlePriv title = new MovieTitlePriv();
                    String url = String.valueOf(prog.getThumbnailUri());
                    Checksum crc32 = new CRC32();
                    crc32.update(url.getBytes(), 0, url.length());
                    File image = new File(AllRepos.CACHE_DIR, String.valueOf(crc32.getValue()));
                    JustWatch.downloadImage(url, image, new JustWatch.Callback() {
                        @Override
                        public void onFailure(String error) {
                            mutexLocal.lock();
                            if (requests.get(pkgName).decrementAndGet() == 0) {
                                packageRepo.get(pkgName).recommendationsTimestamp = System.currentTimeMillis();
                            }
                            mutexLocal.unlock();
                        }
                        @Override
                        public void onSuccess(List<MovieTitle> titles_) {
                            title.imageUrl = url;
                            title.imagePath = image.getAbsolutePath();
                            title.description = prog.getDescription();
                            title.title = prog.getTitle();
                            try {
                                Intent intent = prog.getIntent();
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                title.offers.put(ApkRepo.getPlatform(intent), intent.getDataString());
                            } catch (URISyntaxException ignored) {}
                            mutexLocal.lock();
                            MovieTitle title1 = new MovieTitle(title);
                            if (!packageRepo.get(pkgName).recommendations.contains(title1)) {
                                packageRepo.get(pkgName).recommendations.add(new MovieTitle(title1));
                            }
                            if (requests.get(pkgName).decrementAndGet() == 0) {
                                packageRepo.get(pkgName).recommendationsTimestamp = System.currentTimeMillis();
                            }
                            mutexLocal.unlock();
                        }
                    });
                }
                @Override
                public void onSuccess(List<MovieTitle> titles_) {
                    MovieTitle title = titles_.get(0);
                    try {
                        Intent intent = prog.getIntent();
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        title.offers.put(getPlatform(intent), intent.getDataString());
                    } catch (URISyntaxException e) {
                        Log.w("ApkRepo", "URISyntaxException: " + e);
                        title.offers.put(Platform.ERROR, "");
                    }
                    mutexLocal.lock();
                    if (!packageRepo.get(pkgName).recommendations.contains(title)) {
                        packageRepo.get(pkgName).recommendations.add(title);
                    }
                    if (requests.get(pkgName).decrementAndGet() == 0) {
                        packageRepo.get(pkgName).recommendationsTimestamp = System.currentTimeMillis();
                    }
                    mutexLocal.unlock();
                }
            };
            if (platformPackageRepo.containsKey(pkgName)) {
                JustWatch.findMovieTitle(prog.getTitle(), cb);
            } else {
                cb.onFailure("");
            }
        }
    }

    private static final Map<String, App> platformPackageRepo = new HashMap<>();
    private static final Map<String, App> nonplatformVideoPackageRepo = new HashMap<>();
    private static final Map<String, App> videoPackageRepo = new HashMap<>();
    private static final Map<String, App> audioPackageRepo = new HashMap<>();
    private static final Map<String, App> gamingPackageRepo = new HashMap<>();
    public static Collection<App> getNonPlatformVideoApps() {
        return nonplatformVideoPackageRepo.values();
    }
    public static Collection<App> getPlatformApps() {
        return platformPackageRepo.values();
    }
    public static Collection<App> getVideoApps() {
        return videoPackageRepo.values();
    }
    public static Collection<App> getAudioApps() {
        return audioPackageRepo.values();
    }
    public static Collection<App> getGamingApps() {
        return gamingPackageRepo.values();
    }
    public static boolean isPlatformApp(String pkg) {
        return platformPackageRepo.containsKey(pkg);
    }
    public static boolean isSystemApp(String packageName) {
        try {
            ApplicationInfo applicationInfo = mPM.getApplicationInfo(packageName, 0);
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    public static void init(Context ctx) {
        initPriv(ctx);
    }

    public static List<App> getRecentApps() {
        Date date = new Date();

        List<UsageStats> queryUsageStats = mUSM.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, date.getTime() - SystemClock.uptimeMillis(), date.getTime());
        queryUsageStats.sort((o1, o2) ->
                Long.compare(o2.getLastTimeUsed(), o1.getLastTimeUsed()));

        List<App> recentPkgs = new ArrayList<>();
        for (UsageStats pkgStats : queryUsageStats) {
            if (pkgStats.getLastTimeUsed() > 0 && pkgStats.getTotalTimeInForeground() > 5000) {
                App app = packageRepo.get(pkgStats.getPackageName());
                if (app != null && !recentPkgs.contains(app)) {
                    recentPkgs.add(app);
                }
            }
        }
        return recentPkgs;
    }
}
