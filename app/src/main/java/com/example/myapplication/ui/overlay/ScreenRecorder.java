//package com.example.myapplication.ui.overlay;
//
//import android.media.MediaRecorder;
//import android.media.projection.MediaProjection;
//import android.util.Log;
//import java.io.IOException;
//
//public class ScreenRecorder {
//    private static final String TAG = "ScreenRecorder";
//    private MediaRecorder mediaRecorder;
//    private MediaProjection mediaProjection;
//
//    public ScreenRecorder(MediaProjection mediaProjection) {
//        this.mediaProjection = mediaProjection;
//        setupMediaRecorder();
//    }
//
//    private void setupMediaRecorder() {
//        mediaRecorder = new MediaRecorder();
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // Capture microphone audio
//        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mediaRecorder.setOutputFile("/sdcard/recorded_call.mp4");
//        mediaRecorder.setVideoSize(1280, 720);
//        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//        mediaRecorder.setVideoFrameRate(30);
//
//        try {
//            mediaRecorder.prepare();
//        } catch (IOException e) {
//            Log.e(TAG, "MediaRecorder preparation failed", e);
//        }
//    }
//
//    public void startRecording() {
//        Log.d(TAG, "Screen recording started...");
//        mediaRecorder.start();
//    }
//
//    public void stopRecording() {
//        Log.d(TAG, "Screen recording stopped...");
//        mediaRecorder.stop();
//        mediaRecorder.reset();
//        mediaRecorder.release();
//    }
//}
