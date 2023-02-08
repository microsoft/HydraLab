// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.config;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Li Shen
 * @date 2/8/2023
 */

public class DeviceConfig {
    public String deviceIdentifier = "";
    public String groupTestType = "SINGLE";
    public List<String> neededPermissions = new ArrayList<>();
    public String deviceActionsStr = "";

    @Override
    public String toString() {
        return "DeviceConfig:\n" +
                "\tdeviceIdentifier=" + deviceIdentifier + "\n" +
                "\tgroupTestType=" + groupTestType + "\n" +
                "\tneededPermissions=" + (neededPermissions != null ? neededPermissions.toString() : "") + "\n" +
                "\tdeviceActionsStr=" + deviceActionsStr;
    }
}