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
            System.out.println("Call state changed to: " + state);

            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                System.out.println("Starting screen recording...");
                startScreenRecording(context); // Start recording when call is in progress
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                System.out.println("Stopping screen recording...");
                stopScreenRecording(context);  // Stop recording when call ends
            }
        }
    }

    private void startScreenRecording(Context context) {
        System.out.println("Creating service intent for screen recording");
        Intent serviceIntent = new Intent(context, ScreenRecordingService.class);
        serviceIntent.setAction("START_RECORDING");
        serviceIntent.putExtra("resultCode", ScreenRecordingService.sResultCode);
        serviceIntent.putExtra("data", ScreenRecordingService.sData);
        System.out.println("Starting service with resultCode: " + ScreenRecordingService.sResultCode + 
                         " and data: " + (ScreenRecordingService.sData != null));
        context.startService(serviceIntent);
    }

    private void stopScreenRecording(Context context) {
        Intent serviceIntent = new Intent(context, ScreenRecordingService.class);
        serviceIntent.setAction("STOP_RECORDING");
        context.startService(serviceIntent);
    }
}
