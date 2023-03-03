// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.Entity;

import com.microsoft.hydralab.performance.hprof.ObjectInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AndroidHprofMemoryInfo implements Serializable {
    private List<ObjectInfo> bitmapInfoList;
    private List<ObjectInfo> topObjectList;
    private String appPackageName;
    private long timeStamp;
    private String description;
}
