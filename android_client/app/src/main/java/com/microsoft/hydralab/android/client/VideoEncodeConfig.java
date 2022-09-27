// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import static com.microsoft.hydralab.android.client.ScreenRecorderService.VIDEO_AVC;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.Serializable;
import java.util.Objects;

public class VideoEncodeConfig implements Serializable {
    private static final String TAG = VideoEncodeConfig.class.getSimpleName();
    private static final int DEFAULT_WIDTH = 640;
    int width;
    int height;
    int bitrate;
    int framerate;
    int iframeInterval;
    final String codecName;
    final String mimeType;
    final MediaCodecInfo.CodecProfileLevel codecProfileLevel;
    private static MediaCodecInfo[] mAvcCodecInfos;

    /**
     * @param codecName         selected codec name, maybe null
     * @param mimeType          video MIME type, cannot be null
     * @param codecProfileLevel profile level for video encoder nullable
     */
    public VideoEncodeConfig(int width, int height, int bitrate,
                             int framerate, int iframeInterval,
                             String codecName, String mimeType,
                             MediaCodecInfo.CodecProfileLevel codecProfileLevel) {
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
        this.framerate = framerate;
        this.iframeInterval = iframeInterval;
        this.codecName = codecName;
        this.mimeType = Objects.requireNonNull(mimeType);
        this.codecProfileLevel = codecProfileLevel;
    }

    public static VideoEncodeConfig defaultConfig(Context context) {

        MediaCodecInfo codec = getCodecInfos()[0];
        MediaCodecInfo.CodecCapabilities capabilities = codec.getCapabilitiesForType(VIDEO_AVC);
        MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();

        int width = DEFAULT_WIDTH;
        int height = (int) (width * 1f * PhoneBridge.getScreenHeight(context) / PhoneBridge.getScreenWidth(context));
        int heightAlignment = videoCapabilities.getHeightAlignment();
        height = height + heightAlignment - height % heightAlignment;

        Log.i(TAG, width + ", " + height + ", " + heightAlignment);


        return new VideoEncodeConfig(
                width, height, 1200000,
                15, 1, null, ScreenRecorderService.VIDEO_AVC, null
        );
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public void setResolutionByWidth(Context context, int width) {
        MediaCodecInfo codec = getCodecInfos()[0];
        MediaCodecInfo.CodecCapabilities capabilities = codec.getCapabilitiesForType(VIDEO_AVC);
        MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();

        width = width <= 0 ? DEFAULT_WIDTH : width;
        int height = (int) (width * 1f * PhoneBridge.getScreenHeight(context) / PhoneBridge.getScreenWidth(context));
        int heightAlignment = videoCapabilities.getHeightAlignment();
        height = height + heightAlignment - height % heightAlignment;

        this.width = width;
        this.height = height;
    }

    public void setFramerate(int framerate) {
        this.framerate = framerate;
    }

    public static MediaCodecInfo getVideoCodecInfo(String codecName) {
        getCodecInfos();
        MediaCodecInfo codec = null;
        for (int i = 0; i < mAvcCodecInfos.length; i++) {
            MediaCodecInfo info = mAvcCodecInfos[i];
            if (info.getName().equals(codecName)) {
                codec = info;
                break;
            }
        }
        return codec;
    }

    private static MediaCodecInfo[] getCodecInfos() {
        if (mAvcCodecInfos == null) {
            mAvcCodecInfos = Utils.findEncodersByType(VIDEO_AVC);
        }
        return mAvcCodecInfos;
    }

    MediaFormat toFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(mimeType, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iframeInterval);
        if (codecProfileLevel != null && codecProfileLevel.profile != 0 && codecProfileLevel.level != 0) {
            format.setInteger(MediaFormat.KEY_PROFILE, codecProfileLevel.profile);
            format.setInteger("level", codecProfileLevel.level);
        }
        // maybe useful
        // format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 10_000_000);
        return format;
    }

    @Override
    public String toString() {
        return "VideoEncodeConfig{" +
                "width=" + width +
                ", height=" + height +
                ", bitrate=" + bitrate +
                ", framerate=" + framerate +
                ", iframeInterval=" + iframeInterval +
                ", codecName='" + codecName + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", codecProfileLevel=" + (codecProfileLevel == null ? "" : Utils.avcProfileLevelToString(codecProfileLevel)) +
                '}';
    }
}
