// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.elements.BaseElementInfo;
import com.microsoft.hydralab.t2c.runner.finder.ElementFinder;
import com.microsoft.hydralab.t2c.runner.finder.ElementFinderFactory;
import io.appium.java_client.android.nativekey.AndroidKey;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockDriverT2CTest {
    public T2CJsonParser t2CJsonParser;
    private TestInfo testInfo;
    private Logger logger;
    String filePath = "src/test/resources/DemoJson.json";


    private final Map<String, BaseDriverController> driverControllerMap = new HashMap<>();

    @BeforeEach
    public void setUp() {
        logger = LoggerFactory.getLogger(MockDriverT2CTest.class);
        t2CJsonParser = new T2CJsonParser(logger);
        testInfo = t2CJsonParser.parseJsonFile(filePath);
        T2CAppiumUtils.setSelfTesting(true);
        getDriversMap(testInfo.getDrivers());
        ElementFinderFactory.registerFinder(MockDriverController.class, MockElementFinder.class);

    }

    public void getDriversMap(ArrayList<DriverInfo> drivers) {
        for (DriverInfo driverInfo : drivers) {
            driverControllerMap.put(driverInfo.getId(), new MockDriverController(null, "10086", logger));
        }
    }

    @Test
    public void jsonTest() {
        ArrayList<ActionInfo> caseList = testInfo.getActions();

        for (ActionInfo actionInfo : caseList) {
            BaseDriverController driverController = driverControllerMap.get(actionInfo.getDriverId());
            T2CAppiumUtils.doAction(driverController, actionInfo, logger);
        }
    }

    public static class MockElementFinder implements ElementFinder<BaseElementInfo> {

        private final BaseDriverController driverController;

        public MockElementFinder(BaseDriverController driverController) {
            this.driverController = driverController;
        }

        @Override
        public WebElement findElement(BaseElementInfo elementInfo) {
            driverController.findElementByXPath(elementInfo.getXpath());
            return new MockElement();
        }
    }

    static class MockDriverController extends BaseDriverController {
        private String currentMethodName() {
            return Thread.currentThread().getStackTrace()[2].getMethodName();
        }

        public MockDriverController(WebDriver webDriver, String udid, Logger logger) {
            super(webDriver, udid, logger);
        }

        public void click(WebElement element) {
            logger.info("Called " + currentMethodName());
        }

        public void tap(int x, int y) {
            logger.info("Called " + currentMethodName());
        }

        public void input(WebElement element, String content) {
            logger.info("Called " + currentMethodName());
        }

        public void sendKeys(String content) {
            logger.info("Called " + currentMethodName());
        }

        public void clear(WebElement element) {
            logger.info("Called " + currentMethodName());
        }

        public void activateApp(String appPackageName) {
            logger.info("Called " + currentMethodName());
        }

        public void terminateApp(String appPackageName) {
            logger.info("Called " + currentMethodName());
        }

        public void pressKey(AndroidKey key) {
            logger.info("Called " + currentMethodName());
        }

        public void pressKeyCode(String keyCode) {
            logger.info("Called " + currentMethodName());
        }

        public void scroll(WebElement webElement, int xVector, int yVector) {
            logger.info("Called " + currentMethodName());
        }

        public void swipe(String direction) {
            logger.info("Called " + currentMethodName());
        }

        public void longClick(Integer duration, WebElement element) {
            logger.info("Called " + currentMethodName());
        }

        public void dragAndDrop(WebElement element, int xVec, int yVec) {
            logger.info("Called " + currentMethodName());
        }

        public void dragAndDrop(WebElement fromElement, WebElement toElement) {
            logger.info("Called " + currentMethodName());
        }

        public void dragAndDropWithPosition(int fromX, int fromY, int toX, int toY) {
            logger.info("Called " + currentMethodName());
        }

        public void navigateTo(String url) {
            logger.info("Called " + currentMethodName());
        }

        //Only the following attributes are supported: [checkable, checked, {class,className}, clickable,
        //{content-desc,contentDescription}, enabled, focusable, focused, {long-clickable,longClickable},
        //package, password, {resource-id,resourceId}, scrollable, selection-start, selection-end, selected,
        //{text,name}, bounds, displayed, contentSize]
        public void assertElementAttribute(WebElement webElement, String attribute, String expectedValue) {
            logger.info("Called " + currentMethodName());
        }

        public String getInfo(WebElement webElement, String attribute) {
            logger.info("Called " + currentMethodName());
            return "TestInfo";
        }

        public void switchToUrl(String url) {
            logger.info("Called " + currentMethodName());
        }

        public WebElement findElementByAccessibilityId(String accessibilityId) {
            logger.info("Called " + currentMethodName());
            return new MockElement();
        }

        public WebElement findElementByXPath(String xpath) {
            logger.info("Called " + currentMethodName());
            return new MockElement();
        }

        public WebElement findElementByText(String text) {
            logger.info("Called " + currentMethodName());
            return new MockElement();
        }

        public WebElement findElementByLabel(String text) {
            logger.info("Called " + currentMethodName());
            return new MockElement();
        }

        @Override
        public @Nullable WebElement findElementById(String id) {
            logger.info("Called " + currentMethodName());
            return new MockElement();
        }

        @Override
        public String getPageSource() {
            logger.info("Called " + currentMethodName());
            return "foo";
        }

        @Override
        public void inspectMemoryUsage(String targetApp, String description, boolean isReset) {
            logger.info("Called " + currentMethodName());
        }

        @Override
        public void inspectBatteryUsage(String targetApp, String description, boolean isReset) {
            logger.info("Called " + currentMethodName());
        }


        public void copy(WebElement webElement) {
            logger.info("Called " + currentMethodName());
        }

        public void paste(WebElement webElement) {
            logger.info("Called " + currentMethodName());
        }

        public void setClipboard(String text) {
            logger.info("Called " + currentMethodName());
        }
    }

    static class MockElement implements WebElement {
        @Override
        public void click() {

        }

        @Override
        public void submit() {

        }

        @Override
        public void sendKeys(CharSequence... keysToSend) {

        }

        @Override
        public void clear() {

        }

        @Override
        public String getTagName() {
            return null;
        }

        @Override
        public String getAttribute(String name) {
            return null;
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public String getText() {
            return "MockedName";
        }

        @Override
        public List<WebElement> findElements(By by) {
            return null;
        }

        @Override
        public WebElement findElement(By by) {
            return null;
        }

        @Override
        public boolean isDisplayed() {
            return false;
        }

        @Override
        public Point getLocation() {
            return new Point(0, 0);
        }

        @Override
        public Dimension getSize() {
            return new Dimension(10, 10);
        }

        @Override
        public Rectangle getRect() {
            return new Rectangle(getLocation(), getSize());
        }

        @Override
        public String getCssValue(String propertyName) {
            return "Value";
        }

        @Override
        public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
            return null;
        }
    };
}
