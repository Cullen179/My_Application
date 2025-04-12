package com.example.myapplication.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Objects;

public class Broadcast_Receiver extends BroadcastReceiver {
    Context context;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            assert state != null;
            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                // Incoming call detected, show notification
                System.out.println("incoming call");
                showNotification(context);
            }
        }
    }

    public void showNotification(Context context) {
        if (context == null) return;

        int notificationID = 1;
        String channelId = "incoming_call_channel";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle("Incoming Call")
                .setContentText("Someone is calling you...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//        notificationManager.notify(notificationID, builder.build());

    }
}
