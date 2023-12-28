package net.lonelytransistor.launcher.entrypoints;

import android.content.Context;
import android.content.Intent;

import net.lonelytransistor.commonlib.ForegroundService;
import net.lonelytransistor.launcher.LauncherWindow;
import net.lonelytransistor.launcher.RecentsWindow;

import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackgroundService extends ForegroundService {
    public BackgroundService() {}

    private LauncherWindow mLauncher = null;
    private RecentsWindow mRecents = null;
    private FutureTask<String> delay;
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private void init(String action) {
        delay = new FutureTask<>(() -> {
            if (mLauncher == null || mRecents == null) {
                getMainExecutor().execute(() -> {
                    mLauncher = new LauncherWindow(BackgroundService.this);
                });
                getMainExecutor().execute(() -> {
                    mRecents = new RecentsWindow(BackgroundService.this);
                });
            } else {
                switch (action) {
                    case INTENT_SHOW_LAUNCHER:
                        mLauncher.show();
                        break;
                    case INTENT_SHOW_RECENTS:
                        mRecents.show();
                        break;
                }
            }
            return action;
        });
        executor.schedule(delay, 100, TimeUnit.MILLISECONDS);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        init("");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static final String INTENT_SHOW_LAUNCHER = "net.lonelytransistor.launcher.showlauncher";
    public static final String INTENT_SHOW_RECENTS = "net.lonelytransistor.launcher.showrecents";
    public static boolean showLauncher(Context context) {
        Intent intent = new Intent(context, BackgroundService.class);
        intent.setAction(BackgroundService.INTENT_SHOW_LAUNCHER);
        try {
            BackgroundService.start(context, intent);
        } catch (IllegalStateException e) {
            return false;
        }
        return true;
    }
    public static boolean showRecents(Context context) {
        Intent intent = new Intent(context, BackgroundService.class);
        intent.setAction(BackgroundService.INTENT_SHOW_RECENTS);
        try {
            BackgroundService.start(context, intent);
        } catch (IllegalStateException e) {
            return false;
        }
        return true;
    }
    @Override
    public void onCommand(Intent intent) {
        init(intent.getAction());
    }
}