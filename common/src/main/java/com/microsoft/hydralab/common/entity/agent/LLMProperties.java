// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.agent;

import lombok.Data;

/**
 * @author Li Shen
 * @date 7/18/2023
 */

@Data
public class LLMProperties {
    private String enabled;
    private String deploymentName;
    private String openaiApiKey;
    private String openaiApiBase;
    private String openaiApiVersion;
}
