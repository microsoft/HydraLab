package com.microsoft.hydralab.t2c.runner.controller;

import io.appium.java_client.windows.WindowsDriver;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.slf4j.Logger;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

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
    public @Nullable WebElement findElementBy(Map<String, String> propertyMap) {
        WebElement elementFound = null;
        if (propertyMap.get("accessibilityId") != null && propertyMap.get("accessibilityId").length() != 0) {
            elementFound = findElementByAccessibilityId(propertyMap.get("accessibilityId"));
            return elementFound;
        }
        if (propertyMap.get("text") != null && propertyMap.get("text").length() != 0) {
            elementFound = findElementByName(propertyMap.get("text"));
            return elementFound;
        }
        if (propertyMap.get("xpath") != null && propertyMap.get("xpath").length() != 0) {
            elementFound = findElementByXPath(propertyMap.get("xpath"));
            return elementFound;
        }
        return null;
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

}
