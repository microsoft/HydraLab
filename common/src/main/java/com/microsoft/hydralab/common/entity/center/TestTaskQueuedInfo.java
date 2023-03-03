// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.center;

import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import lombok.Data;

@Data
public class TestTaskQueuedInfo {
    private TestTaskSpec testTaskSpec;
    private int[] queuedInfo = new int[2];
}
