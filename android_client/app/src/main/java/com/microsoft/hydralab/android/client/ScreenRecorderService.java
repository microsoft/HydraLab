// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenRecorderService extends Service {
    public static final String VIDEO_AVC = MIMETYPE_VIDEO_AVC; // H.264 Advanced Video Coding
    public static final String AUDIO_AAC = MIMETYPE_AUDIO_AAC; // H.264 Advanced Audio Coding
    private static final String TAG = "ScreenRecorder";
    private static final String CHANNEL_ID = "RECORDER_CHANNEL_ID";
    private static final boolean VERBOSE = true;
    private static final int INVALID_INDEX = -1;
    private static final int MSG_START = 0;
    private static final int MSG_STOP = 1;
    private static final int MSG_ERROR = 2;
    private static final int STOP_WITH_EOS = 1;
    //    private final AtomicBoolean mForceQuit = new AtomicBoolean(false);
    private final AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private final LinkedList<Integer> mPendingVideoEncoderBufferIndices = new LinkedList<>();
    private final LinkedList<Integer> mPendingAudioEncoderBufferIndices = new LinkedList<>();
    private final LinkedList<MediaCodec.BufferInfo> mPendingAudioEncoderBufferInfos = new LinkedList<>();
    private final LinkedList<MediaCodec.BufferInfo> mPendingVideoEncoderBufferInfos = new LinkedList<>();
    private final IBinder mBinder = new MyBinder();
    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private String mDstPath;
    private VideoEncoder mVideoEncoder;
    private MicRecorder mAudioEncoder;
    private MediaFormat mVideoOutputFormat = null, mAudioOutputFormat = null;
    private int mVideoTrackIndex = INVALID_INDEX, mAudioTrackIndex = INVALID_INDEX;
    private MediaMuxer mMuxer;
    private boolean mMuxerStarted = false;
    private VirtualDisplay mVirtualDisplay;
    private HandlerThread mWorker;
    private CallbackHandler mHandler;
    private Callback mCallback;
    private MediaProjection mMediaProjection;
    private final MediaProjection.Callback mProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            Log.i(TAG, "MediaProjection.Callback stop");
            quit();
        }
    };
    private MediaProjectionManager mMediaProjectionManager;
    private Notifications mNotifications;
    private Intent mProjectionData;
    private int mProjectionResultCode;
    private PowerManager.WakeLock wakeLock;
    private long mVideoPtsOffset, mAudioPtsOffset;
    private Notification notification;

    public static File getSavingDir() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "test_lab");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Create dir failed!!" + dir);
            }
        }
        return dir;
    }

    public void setProjectionData(Intent data, int resultCode) {
        mProjectionData = data;
        mProjectionResultCode = resultCode;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);
        mNotifications = new Notifications(getApplicationContext());
        bindService(new Intent(getApplicationContext(), KeepAliveService.class), conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ScreenRecorderService: onStartCommand");
        int startCommand = super.onStartCommand(intent, flags, startId);

        String action = intent == null ? null : intent.getAction();
        if (CommandReceiver.ACTION_SIGNAL.equals(action)) {
            Log.i(TAG, "onStartCommand: action.SIGNAL");
            return START_STICKY;
        }
        if (CommandReceiver.ACTION_STOP.equals(action)) {
            Log.i(TAG, "onStartCommand: action.STOP");
            quit();
            return START_STICKY;
        }

        if (CommandReceiver.ACTION_START.equals(action)) {
            if (mIsRunning.get()) {
                Log.e(TAG, "onStartCommand: Failed to start screen recording because the record is running");
                return START_STICKY;
            }
            String SN = intent.getStringExtra("SNCode");
            if (SN != null) {
                String previousSN = SharedPreferencesUtils.getSharedPreferencesStringValue(
                        getApplicationContext(), SharedPreferencesUtils.SN_KEY, "unknown");
                if (!SN.equals(previousSN)) {
                    SharedPreferencesUtils.putSharedPreferencesStringValue(getApplicationContext(), SharedPreferencesUtils.SN_KEY, SN);
                    PhoneBridge.getInstance(this).updateFloatText();
                    Log.d(TAG, "onStartCommand: update SN=" + SN);
                }
            }

            VideoEncodeConfig video = (VideoEncodeConfig) intent.getSerializableExtra("videoOption");
            if (video == null) {
                video = VideoEncodeConfig.defaultConfig(getApplicationContext());
            }
            int width = intent.getIntExtra("width", 0);
            if (width > 0) {
                video.setResolutionByWidth(getApplicationContext(), width);
            }
            int bitrate = intent.getIntExtra("bitrate", 0);
            if (bitrate > 0) {
                video.setBitrate(bitrate);
            }
            int framerate = intent.getIntExtra("framerate", 0);
            if (framerate > 0) {
                video.setFramerate(framerate);
            }
            Log.i(TAG, "onStartCommand: video config: " + width + " " + bitrate + " " + framerate);
            AudioEncodeConfig audio = (AudioEncodeConfig) intent.getSerializableExtra("audioOption");
            if (audio == null) {
                Log.i(TAG, "No audio");
            }
            String dstPath = intent.getStringExtra("dstPath");
            if (dstPath == null) {
                String fileName = intent.getStringExtra("fileName");
                if (fileName == null) {
                    fileName = "recording.mp4";
                }
                dstPath = new File(getSavingDir(), fileName).getAbsolutePath();
            }

            startForeground(Notifications.id, mNotifications.createRecordingNotification());

            if (mMediaProjection == null) {
                Intent data = intent.getParcelableExtra("data");
                int resultCode = intent.getIntExtra("resultCode", 0);
                if (data == null || resultCode != Activity.RESULT_OK) {
                    data = mProjectionData;
                    resultCode = mProjectionResultCode;
                }
                if (data == null || resultCode != Activity.RESULT_OK) {
                    Exception exception = new RuntimeException("Failed to start screen recording, " +
                            "for the intent is null, or the media projection permission are not granted.");
                    Log.e(TAG, "onStartCommand: " + exception.getMessage() + Arrays.toString(exception.getStackTrace()));
                    return START_STICKY;
                }
                mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            }
            mMediaProjection.registerCallback(mProjectionCallback, new Handler());

            mVirtualDisplay = getOrCreateVirtualDisplay(mMediaProjection, video);
            mDstPath = dstPath;
            mVideoEncoder = new VideoEncoder(video);
            mAudioEncoder = audio == null ? null : new MicRecorder(audio);

            start();
        }
        return START_STICKY;
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
        quit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        startService(new Intent(getApplicationContext(), KeepAliveService.class));
    }

    private VirtualDisplay getOrCreateVirtualDisplay(MediaProjection mediaProjection, VideoEncodeConfig config) {
        if (mVirtualDisplay == null) {
            mVirtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecorder-display0",
                    config.width, config.height, 1 /*dpi*/,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    null /*surface*/, null, null);
        } else {
            // resize if size not matched
            Point size = new Point();
            mVirtualDisplay.getDisplay().getSize(size);
            if (size.x != config.width || size.y != config.height) {
                mVirtualDisplay.resize(config.width, config.height, 1);
            }
        }
        return mVirtualDisplay;
    }

    public void start() {
        if (mWorker != null) throw new IllegalStateException();
        mWorker = new HandlerThread(TAG);
        mWorker.start();
        mHandler = new CallbackHandler(mWorker.getLooper());
        mHandler.sendEmptyMessage(MSG_START);
    }

    /**
     * stop task
     */
    public final void quit() {
//        mForceQuit.set(true);
        if (!mIsRunning.get()) {
            release();
        } else {
            signalStop(false);
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.setSurface(null);
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public String getSavedPath() {
        return mDstPath;
    }

    public boolean isRecording() {
        return mIsRunning.get();
    }

    private void signalEndOfStream() {
        MediaCodec.BufferInfo eos = new MediaCodec.BufferInfo();
        ByteBuffer buffer = ByteBuffer.allocate(0);
        eos.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        if (VERBOSE) Log.i(TAG, "Signal EOS to muxer ");
        if (mVideoTrackIndex != INVALID_INDEX) {
            writeSampleData(mVideoTrackIndex, eos, buffer);
        }
        if (mAudioTrackIndex != INVALID_INDEX) {
            writeSampleData(mAudioTrackIndex, eos, buffer);
        }
        mVideoTrackIndex = INVALID_INDEX;
        mAudioTrackIndex = INVALID_INDEX;
    }

    private void record() {
        if (mIsRunning.get()) {
            throw new IllegalStateException();
        }
        if (mVirtualDisplay == null) {
            throw new IllegalStateException("maybe release");
        }
        mIsRunning.set(true);
        Log.i(TAG, "Start recording");
        acquireWakeLock();

        try {
            // create muxer
            mMuxer = new MediaMuxer(mDstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            // create encoder and input surface
            prepareVideoEncoder();
            prepareAudioEncoder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // "turn on" VirtualDisplay after VideoEncoder prepared
        mVirtualDisplay.setSurface(mVideoEncoder.getInputSurface());
        if (VERBOSE) Log.d(TAG, "set surface to display: " + mVirtualDisplay.getDisplay());
    }

    private void muxVideo(int index, MediaCodec.BufferInfo buffer) {
        if (!mIsRunning.get()) {
            Log.w(TAG, "muxVideo: Already stopped!");
            return;
        }
        if (!mMuxerStarted || mVideoTrackIndex == INVALID_INDEX) {
            mPendingVideoEncoderBufferIndices.add(index);
            mPendingVideoEncoderBufferInfos.add(buffer);
            return;
        }
        ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(index);
        writeSampleData(mVideoTrackIndex, buffer, encodedData);
        mVideoEncoder.releaseOutputBuffer(index);
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE)
                Log.d(TAG, "Stop encoder and muxer, since the buffer has been marked with EOS");
            // send release msg
            mVideoTrackIndex = INVALID_INDEX;
            signalStop(true);
        }
    }

    private void muxAudio(int index, MediaCodec.BufferInfo buffer) {
        if (!mIsRunning.get()) {
            Log.w(TAG, "muxAudio: Already stopped!");
            return;
        }
        if (!mMuxerStarted || mAudioTrackIndex == INVALID_INDEX) {
            mPendingAudioEncoderBufferIndices.add(index);
            mPendingAudioEncoderBufferInfos.add(buffer);
            return;

        }
        ByteBuffer encodedData = mAudioEncoder.getOutputBuffer(index);
        writeSampleData(mAudioTrackIndex, buffer, encodedData);
        mAudioEncoder.releaseOutputBuffer(index);
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE)
                Log.d(TAG, "Stop encoder and muxer, since the buffer has been marked with EOS");
            mAudioTrackIndex = INVALID_INDEX;
            signalStop(true);
        }
    }

    private void writeSampleData(int track, MediaCodec.BufferInfo buffer, ByteBuffer encodedData) {
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // Ignore it.
            if (VERBOSE) Log.d(TAG, "Ignoring BUFFER_FLAG_CODEC_CONFIG");
            buffer.size = 0;
        }
        boolean eos = (buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
        if (buffer.size == 0 && !eos) {
            if (VERBOSE) Log.d(TAG, "info.size == 0, drop it.");
            encodedData = null;
        } else {
            if (buffer.presentationTimeUs != 0) { // maybe 0 if eos
                if (track == mVideoTrackIndex) {
                    resetVideoPts(buffer);
                } else if (track == mAudioTrackIndex) {
                    resetAudioPts(buffer);
                }
            }
            if (VERBOSE)
                Log.d(TAG, "[" + Thread.currentThread().getId() + "] Got buffer, track=" + track
                        + ", info: size=" + buffer.size
                        + ", presentationTimeUs=" + buffer.presentationTimeUs);
            if (!eos && mCallback != null) {
                mCallback.onRecording(buffer.presentationTimeUs);
            }
        }
        if (encodedData != null) {
            encodedData.position(buffer.offset);
            encodedData.limit(buffer.offset + buffer.size);
            mMuxer.writeSampleData(track, encodedData, buffer);
            if (VERBOSE)
                Log.i(TAG, "Sent " + buffer.size + " bytes to MediaMuxer on track " + track);
        }
    }

    private void resetAudioPts(MediaCodec.BufferInfo buffer) {
        if (mAudioPtsOffset == 0) {
            mAudioPtsOffset = buffer.presentationTimeUs;
            buffer.presentationTimeUs = 0;
        } else {
            buffer.presentationTimeUs -= mAudioPtsOffset;
        }
    }

    private void resetVideoPts(MediaCodec.BufferInfo buffer) {
        if (mVideoPtsOffset == 0) {
            mVideoPtsOffset = buffer.presentationTimeUs;
            buffer.presentationTimeUs = 0;
        } else {
            buffer.presentationTimeUs -= mVideoPtsOffset;
        }
    }

    private void resetVideoOutputFormat(MediaFormat newFormat) {
        // should happen before receiving buffers, and should only happen once
        if (mVideoTrackIndex >= 0 || mMuxerStarted) {
            throw new IllegalStateException("output format already changed!");
        }
        if (VERBOSE)
            Log.i(TAG, "Video output format changed.\n New format: " + newFormat.toString());
        mVideoOutputFormat = newFormat;
    }

    private void resetAudioOutputFormat(MediaFormat newFormat) {
        // should happen before receiving buffers, and should only happen once
        if (mAudioTrackIndex >= 0 || mMuxerStarted) {
            throw new IllegalStateException("output format already changed!");
        }
        if (VERBOSE)
            Log.i(TAG, "Audio output format changed.\n New format: " + newFormat.toString());
        mAudioOutputFormat = newFormat;
    }

    private void startMuxerIfReady() {
        if (mMuxerStarted || mVideoOutputFormat == null
                || (mAudioEncoder != null && mAudioOutputFormat == null)) {
            return;
        }

        mVideoTrackIndex = mMuxer.addTrack(mVideoOutputFormat);
        mAudioTrackIndex = mAudioEncoder == null ? INVALID_INDEX : mMuxer.addTrack(mAudioOutputFormat);
        mMuxer.start();
        mMuxerStarted = true;
        if (VERBOSE) Log.i(TAG, "Started media muxer, videoIndex=" + mVideoTrackIndex);
        if (mPendingVideoEncoderBufferIndices.isEmpty() && mPendingAudioEncoderBufferIndices.isEmpty()) {
            return;
        }
        if (VERBOSE) Log.i(TAG, "Mux pending video output buffers...");
        MediaCodec.BufferInfo info;
        while ((info = mPendingVideoEncoderBufferInfos.poll()) != null) {
            int index = mPendingVideoEncoderBufferIndices.poll();
            muxVideo(index, info);
        }
        if (mAudioEncoder != null) {
            while ((info = mPendingAudioEncoderBufferInfos.poll()) != null) {
                int index = mPendingAudioEncoderBufferIndices.poll();
                muxAudio(index, info);
            }
        }
        if (VERBOSE) Log.i(TAG, "Mux pending video output buffers done.");
    }

    // @WorkerThread
    private void prepareVideoEncoder() throws IOException {
        VideoEncoder.Callback callback = new VideoEncoder.Callback() {
            boolean ranIntoError = false;

            @Override
            public void onOutputBufferAvailable(BaseEncoder codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE) Log.i(TAG, "VideoEncoder output buffer available: index=" + index);
                try {
                    muxVideo(index, info);
                } catch (Exception e) {
                    Log.e(TAG, "Muxer encountered an error! ", e);
                    Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
                }
            }

            @Override
            public void onError(Encoder codec, Exception e) {
                ranIntoError = true;
                Log.e(TAG, "VideoEncoder ran into an error! ", e);
                Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
            }

            @Override
            public void onOutputFormatChanged(BaseEncoder codec, MediaFormat format) {
                resetVideoOutputFormat(format);
                startMuxerIfReady();
            }
        };
        mVideoEncoder.setCallback(callback);
        mVideoEncoder.prepare();
    }

    private void prepareAudioEncoder() throws IOException {
        final MicRecorder micRecorder = mAudioEncoder;
        if (micRecorder == null) return;
        AudioEncoder.Callback callback = new AudioEncoder.Callback() {
            boolean ranIntoError = false;

            @Override
            public void onOutputBufferAvailable(BaseEncoder codec, int index, MediaCodec.BufferInfo info) {
                if (VERBOSE)
                    Log.i(TAG, "[" + Thread.currentThread().getId() + "] AudioEncoder output buffer available: index=" + index);
                try {
                    muxAudio(index, info);
                } catch (Exception e) {
                    Log.e(TAG, "Muxer encountered an error! ", e);
                    Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
                }
            }

            @Override
            public void onOutputFormatChanged(BaseEncoder codec, MediaFormat format) {
                if (VERBOSE)
                    Log.d(TAG, "[" + Thread.currentThread().getId() + "] AudioEncoder returned new format " + format);
                resetAudioOutputFormat(format);
                startMuxerIfReady();
            }

            @Override
            public void onError(Encoder codec, Exception e) {
                ranIntoError = true;
                Log.e(TAG, "MicRecorder ran into an error! ", e);
                Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
            }
        };
        micRecorder.setCallback(callback);
        micRecorder.prepare();
    }

    private void signalStop(boolean stopWithEOS) {
        Message msg = Message.obtain(mHandler, MSG_STOP, stopWithEOS ? STOP_WITH_EOS : 0, 0);
        mHandler.sendMessageAtFrontOfQueue(msg);
    }

    private void stopEncoders() {
        mIsRunning.set(false);
        Log.i(TAG, "Stop recording");
        releaseWakeLock();
        mPendingAudioEncoderBufferInfos.clear();
        mPendingAudioEncoderBufferIndices.clear();
        mPendingVideoEncoderBufferInfos.clear();
        mPendingVideoEncoderBufferIndices.clear();
        // maybe called on an error has been occurred
        try {
            if (mVideoEncoder != null) mVideoEncoder.stop();
        } catch (IllegalStateException e) {
            // ignored
        }
        try {
            if (mAudioEncoder != null) mAudioEncoder.stop();
        } catch (IllegalStateException e) {
            // ignored
        }

    }

    private void acquireWakeLock() {
        if (getPackageManager().checkPermission("android.permission.WAKE_LOCK", getPackageName()) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "start wake lock");
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ScreenRecorderService.class.getName());
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
            Log.d(TAG, "stop wake lock");
            wakeLock.release();
        }
    }

    private void release() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.setSurface(null);
            mVirtualDisplay = null;
        }

        mVideoOutputFormat = mAudioOutputFormat = null;
        mVideoTrackIndex = mAudioTrackIndex = INVALID_INDEX;
        mMuxerStarted = false;

        if (mWorker != null) {
            mWorker.quitSafely();
            mWorker = null;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.release();
            mAudioEncoder = null;
        }

        if (mMuxer != null) {
            try {
                mMuxer.stop();
                mMuxer.release();
            } catch (Exception e) {
                // ignored
            }
            mMuxer = null;
        }
        mHandler = null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (mVirtualDisplay != null) {
            Log.e(TAG, "release() not called!");
            release();
        }
    }

    interface Callback {
        void onStop(Throwable error);

        void onStart();

        void onRecording(long presentationTimeUs);
    }

    public class MyBinder extends Binder {
        ScreenRecorderService getService() {
            return ScreenRecorderService.this;
        }
    }

    private class CallbackHandler extends Handler {
        CallbackHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START:
                    try {
                        record();
                        if (mCallback != null) {
                            mCallback.onStart();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case MSG_STOP:
                    stopEncoders();
                    if (msg.arg1 != STOP_WITH_EOS) signalEndOfStream();
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            .addCategory(Intent.CATEGORY_DEFAULT)
                            .setData(Uri.fromFile(new File(mDstPath)));
                    sendBroadcast(intent);
                    release();
                    break;
                case MSG_ERROR:
                    stopEncoders();
                    if (msg.arg1 != STOP_WITH_EOS) signalEndOfStream();
                    Log.e("@@", "Recorder error ! See logcat for more details");
                    ((Throwable) msg.obj).printStackTrace();
                    new File(mDstPath).delete();
                    release();
                    break;
            }
        }
    }


}
