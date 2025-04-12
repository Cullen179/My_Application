package com.example.myapplication.ui.overlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            Log.d(TAG, "Call state changed to: " + state);

            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                Log.d(TAG, "Starting screen capture...");
                startScreenCapture(context);
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                Log.d(TAG, "Stopping screen capture...");
                stopScreenCapture(context);
            }
        }
    }

    private void startScreenCapture(Context context) {
        Log.d(TAG, "Creating service intent for screen capture");
        Intent serviceIntent = new Intent(context, ScreenRecordingService.class);
        serviceIntent.setAction("START_RECORDING");
        serviceIntent.putExtra("resultCode", ScreenRecordingService.sResultCode);
        serviceIntent.putExtra("data", ScreenRecordingService.sData);
        Log.d(TAG, "Starting service with resultCode: " + ScreenRecordingService.sResultCode + 
                   " and data: " + (ScreenRecordingService.sData != null));
        context.startService(serviceIntent);
    }

    private void stopScreenCapture(Context context) {
        Intent serviceIntent = new Intent(context, ScreenRecordingService.class);
        serviceIntent.setAction("STOP_RECORDING");
        context.startService(serviceIntent);
    }
}
