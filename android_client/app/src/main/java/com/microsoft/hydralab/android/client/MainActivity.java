// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaCodecInfo;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.util.Range;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.Toast;

import com.microsoft.hydralab.android.client.view.NamedSpinner;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION_CODES.M;
import static com.microsoft.hydralab.android.client.ScreenRecorderService.AUDIO_AAC;
import static com.microsoft.hydralab.android.client.ScreenRecorderService.VIDEO_AVC;

public class MainActivity extends Activity {
    private static final String TAG = "ScreenRecorder";
    private static final int REQUEST_MEDIA_PROJECTION = 1;
    private static final int REQUEST_MEDIA_PROJECTION_AND_START = 3;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int CODE_REQUEST_ALERT = 5;
    // members below will be initialized in onCreate()
    private MediaProjectionManager mMediaProjectionManager;
    private Button mButton;
    private Switch mAudioSwitch;
    private ImageView mPlaceHolderView;
    private View mSettingsView;
    private NamedSpinner mVieoResolution;
    private NamedSpinner mVideoFramerate;
    private NamedSpinner mIFrameInterval;
    private NamedSpinner mVideoBitrate;
    private NamedSpinner mAudioBitrate;
    private NamedSpinner mAudioSampleRate;
    private NamedSpinner mAudioChannelCount;
    private NamedSpinner mVideoCodec;
    private NamedSpinner mAudioCodec;
    private NamedSpinner mVideoProfileLevel;
    private NamedSpinner mAudioProfile;
    private NamedSpinner mOrientation;
    private MediaCodecInfo[] mAvcCodecInfos; // avc codecs
    private MediaCodecInfo[] mAacCodecInfos; // aac codecs
    private ScreenRecorderService mService;

    private final Handler handler = new Handler();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((ScreenRecorderService.MyBinder) service).getService();
            runOnUiThread(updateButtonState);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    private final Runnable updateButtonState = () -> {
        boolean recording = mService != null && mService.isRecording();
        mButton.setEnabled(true);
        mButton.setText(recording ? getString(R.string.stop_recorder) : getString(R.string.start_recorder));
    };
    private Intent mProjectionData;
    private int mProjectionResultCode;

    /**
     * <b>NOTE:</b>
     * {@code ScreenRecorder} should run in background Service
     * instead of a foreground Activity in this demonstrate.
     */
//    private ScreenRecorder mRecorder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);
        bindRecordService();

        bindViews();

        Utils.findEncodersByTypeAsync(VIDEO_AVC, infos -> {
            logCodecInfos(infos, VIDEO_AVC);
            mAvcCodecInfos = infos;
            SpinnerAdapter codecsAdapter = createCodecsAdapter(mAvcCodecInfos);
            mVideoCodec.setAdapter(codecsAdapter);
            restoreSelections(mVideoCodec, mVieoResolution, mVideoFramerate, mIFrameInterval, mVideoBitrate);

        });
        Utils.findEncodersByTypeAsync(AUDIO_AAC, infos -> {
            logCodecInfos(infos, AUDIO_AAC);
            mAacCodecInfos = infos;
            SpinnerAdapter codecsAdapter = createCodecsAdapter(mAacCodecInfos);
            mAudioCodec.setAdapter(codecsAdapter);
            restoreSelections(mAudioCodec, mAudioChannelCount);
        });
        mAudioSwitch.setChecked(
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .getBoolean(getResources().getResourceEntryName(mAudioSwitch.getId()), false));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasPermissions()) {
            if (mProjectionData == null) {
                requestMediaProjection(REQUEST_MEDIA_PROJECTION);
            }
        } else if (Build.VERSION.SDK_INT >= M) {
            requestPermissions();
        } else {
            toast(getString(R.string.no_permission_to_write_sd_ard));
        }
        if (requestAlertWindowPermission(this)) {
            onGetWindowPermission();
        }
        if (requestIgnoreBatteryOptimizationsPermission(this)) {
            Log.d(TAG, "Battery usage: Unrestricted");
        }
    }

    private void bindRecordService() {
        bindService(new Intent(this, ScreenRecorderService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mOrientation.setSelectedPosition(1);
        } else {
            mOrientation.setSelectedPosition(0);
        }
        // reset padding
        int horizontal = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
        int vertical = (int) getResources().getDimension(R.dimen.activity_vertical_margin);
        findViewById(R.id.container).setPadding(horizontal, vertical, horizontal, vertical);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public boolean requestAlertWindowPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (hasDrawOverlayPermission(activity)) {
                return true;
            }
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
            try {
                activity.startActivityForResult(intent, CODE_REQUEST_ALERT);
            } catch (Exception e) {
                e.printStackTrace();
                startAppDetail(activity);
            }
        }
        return false;
    }

    @SuppressLint("BatteryLife")
    public boolean requestIgnoreBatteryOptimizationsPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent();
            String packageName = activity.getPackageName();
            PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(packageName))
                return true;
            else {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
        return false;
    }

    /**
     * this will redirect to app detail page to let user clean the app themselves
     */
    private void startAppDetail(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    public static boolean hasDrawOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        } else {
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_MEDIA_PROJECTION:
                updateData(resultCode, data);
                break;
            case REQUEST_MEDIA_PROJECTION_AND_START:
                updateData(resultCode, data);
                startCapturing(resultCode, data);
                break;
            case CODE_REQUEST_ALERT:
                if (requestCode == RESULT_OK) {
                    onGetWindowPermission();
                }
                break;
        }
    }

    private void onGetWindowPermission() {
        PhoneBridge.getInstance(getApplicationContext()).show();
    }

    private void updateData(int resultCode, Intent data) {
        mProjectionData = data;
        mProjectionResultCode = resultCode;
        if (mService != null) {
            mService.setProjectionData(data, resultCode);
        }
    }

    private void startCapturing(int resultCode, Intent data) {
        VideoEncodeConfig video = createVideoConfig();
        AudioEncodeConfig audio = createAudioConfig(); // audio can be null
        if (video == null) {
            toast(getString(R.string.create_screenRecorder_failure));
            return;
        }

        File dir = ScreenRecorderService.getSavingDir();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        File file = new File(dir, "Screenshots-" + format.format(new Date())
                + "-" + video.width + "x" + video.height + ".mp4");

        Intent intent = new Intent(getApplicationContext(), ScreenRecorderService.class);
        intent.setAction(CommandReceiver.ACTION_START);
        intent.putExtra("videoOption", video);
        intent.putExtra("audioOption", audio);
        intent.putExtra("dstPath", file.getAbsolutePath());
        intent.putExtra("resultCode", resultCode);
        intent.putExtra("data", data);
        startService(intent);

        handler.postDelayed(updateButtonState, 1000);
        //moveTaskToBack(true);
    }


    private AudioEncodeConfig createAudioConfig() {
        if (!mAudioSwitch.isChecked()) return null;
        String codec = getSelectedAudioCodec();
        if (codec == null) {
            return null;
        }
        int bitrate = getSelectedAudioBitrate();
        int samplerate = getSelectedAudioSampleRate();
        int channelCount = getSelectedAudioChannelCount();
        int profile = getSelectedAudioProfile();

        return new AudioEncodeConfig(codec, AUDIO_AAC, bitrate, samplerate, channelCount, profile);
    }

    private VideoEncodeConfig createVideoConfig() {
        final String codec = getSelectedVideoCodec();
        if (codec == null) {
            // no selected codec ??
            return null;
        }
        // video size
        int[] selectedWithHeight = getSelectedWithHeight();
        boolean isLandscape = isLandscape();
        int width = selectedWithHeight[isLandscape ? 0 : 1];
        int height = selectedWithHeight[isLandscape ? 1 : 0];
        int framerate = getSelectedFramerate();
        int iframe = getSelectedIFrameInterval();
        int bitrate = getSelectedVideoBitrate();
        MediaCodecInfo.CodecProfileLevel profileLevel = getSelectedProfileLevel();
        return new VideoEncodeConfig(width, height, bitrate,
                framerate, iframe, codec, VIDEO_AVC, profileLevel);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            int granted = PackageManager.PERMISSION_GRANTED;
            for (int r : grantResults) {
                granted |= r;
            }
            if (granted == PackageManager.PERMISSION_GRANTED) {
                requestMediaProjection(REQUEST_MEDIA_PROJECTION);
            } else {
                toast(getString(R.string.no_permission));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveSelections();
        mService = null;
        unbindService(serviceConnection);
        handler.removeCallbacksAndMessages(null);
    }

    private void requestMediaProjection(int code) {
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, code);
    }

    private void bindViews() {
        mButton = findViewById(R.id.record_button);
        mButton.setEnabled(false);
        mButton.setOnClickListener(this::onButtonClick);

        ImageView mDebugIcon = findViewById(R.id.debug_icon);
        mDebugIcon.setOnClickListener(this::onDebugIconClick);
        mSettingsView = findViewById(R.id.media_format_chooser);
        mPlaceHolderView = findViewById(R.id.place_holder);

        mVideoCodec = findViewById(R.id.video_codec);
        mVieoResolution = findViewById(R.id.resolution);
        mVideoFramerate = findViewById(R.id.framerate);
        mIFrameInterval = findViewById(R.id.iframe_interval);
        mVideoBitrate = findViewById(R.id.video_bitrate);
        mOrientation = findViewById(R.id.orientation);

        mAudioCodec = findViewById(R.id.audio_codec);
        mVideoProfileLevel = findViewById(R.id.avc_profile);
        mAudioBitrate = findViewById(R.id.audio_bitrate);
        mAudioSampleRate = findViewById(R.id.sample_rate);
        mAudioProfile = findViewById(R.id.aac_profile);
        mAudioChannelCount = findViewById(R.id.audio_channel_count);

        mAudioSwitch = findViewById(R.id.with_audio);
        mAudioSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                findViewById(R.id.audio_format_chooser)
                        .setVisibility(isChecked ? View.VISIBLE : View.GONE)
        );

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mOrientation.setSelectedPosition(1);
        }

        mVideoCodec.setOnItemSelectedListener((view, position) -> onVideoCodecSelected(view.getSelectedItem()));
        mAudioCodec.setOnItemSelectedListener((view, position) -> onAudioCodecSelected(view.getSelectedItem()));
        mVieoResolution.setOnItemSelectedListener((view, position) -> {
            onResolutionChanged(position, view.getSelectedItem());
        });
        mVideoFramerate.setOnItemSelectedListener((view, position) -> {
            onFramerateChanged(position, view.getSelectedItem());
        });
        mVideoBitrate.setOnItemSelectedListener((view, position) -> {
            onBitrateChanged(position, view.getSelectedItem());
        });
        mOrientation.setOnItemSelectedListener((view, position) -> {
            onOrientationChanged(position, view.getSelectedItem());
        });
    }

    private void onButtonClick(View v) {
        if (mService != null && mService.isRecording()) {
            stopRecordingAndOpenFile(v.getContext());
        } else if (hasPermissions()) {
            if (mProjectionData == null) {
                requestMediaProjection(REQUEST_MEDIA_PROJECTION_AND_START);
            } else {
                startCapturing(mProjectionResultCode, mProjectionData);
            }
        } else if (Build.VERSION.SDK_INT >= M) {
            requestPermissions();
        } else {
            toast(getString(R.string.no_permission_to_write_sd_ard));
        }
    }

    private void onDebugIconClick(View v) {
        // if this icon is clicked, display or hide mSettingsView
        mSettingsView.setVisibility(mSettingsView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        mPlaceHolderView.setVisibility(mPlaceHolderView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }


    private void cancelRecorder() {
        Toast.makeText(this, getString(R.string.permission_denied_screen_recorder_cancel), Toast.LENGTH_SHORT).show();
        handler.post(updateButtonState);
    }

    @TargetApi(M)
    private void requestPermissions() {
        String[] permissions = mAudioSwitch.isChecked()
                ? new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}
                : new String[]{WRITE_EXTERNAL_STORAGE};
        boolean showRationale = false;
        for (String perm : permissions) {
            showRationale |= shouldShowRequestPermissionRationale(perm);
        }
        if (!showRationale) {
            requestPermissions(permissions, REQUEST_PERMISSIONS);
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.using_your_mic_to_record_audio))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        requestPermissions(permissions, REQUEST_PERMISSIONS))
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    private boolean hasPermissions() {
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        int granted = (mAudioSwitch.isChecked() ? pm.checkPermission(RECORD_AUDIO, packageName) : PackageManager.PERMISSION_GRANTED)
                | pm.checkPermission(WRITE_EXTERNAL_STORAGE, packageName);
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    private void onResolutionChanged(int selectedPosition, String resolution) {
        String codecName = getSelectedVideoCodec();
        MediaCodecInfo codec = VideoEncodeConfig.getVideoCodecInfo(codecName);
        if (codec == null) return;
        MediaCodecInfo.CodecCapabilities capabilities = codec.getCapabilitiesForType(VIDEO_AVC);
        MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();
        String[] xes = resolution.split("x");
        if (xes.length != 2) throw new IllegalArgumentException();
        boolean isLandscape = isLandscape();
        int width = Integer.parseInt(xes[isLandscape ? 0 : 1]);
        int height = Integer.parseInt(xes[isLandscape ? 1 : 0]);

        double selectedFramerate = getSelectedFramerate();
        int resetPos = Math.max(selectedPosition - 1, 0);
        if (!videoCapabilities.isSizeSupported(width, height)) {
            mVieoResolution.setSelectedPosition(resetPos);
            toast(getString(R.string.codec_unsupported_size),
                    codecName, width, height, mOrientation.getSelectedItem());
            Log.w("@@", codecName +
                    " height range: " + videoCapabilities.getSupportedHeights() +
                    "\n width range: " + videoCapabilities.getSupportedHeights());
        } else if (!videoCapabilities.areSizeAndRateSupported(width, height, selectedFramerate)) {
            mVieoResolution.setSelectedPosition(resetPos);
            toast(getString(R.string.codec_unsupported_size_with_framerate),
                    codecName, width, height, mOrientation.getSelectedItem(), (int) selectedFramerate);
        }
    }

    private void onBitrateChanged(int selectedPosition, String bitrate) {
        String codecName = getSelectedVideoCodec();
        MediaCodecInfo codec = VideoEncodeConfig.getVideoCodecInfo(codecName);
        if (codec == null) return;
        MediaCodecInfo.CodecCapabilities capabilities = codec.getCapabilitiesForType(VIDEO_AVC);
        MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();
        int selectedBitrate = Integer.parseInt(bitrate) * 1000;

        int resetPos = Math.max(selectedPosition - 1, 0);
        if (!videoCapabilities.getBitrateRange().contains(selectedBitrate)) {
            mVideoBitrate.setSelectedPosition(resetPos);
            toast(getString(R.string.codec_unsupported_bitrate), codecName, selectedBitrate);
            Log.w("@@", codecName +
                    " bitrate range: " + videoCapabilities.getBitrateRange());
        }
    }

    private void onOrientationChanged(int selectedPosition, String orientation) {
        String codecName = getSelectedVideoCodec();
        MediaCodecInfo codec = VideoEncodeConfig.getVideoCodecInfo(codecName);
        if (codec == null) return;
        MediaCodecInfo.CodecCapabilities capabilities = codec.getCapabilitiesForType(VIDEO_AVC);
        MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();
        int[] selectedWithHeight = getSelectedWithHeight();
        boolean isLandscape = selectedPosition == 1;
        int width = selectedWithHeight[isLandscape ? 0 : 1];
        int height = selectedWithHeight[isLandscape ? 1 : 0];
        int resetPos = Math.max(mVieoResolution.getSelectedItemPosition() - 1, 0);
        if (!videoCapabilities.isSizeSupported(width, height)) {
            mVieoResolution.setSelectedPosition(resetPos);
            toast(getString(R.string.codec_unsupported_size),
                    codecName, width, height, orientation);
            return;
        }

        int current = getResources().getConfiguration().orientation;
        if (isLandscape && current == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (!isLandscape && current == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void onFramerateChanged(int selectedPosition, String rate) {
        String codecName = getSelectedVideoCodec();
        MediaCodecInfo codec = VideoEncodeConfig.getVideoCodecInfo(codecName);
        if (codec == null) return;
        MediaCodecInfo.CodecCapabilities capabilities = codec.getCapabilitiesForType(VIDEO_AVC);
        MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();
        int[] selectedWithHeight = getSelectedWithHeight();
        int selectedFramerate = Integer.parseInt(rate);
        boolean isLandscape = isLandscape();
        int width = selectedWithHeight[isLandscape ? 0 : 1];
        int height = selectedWithHeight[isLandscape ? 1 : 0];

        int resetPos = Math.max(selectedPosition - 1, 0);
        if (!videoCapabilities.getSupportedFrameRates().contains(selectedFramerate)) {
            mVideoFramerate.setSelectedPosition(resetPos);
            toast(getString(R.string.codec_unsupported_with_framerate), codecName, selectedFramerate);
        } else if (!videoCapabilities.areSizeAndRateSupported(width, height, selectedFramerate)) {
            mVideoFramerate.setSelectedPosition(resetPos);
            toast(getString(R.string.codec_unsupported_size_with_framerate),
                    codecName, width, height, selectedFramerate);
        }
    }

    private void onVideoCodecSelected(String codecName) {
        MediaCodecInfo codec = VideoEncodeConfig.getVideoCodecInfo(codecName);
        MediaCodecInfo.CodecCapabilities capabilities = codec.getCapabilitiesForType(VIDEO_AVC);

        resetAvcProfileLevelAdapter(capabilities);
    }


    private void resetAvcProfileLevelAdapter(MediaCodecInfo.CodecCapabilities capabilities) {
        MediaCodecInfo.CodecProfileLevel[] profiles = capabilities.profileLevels;
        if (profiles == null || profiles.length == 0) {
            mVideoProfileLevel.setEnabled(false);
            return;
        }
        mVideoProfileLevel.setEnabled(true);
        String[] profileLevels = new String[profiles.length + 1];
        profileLevels[0] = "Default";
        for (int i = 0; i < profiles.length; i++) {
            profileLevels[i + 1] = Utils.avcProfileLevelToString(profiles[i]);
        }

        SpinnerAdapter old = mVideoProfileLevel.getAdapter();
        if (!(old instanceof ArrayAdapter)) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapter.addAll(profileLevels);
            mVideoProfileLevel.setAdapter(adapter);
        } else {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) old;
            adapter.setNotifyOnChange(false);
            adapter.clear();
            adapter.addAll(profileLevels);
            adapter.notifyDataSetChanged();
        }
    }

    private void onAudioCodecSelected(String codecName) {
        MediaCodecInfo codec = getAudioCodecInfo(codecName);
        if (codec == null) {
            mAudioProfile.setAdapter(null);
            mAudioSampleRate.setAdapter(null);
            mAudioBitrate.setAdapter(null);
            return;
        }
        MediaCodecInfo.CodecCapabilities capabilities = codec.getCapabilitiesForType(AUDIO_AAC);

        resetAudioBitrateAdapter(capabilities);
        resetSampleRateAdapter(capabilities);
        resetAacProfileAdapter(capabilities);
        restoreSelections(mAudioBitrate, mAudioSampleRate, mAudioProfile);
    }

    private void resetAacProfileAdapter(MediaCodecInfo.CodecCapabilities capabilities) {
        String[] profiles = Utils.aacProfiles();
        SpinnerAdapter old = mAudioProfile.getAdapter();
        if (old == null || !(old instanceof ArrayAdapter)) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapter.addAll(profiles);
            mAudioProfile.setAdapter(adapter);
        } else {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) old;
            adapter.setNotifyOnChange(false);
            adapter.clear();
            adapter.addAll(profiles);
            adapter.notifyDataSetChanged();
        }

    }

    private void resetSampleRateAdapter(MediaCodecInfo.CodecCapabilities capabilities) {
        int[] sampleRates = capabilities.getAudioCapabilities().getSupportedSampleRates();
        List<Integer> rates = new ArrayList<>(sampleRates.length);
        int preferred = -1;
        for (int i = 0; i < sampleRates.length; i++) {
            int sampleRate = sampleRates[i];
            if (sampleRate == 44100) {
                preferred = i;
            }
            rates.add(sampleRate);
        }

        SpinnerAdapter old = mAudioSampleRate.getAdapter();
        if (!(old instanceof ArrayAdapter)) {
            ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapter.addAll(rates);
            mAudioSampleRate.setAdapter(adapter);
        } else {
            ArrayAdapter<Integer> adapter = (ArrayAdapter<Integer>) old;
            adapter.setNotifyOnChange(false);
            adapter.clear();
            adapter.addAll(rates);
            adapter.notifyDataSetChanged();
        }
        mAudioSampleRate.setSelectedPosition(preferred);
    }

    private void resetAudioBitrateAdapter(MediaCodecInfo.CodecCapabilities capabilities) {
        Range<Integer> bitrateRange = capabilities.getAudioCapabilities().getBitrateRange();
        int lower = Math.max(bitrateRange.getLower() / 1000, 80);
        int upper = bitrateRange.getUpper() / 1000;
        List<Integer> rates = new ArrayList<>();
        for (int rate = lower; rate < upper; rate += lower) {
            rates.add(rate);
        }
        rates.add(upper);

        SpinnerAdapter old = mAudioBitrate.getAdapter();
        if (old == null || !(old instanceof ArrayAdapter)) {
            ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapter.addAll(rates);
            mAudioBitrate.setAdapter(adapter);
        } else {
            ArrayAdapter<Integer> adapter = (ArrayAdapter<Integer>) old;
            adapter.setNotifyOnChange(false);
            adapter.clear();
            adapter.addAll(rates);
            adapter.notifyDataSetChanged();
        }
        mAudioSampleRate.setSelectedPosition(rates.size() / 2);
    }


    private MediaCodecInfo getAudioCodecInfo(String codecName) {
        if (codecName == null) return null;
        if (mAacCodecInfos == null) {
            mAacCodecInfos = Utils.findEncodersByType(AUDIO_AAC);
        }
        MediaCodecInfo codec = null;
        for (int i = 0; i < mAacCodecInfos.length; i++) {
            MediaCodecInfo info = mAacCodecInfos[i];
            if (info.getName().equals(codecName)) {
                codec = info;
                break;
            }
        }
        if (codec == null) return null;
        return codec;
    }

    private String getSelectedVideoCodec() {
        return mVideoCodec == null ? null : mVideoCodec.getSelectedItem();
    }

    private SpinnerAdapter createCodecsAdapter(MediaCodecInfo[] codecInfos) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, codecInfoNames(codecInfos));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private boolean isLandscape() {
        return mOrientation != null && mOrientation.getSelectedItemPosition() == 1;
    }

    private int getSelectedFramerate() {
        if (mVideoFramerate == null) throw new IllegalStateException();
        return Integer.parseInt(mVideoFramerate.getSelectedItem());
    }

    private int getSelectedVideoBitrate() {
        if (mVideoBitrate == null) throw new IllegalStateException();
        String selectedItem = mVideoBitrate.getSelectedItem(); //kbps
        return Integer.parseInt(selectedItem) * 1000;
    }

    private int getSelectedIFrameInterval() {
        return (mIFrameInterval != null) ? Integer.parseInt(mIFrameInterval.getSelectedItem()) : 5;
    }

    private MediaCodecInfo.CodecProfileLevel getSelectedProfileLevel() {
        return mVideoProfileLevel != null ? Utils.toProfileLevel(mVideoProfileLevel.getSelectedItem()) : null;
    }

    private int[] getSelectedWithHeight() {
        if (mVieoResolution == null) throw new IllegalStateException();
        String selected = mVieoResolution.getSelectedItem();
        String[] xes = selected.split("x");
        if (xes.length != 2) throw new IllegalArgumentException();
        return new int[]{Integer.parseInt(xes[0]), Integer.parseInt(xes[1])};

    }

    private String getSelectedAudioCodec() {
        return mAudioCodec == null ? null : mAudioCodec.getSelectedItem();
    }

    private int getSelectedAudioBitrate() {
        if (mAudioBitrate == null) throw new IllegalStateException();
        Integer selectedItem = mAudioBitrate.getSelectedItem();
        return selectedItem * 1000; // bps
    }

    private int getSelectedAudioSampleRate() {
        if (mAudioSampleRate == null) throw new IllegalStateException();
        return mAudioSampleRate.getSelectedItem();
    }

    private int getSelectedAudioProfile() {
        if (mAudioProfile == null) throw new IllegalStateException();
        String selectedItem = mAudioProfile.getSelectedItem();
        MediaCodecInfo.CodecProfileLevel profileLevel = Utils.toProfileLevel(selectedItem);
        return profileLevel == null ? MediaCodecInfo.CodecProfileLevel.AACObjectMain : profileLevel.profile;
    }

    private int getSelectedAudioChannelCount() {
        if (mAudioChannelCount == null) throw new IllegalStateException();
        String selectedItem = mAudioChannelCount.getSelectedItem().toString();
        return Integer.parseInt(selectedItem);
    }

    private void toast(String message, Object... args) {

        int length_toast = Locale.getDefault().getCountry().equals("BR") ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        // In Brazilian Portuguese this may take longer to read

        Toast toast = Toast.makeText(this,
                (args.length == 0) ? message : String.format(Locale.US, message, args),
                length_toast);
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(toast::show);
        } else {
            toast.show();
        }
    }

    private static String[] codecInfoNames(MediaCodecInfo[] codecInfos) {
        String[] names = new String[codecInfos.length];
        for (int i = 0; i < codecInfos.length; i++) {
            names[i] = codecInfos[i].getName();
        }
        return names;
    }

    /**
     * Print information of all MediaCodec on this device.
     */
    private static void logCodecInfos(MediaCodecInfo[] codecInfos, String mimeType) {
        for (MediaCodecInfo info : codecInfos) {
            StringBuilder builder = new StringBuilder(512);
            MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mimeType);
            builder.append("Encoder '").append(info.getName()).append('\'')
                    .append("\n  supported : ")
                    .append(Arrays.toString(info.getSupportedTypes()));
            MediaCodecInfo.VideoCapabilities videoCaps = caps.getVideoCapabilities();
            if (videoCaps != null) {
                builder.append("\n  Video capabilities:")
                        .append("\n  Widths: ").append(videoCaps.getSupportedWidths())
                        .append("\n  Heights: ").append(videoCaps.getSupportedHeights())
                        .append("\n  Frame Rates: ").append(videoCaps.getSupportedFrameRates())
                        .append("\n  Bitrate: ").append(videoCaps.getBitrateRange());
                if (VIDEO_AVC.equals(mimeType)) {
                    MediaCodecInfo.CodecProfileLevel[] levels = caps.profileLevels;

                    builder.append("\n  Profile-levels: ");
                    for (MediaCodecInfo.CodecProfileLevel level : levels) {
                        builder.append("\n  ").append(Utils.avcProfileLevelToString(level));
                    }
                }
                builder.append("\n  Color-formats: ");
                for (int c : caps.colorFormats) {
                    builder.append("\n  ").append(Utils.toHumanReadable(c));
                }
            }
            MediaCodecInfo.AudioCapabilities audioCaps = caps.getAudioCapabilities();
            if (audioCaps != null) {
                builder.append("\n Audio capabilities:")
                        .append("\n Sample Rates: ").append(Arrays.toString(audioCaps.getSupportedSampleRates()))
                        .append("\n Bit Rates: ").append(audioCaps.getBitrateRange())
                        .append("\n Max channels: ").append(audioCaps.getMaxInputChannelCount());
            }
            Log.i("@@@", builder.toString());
        }
    }

    private void restoreSelections(NamedSpinner... spinners) {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        for (NamedSpinner spinner : spinners) {
            restoreSelectionFromPreferences(preferences, spinner);
        }
    }

    private void saveSelections() {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor edit = preferences.edit();
        for (NamedSpinner spinner : new NamedSpinner[]{
                mVieoResolution,
                mVideoFramerate,
                mIFrameInterval,
                mVideoBitrate,
                mAudioBitrate,
                mAudioSampleRate,
                mAudioChannelCount,
                mVideoCodec,
                mAudioCodec,
                mAudioProfile,
        }) {
            saveSelectionToPreferences(edit, spinner);
        }
        edit.putBoolean(getResources().getResourceEntryName(mAudioSwitch.getId()), mAudioSwitch.isChecked());
        edit.apply();
    }

    private void saveSelectionToPreferences(SharedPreferences.Editor preferences, NamedSpinner spinner) {
        int resId = spinner.getId();
        String key = getResources().getResourceEntryName(resId);
        int selectedItemPosition = spinner.getSelectedItemPosition();
        if (selectedItemPosition >= 0) {
            preferences.putInt(key, selectedItemPosition);
        }
    }

    private void restoreSelectionFromPreferences(SharedPreferences preferences, NamedSpinner spinner) {
        int resId = spinner.getId();
        String key = getResources().getResourceEntryName(resId);
        int value = preferences.getInt(key, -1);
        if (value >= 0 && spinner.getAdapter() != null) {
            spinner.setSelectedPosition(value);
        }
    }

    private void stopRecordingAndOpenFile(Context context) {
        Intent service = new Intent(context, ScreenRecorderService.class);
        service.setAction(CommandReceiver.ACTION_STOP);
        startService(service);

        File file = new File(mService.getSavedPath());
        handler.postDelayed(updateButtonState, 1000);
        Toast.makeText(context, getString(R.string.recorder_stopped_saved_file) + " " + file, Toast.LENGTH_LONG).show();
        StrictMode.VmPolicy vmPolicy = StrictMode.getVmPolicy();
        try {
            // disable detecting FileUriExposure on public file
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
            viewResult(file);
        } finally {
            StrictMode.setVmPolicy(vmPolicy);
        }
    }

    private void viewResult(File file) {
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.addCategory(Intent.CATEGORY_DEFAULT);
        view.setDataAndType(Uri.fromFile(file), VIDEO_AVC);
        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(view);
        } catch (ActivityNotFoundException e) {
            // no activity can open this video
        }
    }


}
