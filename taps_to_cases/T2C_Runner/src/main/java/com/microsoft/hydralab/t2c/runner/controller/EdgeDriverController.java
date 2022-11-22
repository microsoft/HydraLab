package com.microsoft.hydralab.t2c.runner.controller;

import io.appium.java_client.windows.WindowsDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.slf4j.Logger;

import java.time.Duration;
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
    public void sendKeys(String content) {
        edgeDriver.getKeyboard().sendKeys(content);
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
    public void sleep(Duration duration) {
        edgeDriver.manage().timeouts().implicitlyWait(duration);
        windowsDriver.manage().timeouts().implicitlyWait(duration);
    }
}
