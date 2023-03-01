// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.command;

import lombok.Data;

/**
 * @author zhoule
 * @date 01/31/2023
 */
@Data
public class DeviceScriptCommand {
    private String type;
    private String device;
    private String when;
    /**
     * If the suite-class doesn't be provided, the value of suite-class would be the package name.
     */
    private String suiteClassMatcher;
    private String inline;
}
