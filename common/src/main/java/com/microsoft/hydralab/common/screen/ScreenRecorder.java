// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.screen;

public interface ScreenRecorder {
    void setupDevice();

    void startRecord(int maxTimeInSecond);

    String finishRecording();

    int getPreSleepSeconds();

}
