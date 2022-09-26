// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner.controller;

import io.appium.java_client.windows.WindowsDriver;
import org.slf4j.Logger;

public class WindowsDriverController extends BaseDriverController {
    WindowsDriver windowsDriver;
    public WindowsDriverController(WindowsDriver windowsDriver, Logger logger){
        super(windowsDriver, logger);
        this.windowsDriver = windowsDriver;
    }
}
