package com.osuobiem.mobiletracker.controls;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;

public class TrackOnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent i = new Intent(context, TrackerService.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ContextCompat.startForegroundService(context, i);
        }
    }
}
