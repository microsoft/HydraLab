// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.entity;

import com.microsoft.hydralab.entity.performance.InspectionStrategy;

import java.util.List;
import java.util.Map;

/**
 * @author Li Shen
 * @date 1/24/2024
 */

public class AnalysisConfig {
    public String analysisType;
    public String executor;
    public Map<String, String> analysisConfig;
}
