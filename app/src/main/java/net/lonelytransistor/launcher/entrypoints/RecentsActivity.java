package net.lonelytransistor.launcher.entrypoints;

import android.os.Bundle;

import net.lonelytransistor.launcher.generics.GenericActivity;

public class RecentsActivity extends GenericActivity {
    private static final String TAG = "RecentsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BackgroundService.showRecents(this);
        finish();
        overridePendingTransition(0, 0);
    }
}