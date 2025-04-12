package com.example.myapplication.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.window.layout.WindowMetrics;
import androidx.window.layout.WindowMetricsCalculator;

import com.example.myapplication.model.ScreenRecordConfig;
import com.example.myapplication.util.NotificationHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenRecordService extends Service {

    public static final String START_RECORDING = "START_RECORDING";
    public static final String STOP_RECORDING = "STOP_RECORDING";
    public static final String KEY_RECORDING_CONFIG = "KEY_RECORDING_CONFIG";

    private static final int VIDEO_WIDTH = 720;
    private static final int VIDEO_HEIGHT = 1280;
    private static final int VIDEO_BITRATE = 512 * 1000;
    private static final int VIDEO_FRAMERATE = 30;
    private static final String VIDEO_MIME_TYPE = "video/avc"; // H.264

    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private VirtualDisplay virtualDisplay;

    private MediaCodec mediaCodec;
    private Surface inputSurface;

    private HandlerThread codecThread;
    private Handler codecHandler;

    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_STICKY;

        switch (intent.getAction()) {
            case START_RECORDING:
                startForegroundServiceWithNotification();
                ScreenRecordConfig config = intent.getParcelableExtra(KEY_RECORDING_CONFIG);
                if (config != null) {
                    startScreenCapture(config);
                }
                break;

            case STOP_RECORDING:
                stopScreenCapture();
                break;
        }
        return START_STICKY;
    }

    private void startForegroundServiceWithNotification() {
        NotificationHelper.createNotificationChannel(this);
        Notification notification = NotificationHelper.createNotification(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1, notification);
        }
    }

    private void startScreenCapture(ScreenRecordConfig config) {
        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(config.getResultCode(), config.getData());
            setupMediaCodec();
            setupVirtualDisplay();
            startReadingOutputBuffers();
            isRecording = true;
        } catch (Exception e) {
            Log.e("ScreenRecordService", "Failed to start screen capture", e);
        }
    }

    private void setupMediaCodec() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAMERATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        mediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        inputSurface = mediaCodec.createInputSurface();
        mediaCodec.start();
    }

    private void setupVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "LiveStream",
                VIDEO_WIDTH,
                VIDEO_HEIGHT,
                getResources().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface,
                null,
                null
        );
    }

    private void startReadingOutputBuffers() {
        codecThread = new HandlerThread("CodecThread");
        codecThread.start();
        codecHandler = new Handler(codecThread.getLooper());

        codecHandler.post(() -> {
            BufferInfo bufferInfo = new BufferInfo();

            while (isRecording) {
                int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                if (outputBufferId >= 0) {
                    ByteBuffer encodedData = mediaCodec.getOutputBuffer(outputBufferId);

                    if (encodedData != null && bufferInfo.size > 0) {
                        encodedData.position(bufferInfo.offset);
                        encodedData.limit(bufferInfo.offset + bufferInfo.size);

                        // ✅ Send this to your model
                        byte[] data = new byte[bufferInfo.size];
                        encodedData.get(data);
                        runModelInference(data);
                    }

                    mediaCodec.releaseOutputBuffer(outputBufferId, false);
                }
            }
        });
    }

    private void runModelInference(byte[] frameData) {
        // 🔍 Replace this with your AI inference logic
        Log.d("Model", "Received frame of size: " + frameData.length);
    }

    private void stopScreenCapture() {
        isRecording = false;

        if (codecHandler != null) {
            codecHandler.post(() -> {
                try {
                    if (mediaCodec != null) {
                        mediaCodec.stop();
                        mediaCodec.release();
                    }
                    if (inputSurface != null) {
                        inputSurface.release();
                    }
                    if (virtualDisplay != null) {
                        virtualDisplay.release();
                    }
                    if (mediaProjection != null) {
                        mediaProjection.stop();
                    }
                } catch (Exception e) {
                    Log.e("ScreenRecordService", "Failed to stop screen capture", e);
                }
            });
        }

        if (codecThread != null) {
            codecThread.quitSafely();
        }

        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
