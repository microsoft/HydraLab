// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner;

import com.microsoft.hydralab.t2c.runner.elements.BaseElementInfo;
import java.util.Map;
import javax.annotation.Nullable;

public class ActionInfo {
    private final Integer id;
    private final BaseElementInfo testElement;
    private final String actionType;
    private final String driverId;
    private Map<String, Object> arguments;
    private final boolean isOptional;

    public ActionInfo(Integer id, @Nullable BaseElementInfo testElement, String actionType, Map<String, Object> arguments,
                      String driverId, boolean isOptional) {
        this.id = id;
        this.testElement = testElement;
        this.actionType = actionType;
        this.driverId = driverId;
        this.isOptional = isOptional;
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

    public boolean isOptional() {
        return isOptional;
    }
}

