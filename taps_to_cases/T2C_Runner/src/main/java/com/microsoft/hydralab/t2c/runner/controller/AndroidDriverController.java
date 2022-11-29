// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner.controller;

import com.google.common.collect.ImmutableMap;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.appmanagement.ApplicationState;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AndroidDriverController extends BaseDriverController {
    private final AndroidDriver androidDriver;

    public AndroidDriverController(AndroidDriver androidDriver, Logger logger) {
        super(androidDriver, logger);
        this.androidDriver = androidDriver;
    }

    @Override
    public void activateApp(String appPackageName) {
        if (androidDriver.isAppInstalled(appPackageName)) {
            if (androidDriver.queryAppState(appPackageName) != ApplicationState.RUNNING_IN_FOREGROUND) {
                androidDriver.activateApp(appPackageName);
            }
        } else {
            throw new RuntimeException("the app is not installed");
        }
    }

    @Override
    public void terminateApp(String appPackageName) {
        if (androidDriver.queryAppState(appPackageName) != ApplicationState.NOT_RUNNING &&
                androidDriver.queryAppState(appPackageName) != ApplicationState.NOT_INSTALLED) {
            androidDriver.terminateApp(appPackageName);
        }
    }

    @Override
    public void pressKey(AndroidKey key) {
        androidDriver.pressKey(new KeyEvent(key));
    }

    @Override
    public void pressKeyCode(String keyCode) {
        List<String> keyEventArgs = Arrays.asList("keyevent", keyCode);
        Map<String, Object> keyEventCmd = ImmutableMap.of("command", "input", "args", keyEventArgs);
        androidDriver.executeScript("mobile: shell", keyEventCmd);
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
        androidDriver.perform(Arrays.asList(dragNDrop));

        logger.info("centerX" + centerX + "centerY" + centerY);
    }

    @Override
    public void swipe(String direction) {
        //androidDriver.executeScript("mobile: scroll", ImmutableMap.of("direction", direction));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Dimension dimension = androidDriver.manage().window().getSize();
        int width = dimension.getWidth();
        int height = dimension.getHeight();
        ((JavascriptExecutor) androidDriver).executeScript("mobile: swipeGesture", ImmutableMap.of(
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
        androidDriver.perform(Arrays.asList(tap));
    }

    @Override
    public void longClick(Integer duration, WebElement element) {
        ((JavascriptExecutor) androidDriver).executeScript("mobile: longClickGesture", ImmutableMap.of(
                "elementId", ((RemoteWebElement) element).getId(), "duration", duration
        ));
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
        androidDriver.perform(Arrays.asList(dragNDrop));
    }

    @Override
    public WebElement findElementByName(String name) {
        WebElement elementFound = null;
        try {
            elementFound = new WebDriverWait(webDriver, Duration.ofSeconds(10))
                    .until(driver -> driver.findElement(AppiumBy.xpath("//*[@text='" + name + "']")));
        } catch (Exception e) {
            logger.info("Can not find element by Name: " + name);
        }
        return elementFound;
    }
}
