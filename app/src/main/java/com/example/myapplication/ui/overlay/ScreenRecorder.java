package com.example.myapplication.ui.overlay;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;

public class ScreenRecorder {
    private static final String TAG = "ScreenRecorder";
    private MediaRecorder mediaRecorder;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private Context context;
    private Uri videoUri;
    private ParcelFileDescriptor pfd;

    public ScreenRecorder(Context context, MediaProjection mediaProjection) {
        this.context = context;
        this.mediaProjection = mediaProjection;
        setupMediaRecorder();
    }

    private void setupMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        try {
            // Configure MediaRecorder settings here
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(1280, 720);
            // Set output file (example: using ParcelFileDescriptor)
            if (pfd != null) {
                mediaRecorder.setOutputFile(pfd.getFileDescriptor());
            }
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Error setting up MediaRecorder", e);
            releaseMediaRecorder(); // Ensure resources are released on failure
        }
    }

    public void startRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.start();
        }
    }

    public void stopRecording() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Error stopping MediaRecorder", e);
        } finally {
            releaseMediaRecorder();
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    public void release() {
        stopRecording();
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (pfd != null) {
            try {
                pfd.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing ParcelFileDescriptor", e);
            }
            pfd = null;
        }
    }
}