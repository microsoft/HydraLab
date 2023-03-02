// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner.controller;

import io.appium.java_client.windows.WindowsDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.slf4j.Logger;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

public class WindowsDriverController extends BaseDriverController {
    WindowsDriver windowsDriver;

    public WindowsDriverController(WindowsDriver windowsDriver, Logger logger) {
        super(windowsDriver, logger);
        this.windowsDriver = windowsDriver;
    }

    @Override
    public void dragAndDropWithPosition(int fromX, int fromY, int toX, int toY) {
        // Using "Pen" on Windows Platform can run Drag and Drop well since Mouse is not supported in current appium version
        PointerInput finger = new PointerInput(PointerInput.Kind.PEN, "PEN");
        Sequence dragNDrop = new Sequence(finger, 1);
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(0),
                PointerInput.Origin.viewport(), fromX, fromY));
        dragNDrop.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), fromX, fromY));
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(700),
                PointerInput.Origin.viewport(), toX, toY));
        dragNDrop.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        windowsDriver.perform(Arrays.asList(dragNDrop));
    }

    @Override
    public void tap(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ofMillis(0),
                PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        windowsDriver.perform(Arrays.asList(tap));
    }

    @Override
    public void setClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }

    @Override
    public String getPageSource() {
        return windowsDriver.getPageSource();
    }

    @Override
    public void paste(WebElement webElement) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            String text = (String) clipboard.getData(DataFlavor.stringFlavor);
            input(webElement, text);
        } catch (UnsupportedFlavorException | IOException e) {
            throw new IllegalStateException("Could not get clipboard text on Windows", e);
        }
    }
}
