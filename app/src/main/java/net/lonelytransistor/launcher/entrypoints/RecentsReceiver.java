package net.lonelytransistor.launcher.entrypoints;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RecentsReceiver extends BroadcastReceiver {
    private static final String TAG = "RecentsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!BackgroundService.showRecents(context)) {
            Intent intent1 = new Intent(context, RecentsActivity.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent1);
        }
        setResultCode(1);
    }
}