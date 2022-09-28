package com.microsoft.hydralab.t2c.runner.controller;

import io.appium.java_client.windows.WindowsDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.slf4j.Logger;

public class EdgeDriverController extends BaseDriverController {
    EdgeDriver edgeDriver;
    WindowsDriver windowsDriver;

    public EdgeDriverController(WindowsDriver windowsDriver, EdgeDriver edgeDriver, Logger logger) {
        super(windowsDriver, logger);
        this.edgeDriver = edgeDriver;
        this.windowsDriver = windowsDriver;
    }

    @Override
    public void switchToUrl(String url) {
        edgeDriver.get(url);
    }
}
