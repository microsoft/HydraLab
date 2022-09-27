// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import android.media.MediaFormat;

class AudioEncoder extends BaseEncoder {
    private final AudioEncodeConfig mConfig;

    AudioEncoder(AudioEncodeConfig config) {
        super(config.codecName);
        this.mConfig = config;
    }

    @Override
    protected MediaFormat createMediaFormat() {
        return mConfig.toFormat();
    }

}
