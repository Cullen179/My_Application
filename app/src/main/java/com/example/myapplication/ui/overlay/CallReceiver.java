package com.example.myapplication.ui.overlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.example.myapplication.service.ScreenRecordService;

public class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                startScreenRecording(context);
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                stopScreenRecording(context);
            }
        }
    }

    private void startScreenRecording(Context context) {
        Intent serviceIntent = new Intent(context, ScreenRecordService.class);
        serviceIntent.setAction(ScreenRecordService.START_RECORDING);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    private void stopScreenRecording(Context context) {
        Intent serviceIntent = new Intent(context, ScreenRecordService.class);
        serviceIntent.setAction(ScreenRecordService.STOP_RECORDING);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
