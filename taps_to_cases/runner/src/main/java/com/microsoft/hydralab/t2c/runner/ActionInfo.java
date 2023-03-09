// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import com.microsoft.hydralab.t2c.runner.elements.BaseElementInfo;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ActionInfo {
    static final String ACTION_TYPE_INSPECT_MEM_USAGE = "inspectMemoryUsage";
    static final String ACTION_TYPE_INSPECT_BATTERY_USAGE = "inspectBatteryUsage";
    private final Integer id;
    private final BaseElementInfo testElement;
    private final String actionType;
    private final String driverId;

    private final String description;
    private final Map<String, Object> arguments;
    private final boolean isOptional;

    public ActionInfo(int id,
                      @Nullable BaseElementInfo testElement,
                      @NotNull String actionType,
                      @NotNull Map<String, Object> arguments,
                      @NotNull String driverId,
                      @NotNull String description,
                      boolean isOptional) {
        this.id = id;
        this.testElement = testElement;
        this.actionType = actionType;
        this.driverId = driverId;
        this.isOptional = isOptional;
        this.description = description;
        this.arguments = arguments;
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
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
        // Labeled all the performance related action
        return isOptional
                || ACTION_TYPE_INSPECT_MEM_USAGE.equalsIgnoreCase(actionType)
                || ACTION_TYPE_INSPECT_BATTERY_USAGE.equalsIgnoreCase(actionType);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}

