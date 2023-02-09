// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.entity;

import java.util.ArrayList;
import java.util.List;

public class DeviceAction {
    public String deviceType;
    public String method;
    public List<String> args = new ArrayList<>();
}