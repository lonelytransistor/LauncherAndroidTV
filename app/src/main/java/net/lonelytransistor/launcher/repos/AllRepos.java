package net.lonelytransistor.launcher.repos;

import android.content.Context;
import android.graphics.drawable.Drawable;

import net.lonelytransistor.launcher.R;

public class AllRepos {
    public static Drawable newIcon;
    public static Drawable pausedIcon;
    public static Drawable tickIcon;
    public static Drawable nextIcon;
    public static Drawable listIcon;
    public static Drawable androidIcon;
    public static String CACHE_DIR;
    public static void init(Context ctx) {
        newIcon = ctx.getDrawable(R.drawable.icon_new_release);
        nextIcon = ctx.getDrawable(R.drawable.skip_next);
        listIcon = ctx.getDrawable(R.drawable.watch_list);
        pausedIcon = ctx.getDrawable(R.drawable.icon_pause);
        tickIcon = ctx.getDrawable(R.drawable.icon_tick);
        androidIcon = ctx.getDrawable(R.drawable.android);
        CACHE_DIR = ctx.getCacheDir().getAbsolutePath().replaceFirst("/$","");
        JustWatch.init(ctx);
        MovieTitle.init(ctx);
        MovieRepo.init(ctx);
        ApkRepo.init(ctx);
        MovieRepo.postInit(ctx);
        ApkRepo.postInit(ctx);
    }
}
