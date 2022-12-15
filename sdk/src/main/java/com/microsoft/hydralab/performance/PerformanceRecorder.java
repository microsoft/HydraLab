// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.performance;

import com.microsoft.hydralab.appium.AppiumParam;

/**
 * @author zhoule
 * @date 12/14/2022
 */

public interface PerformanceRecorder {
    void beforeTest();

    void addRecord();

    void afterTest();
}
