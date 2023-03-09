// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.entity.performance;

import java.io.File;
import java.io.Serializable;

public class PerformanceInspection implements Serializable {

    public PerformanceInspector.PerformanceInspectorType inspectorType;
    public String appId;
    public String deviceIdentifier = "";
    public String description;
    public String inspectionKey = "";
    public boolean isReset = false;
    public File resultFolder;
}
