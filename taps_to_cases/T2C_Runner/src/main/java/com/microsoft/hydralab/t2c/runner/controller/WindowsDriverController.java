// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner.controller;

import io.appium.java_client.windows.WindowsDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Arrays;

public class WindowsDriverController extends BaseDriverController {
    WindowsDriver windowsDriver;
    public WindowsDriverController(WindowsDriver windowsDriver, Logger logger){
        super(windowsDriver, logger);
        this.windowsDriver = windowsDriver;
    }

    @Override
    public void dragAndDropWithPosition(int fromX, int fromY, int toX, int toY) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence dragNDrop = new Sequence(finger, 1);
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(0),
                PointerInput.Origin.viewport(), fromX, fromY));
        dragNDrop.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(),fromX,fromY));
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(700),
                PointerInput.Origin.viewport(), toX, toY));
        dragNDrop.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        windowsDriver.perform(Arrays.asList(dragNDrop));
    }
}
