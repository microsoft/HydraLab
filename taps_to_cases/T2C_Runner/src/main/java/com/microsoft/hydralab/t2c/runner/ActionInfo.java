// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner;

import com.microsoft.hydralab.t2c.runner.elements.BaseElementInfo;

import javax.annotation.Nullable;
import java.util.Map;
public class ActionInfo {
    private Integer id;
    private BaseElementInfo testElement;

    private String actionType;

    private String driverId;
    private Map<String, Object> arguments;
    private boolean isOption;


    public ActionInfo(Integer id, @Nullable BaseElementInfo testElement, String actionType, Map<String, Object> arguments,
                      String driverId, boolean isOption) {
        this.id = id;
        this.testElement = testElement;
        this.actionType = actionType;
        this.driverId = driverId;
        this.isOption = isOption;
        if (arguments != null) {
            this.arguments = arguments;
        }
    }

    public Integer getId() {
        return id;
    }

    public BaseElementInfo getTestElement() {
        return testElement;
    }

    public String getActionType() {
        return actionType;
    }

    public String getDriverId() {
        return driverId;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public boolean isOption() {
        return isOption;
    }
}

