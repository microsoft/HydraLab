// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.agent;

import lombok.Data;

/**
 * @author zhoule
 * @date 01/31/2023
 */
@Data
public class DeviceCommand {
    private String type;
    private String device;
    private String when;
    private String matcher;
    private String inline;
}
