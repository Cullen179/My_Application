package com.example.myapplication.ui.overlay;

import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ScreenRecordingService extends Service {
    private static final String TAG = "ScreenRecordingService";
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private ScreenRecorder screenRecorder;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "START_RECORDING".equals(intent.getAction())) {
            startRecording();
        } else if (intent != null && "STOP_RECORDING".equals(intent.getAction())) {
            stopRecording();
        }
        return START_STICKY;
    }

    private void startRecording() {
        Log.d(TAG, "Starting screen recording...");
        if (mediaProjection == null) {
            Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
            captureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(captureIntent);
        }
    }

    private void stopRecording() {
        Log.d(TAG, "Stopping screen recording...");
        if (screenRecorder != null) {
            screenRecorder.stopRecording();
            screenRecorder = null;
        }
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }
}
