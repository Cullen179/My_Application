package com.example.myapplication.ui.overlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                startScreenRecording(context); // Start recording when call is in progress
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                stopScreenRecording(context);  // Stop recording when call ends
            }
        }
    }

    private void startScreenRecording(Context context) {
        Intent serviceIntent = new Intent(context, ScreenRecordingService.class);
        serviceIntent.setAction("START_RECORDING");
        context.startService(serviceIntent);
    }

    private void stopScreenRecording(Context context) {
        Intent serviceIntent = new Intent(context, ScreenRecordingService.class);
        serviceIntent.setAction("STOP_RECORDING");
        context.startService(serviceIntent);
    }
}
