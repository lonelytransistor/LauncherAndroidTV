package net.lonelytransistor.launcher.entrypoints;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;

import net.lonelytransistor.launcher.generics.GenericActivity;

/*
 * Main Activity class that loads {@link MainFragment}.
 */
@SuppressLint("RestrictedApi")
public class MainActivity extends GenericActivity {
    private static final String TAG = "main";

    private static final String PERMISSION_READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkCallingOrSelfPermission(PERMISSION_READ_TV_LISTINGS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{PERMISSION_READ_TV_LISTINGS}, 0);
        }


    }
}