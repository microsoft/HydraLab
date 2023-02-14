// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.config;

import com.microsoft.hydralab.entity.DeviceAction;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.hydralab.utils.CommonUtils.GSON;

/**
 * @author Li Shen
 * @date 2/8/2023
 */

public class DeviceConfig {
    public String deviceIdentifier = "";
    public String groupTestType = "SINGLE";
    public Map<String, List<DeviceAction>> deviceActions = new HashMap<>();
    public String deviceActionsStr = "";

    public void extractFromExistingField(){
        if (StringUtils.isBlank(this.deviceActionsStr) && deviceActions.size() != 0) {
            this.deviceActionsStr = GSON.toJson(this.deviceActions);
        }
    }

    @Override
    public String toString() {
        return "DeviceConfig:\n" +
                "\tdeviceIdentifier=" + deviceIdentifier + "\n" +
                "\tgroupTestType=" + groupTestType + "\n" +
                "\tdeviceActionsStr=" + deviceActionsStr;
    }
}