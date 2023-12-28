package net.lonelytransistor.launcher.entrypoints;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;

import net.lonelytransistor.launcher.BadgeCard;
import net.lonelytransistor.launcher.LauncherBar;
import net.lonelytransistor.launcher.MovieCard;
import net.lonelytransistor.launcher.R;
import net.lonelytransistor.launcher.generics.GenericActivity;

public class LauncherActivity extends GenericActivity {
    private static final String TAG = "LauncherActivity";
    private static final String PERMISSION_READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS";

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int res : grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                finish();
                return;
            }
        }
        start();
    }
    private boolean isUsageAccessGranted() {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.unsafeCheckOpRaw(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid, applicationInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    private boolean isIgnoringBatteryOptimizations() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return powerManager.isIgnoringBatteryOptimizations(getPackageName());
    }
    private void start() {
        if (checkCallingOrSelfPermission(PERMISSION_READ_TV_LISTINGS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{PERMISSION_READ_TV_LISTINGS}, 0);
        } else {
            if (!isUsageAccessGranted()) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.putExtra(Intent.EXTRA_PACKAGE_NAME, getPackageName());
                startActivity(intent);
            } else if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else /*if (!isIgnoringBatteryOptimizations()) {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, Uri.parse("package:" + getPackageName()));
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else*/ {
                BackgroundService.showLauncher(this);
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (true) {
            start();
        } else if (true) {
            setContentView(R.layout.activity_main);
            LauncherBar bar = findViewById(R.id.launcherBar);

            int row = bar.addRow(new BadgeCard("Title",
                    "Test0", R.drawable.icon_apps, 0xffff5050, 0xff5050ff,
                    null, null));
            bar.addItem(row, new MovieCard(1));
            bar.addItem(row, new MovieCard(2));
            bar.addItem(row, new MovieCard(3));
            bar.addItem(row, new MovieCard(4));
            row = bar.addRow(new BadgeCard("Title",
                    "Test1", R.drawable.icon_apps, 0xffff5050, 0xff5050ff,
                    null, null));
            bar.addItem(row, new MovieCard(5));
            bar.addItem(row, new MovieCard(6));
            bar.addItem(row, new MovieCard(8));
            row = bar.addRow(new BadgeCard("Title",
                    "Test2", R.drawable.icon_apps, 0xffff5050, 0xff5050ff,
                    null, null));
            bar.addItem(row, new BadgeCard("Title",
                    "Test0", R.drawable.icon_apps, 0xffff5050, 0xff5050ff,
                    null, null));
            bar.addItem(row, new BadgeCard("Title",
                    "Test1", R.drawable.icon_apps, 0xffff5050, 0xff5050ff,
                    null, null));
            bar.addItem(row, new BadgeCard("Title",
                    "Test2", R.drawable.icon_apps, 0xffff5050, 0xff5050ff,
                    null, null));
            row = bar.addRow(new BadgeCard("Title",
                    "Test3", R.drawable.icon_apps, 0xffff5050, 0xff5050ff,
                    null, null));
            bar.addItem(row, new MovieCard(9));
        }
    }
}