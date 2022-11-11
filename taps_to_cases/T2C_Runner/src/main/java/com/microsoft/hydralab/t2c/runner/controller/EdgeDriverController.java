package com.microsoft.hydralab.t2c.runner.controller;

import io.appium.java_client.windows.WindowsDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.slf4j.Logger;

import java.util.Arrays;

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

    @Override
    public void tap(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.PEN, "PEN");
        Sequence click = new Sequence(finger, 1);
        click.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg());
        windowsDriver.perform(Arrays.asList(click));
    }
}
