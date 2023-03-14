package com.microsoft.hydralab.t2c.runner.controller;

import io.appium.java_client.windows.WindowsDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
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

public class EdgeDriverController extends BaseDriverController {
    EdgeDriver edgeDriver;
    WindowsDriver windowsDriver;

    public EdgeDriverController(WindowsDriver windowsDriver, EdgeDriver edgeDriver, String udid, Logger logger) {
        super(windowsDriver, udid, logger);
        this.edgeDriver = edgeDriver;
        this.windowsDriver = windowsDriver;
    }

    @Override
    public void switchToUrl(String url) {
        edgeDriver.get(url);
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
        return "Windows page: \n" + windowsDriver.getPageSource() + "\n Edge source: " + edgeDriver.getPageSource();
    }

    @Override
    public void paste(WebElement webElement) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            String text = (String) clipboard.getData(DataFlavor.stringFlavor);
            input(webElement, text);
        } catch (UnsupportedFlavorException | IOException e) {
            throw new IllegalStateException("Could not get clipboard text on Edge", e);
        }
    }

    @Override
    public void inspectMemoryUsage(String targetApp, String description, boolean isReset) {
        // Nothing need to do for Edge Driver
    }

    @Override
    public void inspectBatteryUsage(String targetApp, String description, boolean isReset) {
        // Nothing need to do for Edge Driver
    }
}
