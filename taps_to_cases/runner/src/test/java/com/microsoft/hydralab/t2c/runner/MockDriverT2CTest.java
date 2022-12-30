// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import io.appium.java_client.android.nativekey.AndroidKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
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

    }

    public void getDriversMap(ArrayList<DriverInfo> drivers) {
        for (DriverInfo driverInfo : drivers) {
            driverControllerMap.put(driverInfo.getId(), new MockDriverController(null, logger));
        }
    }

    @Test
    public void jsonTest() {
        ArrayList<ActionInfo> caseList = testInfo.getCases();

        for (ActionInfo actionInfo : caseList) {
            BaseDriverController driverController = driverControllerMap.get(actionInfo.getDriverId());
            T2CAppiumUtils.doAction(driverController, actionInfo, logger);
        }
    }

    static class MockDriverController extends BaseDriverController {
        private String currentMethodName() {
            return Thread.currentThread().getStackTrace()[2].getMethodName();
        }

        public MockDriverController(WebDriver webDriver, Logger logger) {
            super(webDriver, logger);
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

        public WebElement findElementByName(String name) {
            logger.info("Called " + currentMethodName());
            return new MockElement();
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
