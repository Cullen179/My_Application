package com.example.myapplication.ui.overlay;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenRecordingService extends Service {
    public static final int SCREEN_RECORD_REQUEST_CODE = 1001;
    private static final String TAG = "ScreenRecordingService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "screen_recording_channel";

    // Add static fields to store permission data
    static int sResultCode = Activity.RESULT_CANCELED;
    static Intent sData = null;

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private int mScreenDensity;
    private int mScreenWidth;
    private int mScreenHeight;
    private String mOutputPath;
    private boolean mIsRecording = false;

    // Add MediaProjection callback
    private final MediaProjection.Callback mMediaProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            if (mIsRecording) {
                stopRecording();
            }
            mMediaProjection = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;

        // Create output directory if it doesn't exist
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "ScreenRecordings");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Failed to create directory");
            }
        }

        // Create output file name
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        mOutputPath = mediaStorageDir.getPath() + File.separator + "SCR_" + timestamp + ".mp4";
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        System.out.println("ScreenRecordingService received action: " + action);
        
        if ("START_RECORDING".equals(action)) {
            System.out.println("Processing START_RECORDING command");
            // Get media projection data from the intent
            int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra("data");
            System.out.println("Received resultCode: " + resultCode + ", data: " + (data != null));
            
            // Store the permission data if it's valid
            if (resultCode == Activity.RESULT_OK && data != null) {
                System.out.println("Storing valid permission data");
                sResultCode = resultCode;
                sData = data;
            }
            
            // Use stored permission data if available
            if (sResultCode == Activity.RESULT_OK && sData != null) {
                System.out.println("Starting recording with stored permission data");
                startRecording(sResultCode, sData);
            } else {
                System.out.println("Cannot start recording - missing valid permission data");
            }
        } else if ("STOP_RECORDING".equals(action)) {
            System.out.println("Processing STOP_RECORDING command");
            stopRecording();
        }
        return START_STICKY;
    }

    private void startRecording(int resultCode, Intent data) {
        if (mIsRecording) {
            return;
        }

        System.out.println("is recording");
        // Create foreground notification
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        initRecorder();

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "MediaRecorder prepare failed", e);
            releaseRecorder();
            return;
        }

        // Create media projection
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        if (mMediaProjection == null) {
            Log.e(TAG, "MediaProjection is null");
            return;
        }

        // Register the callback
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);

        // Create virtual display
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "ScreenRecording",
                mScreenWidth,
                mScreenHeight,
                mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(),
                null,
                null
        );

        // Start recording
        mMediaRecorder.start();
        mIsRecording = true;

        Log.d(TAG, "Screen recording started");
    }

    private void stopRecording() {
        if (!mIsRecording) {
            return;
        }

        mIsRecording = false;

        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.d(TAG, "Recording saved to: " + mOutputPath);

            // Notify user that recording is saved
            MediaScannerConnection.scanFile(
                    this,
                    new String[]{mOutputPath},
                    null,
                    (path, uri) -> {
                        Log.i(TAG, "Scanned " + path + " -> uri=" + uri);
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop recording", e);
        } finally {
            releaseRecorder();
            stopForeground(true);
            stopSelf();
        }
    }

    private void initRecorder() {
        mMediaRecorder = new MediaRecorder();

        // Configure MediaRecorder
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        // Video settings
        mMediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024); // 5 Mbps
        mMediaRecorder.setVideoFrameRate(30);

        // Audio settings
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioEncodingBitRate(128 * 1024); // 128 kbps
        mMediaRecorder.setAudioSamplingRate(44100);

        mMediaRecorder.setOutputFile(mOutputPath);
    }

    private void releaseRecorder() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen Recording Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Screen recording service notification");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Recording")
                .setContentText("Recording in progress...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Add a stop recording action
        Intent stopIntent = new Intent(this, ScreenRecordingService.class);
        stopIntent.setAction("STOP_RECORDING");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(android.R.drawable.ic_media_pause, "Stop Recording", stopPendingIntent);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    // Add method to request screen recording permission
    public static void requestScreenRecordingPermission(Activity activity) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) 
            activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            SCREEN_RECORD_REQUEST_CODE
        );
    }

    // Add method to handle permission result
    public static void handlePermissionResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            sResultCode = resultCode;
            sData = data;
            System.out.println("Screen recording permission granted and stored");
        } else {
            System.out.println("Screen recording permission denied");
        }
    }
}