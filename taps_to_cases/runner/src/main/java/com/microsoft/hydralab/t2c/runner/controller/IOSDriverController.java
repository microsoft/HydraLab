// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner.controller;

import com.google.common.collect.ImmutableMap;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspectionService;
import com.microsoft.hydralab.t2c.runner.T2CAppiumUtils;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;

public class IOSDriverController extends BaseDriverController {
    private final IOSDriver iosDriver;
    private String clipboardString;

    public IOSDriverController(IOSDriver iosDriver, String udid, Logger logger) {
        super(iosDriver, udid, logger);
        this.iosDriver = iosDriver;
    }

    @Override
    public void activateApp(String appPackageName) {
        if (iosDriver.isAppInstalled(appPackageName)) {
            if (iosDriver.queryAppState(appPackageName) != ApplicationState.RUNNING_IN_FOREGROUND) {
                iosDriver.activateApp(appPackageName);
            }
        } else {
            throw new RuntimeException("the app " + appPackageName + " is not installed");
        }
    }

    @Override
    public void terminateApp(String appPackageName) {
        if (iosDriver.queryAppState(appPackageName) != ApplicationState.NOT_RUNNING &&
                iosDriver.queryAppState(appPackageName) != ApplicationState.NOT_INSTALLED) {
            iosDriver.terminateApp(appPackageName);
        }
    }

    @Override
    public void scroll(WebElement webElement, int xVector, int yVector) {
        Point location = webElement.getLocation();
        Dimension dimension = webElement.getSize();
        int x = location.getX();
        int y = location.getY();
        int width = dimension.getWidth();
        int height = dimension.getHeight();
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence dragNDrop = new Sequence(finger, 1);
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(0),
                PointerInput.Origin.viewport(), centerX, centerY));
        dragNDrop.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(700),
                PointerInput.Origin.viewport(), centerX + xVector, centerY + yVector));
        dragNDrop.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        iosDriver.perform(List.of(dragNDrop));

        logger.info("centerX" + centerX + "centerY" + centerY);
    }

    @Override
    public void swipe(String direction) {
        T2CAppiumUtils.safeSleep(1000);
        Dimension dimension = iosDriver.manage().window().getSize();
        int width = dimension.getWidth();
        int height = dimension.getHeight();
        ((JavascriptExecutor) iosDriver).executeScript("mobile: swipe", ImmutableMap.of(
                "left", width * 0.1, "top", height * 0.1, "width", width * 0.9, "height", height * 0.9,
                "direction", direction,
                "percent", 0.7

        ));
    }

    @Override
    public void tap(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ofMillis(0),
                PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        iosDriver.perform(List.of(tap));
    }

    @Override
    public void longClick(Integer duration, WebElement element) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence longClickActions = new Sequence(finger, 1);
        longClickActions.addAction(finger.createPointerMove(Duration.ofMillis(0),
                PointerInput.Origin.viewport(), element.getLocation().x, element.getLocation().y));
        longClickActions.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        longClickActions.addAction(finger.createPointerMove(Duration.ofMillis(duration),
                PointerInput.Origin.viewport(), element.getLocation().x, element.getLocation().y));
        longClickActions.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        iosDriver.perform(List.of(longClickActions));
    }

    @Override
    public void dragAndDropWithPosition(int fromX, int fromY, int toX, int toY) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence dragNDrop = new Sequence(finger, 1);
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(0),
                PointerInput.Origin.viewport(), fromX, fromY));
        dragNDrop.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), fromX, fromY));
        dragNDrop.addAction(finger.createPointerMove(Duration.ofMillis(700),
                PointerInput.Origin.viewport(), toX, toY));
        dragNDrop.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        iosDriver.perform(List.of(dragNDrop));
    }

    // Todo: setClipboard() of Appium doesn't works for ios driver
    @Override
    public void setClipboard(String text) {
        clipboardString = text;
    }

    @Override
    public String getPageSource() {
        return iosDriver.getPageSource();
    }

    @Override
    public void inspectMemoryUsage(String targetApp, String description, boolean isReset) {
        // TODO: Need to add memory stack profiling inspector here
        PerformanceInspectionService.getInstance()
                .inspect(PerformanceInspection.createIOSMemoryInspection(
                        targetApp, this.udid, description, isReset));
    }

    @Override
    public void inspectBatteryUsage(String targetApp, String description, boolean isReset) {
        PerformanceInspectionService.getInstance()
                .inspect(PerformanceInspection.createIOSEnergyInspection(
                        targetApp, this.udid, description, isReset));
    }

    @Override
    public void paste(WebElement webElement) {
        input(webElement, clipboardString);
    }

    @Override
    public void backToHome() {
        iosDriver.runAppInBackground(Duration.ofSeconds(-1));
    }
}
