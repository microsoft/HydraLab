// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management.device;

import lombok.Getter;
import lombok.Setter;

/**
 * @author zhoule
 * @date 03/01/2023
 */
@Getter
@Setter
public class DeviceManagerProperty {
    String type;
    boolean enabled;
    boolean status;
}
