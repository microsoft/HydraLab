// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import com.alibaba.fastjson.JSON;
import com.microsoft.hydralab.t2c.runner.controller.AndroidDriverController;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.controller.EdgeDriverController;
import com.microsoft.hydralab.t2c.runner.controller.WindowsDriverController;
import com.microsoft.hydralab.t2c.runner.elements.AndroidElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.BaseElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.EdgeElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.WindowsElementInfo;
import com.microsoft.hydralab.t2c.runner.finder.ElementFinder;
import com.microsoft.hydralab.t2c.runner.finder.ElementFinderFactory;
import io.appium.java_client.android.nativekey.AndroidKey;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public final class T2CAppiumUtils {
    static HashMap<String, String> keyToInfoMap = new HashMap<>();
    private static boolean isSelfTesting = false;

    private T2CAppiumUtils() {

    }

    public static WebElement findElement(BaseDriverController driver, BaseElementInfo element, Logger logger) {
        WebElement elementFound = null;
        if (element == null) {
            return null;
        }
        ElementFinder<BaseElementInfo> finder = ElementFinderFactory.createElementFinder(driver);
        elementFound = finder.findElement(element);
        if (elementFound != null) {
            return elementFound;
        }
        logger.warn("Page source: " + driver.getPageSource());
        throw new IllegalArgumentException("Element can not be found in current UI. Element info is " + element.getElementInfo());
    }

    public static void doAction(@NotNull BaseDriverController driver, @NotNull ActionInfo actionInfo, @NotNull Logger logger) {
        boolean isOption = actionInfo.isOptional();
        try {
            chooseActionType(driver, actionInfo, logger);
        } catch (Exception e) {
            e.printStackTrace();
            int index = actionInfo.getId();
            String description = actionInfo.getDescription();
            logger.error("doAction at " + index + ", description: " + description + ", page source: " + prettyPrintByTransformer(driver.getPageSource(), 2, false)
                    + "\n, with exception: " + e.getMessage());
            if (!isOption) {
                throw new IllegalStateException("Failed at " + index + ", description: " + description + ", " + e.getMessage()
                        + ", page source: \n" + prettyPrintByTransformer(driver.getPageSource(), 2, false), e);
            }
        }
    }

    private static String prettyPrintByTransformer(String xmlString, int indent, boolean ignoreDeclaration) {

        try {
            InputSource src = new InputSource(new StringReader(xmlString));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            return xmlString;
        }
    }

    @SuppressWarnings("methodlength")
    public static void chooseActionType(BaseDriverController driver, ActionInfo actionInfo, Logger logger) {
        String actionType = actionInfo.getActionType();
        BaseElementInfo element = actionInfo.getTestElement();
        WebElement webElement = findElement(driver, element, logger);
        Map<String, Object> arguments = actionInfo.getArguments();
        // Safe wait if no element required before this action to ensure the UI is ready
        if (webElement == null && !isSelfTesting) {
            safeSleep(3000);
        }
        logger.info("chooseActionType, action id: " + actionInfo.getId() + ", description: " + actionInfo.getDescription() + " on element: "  + webElement);
        logger.info("chooseActionType, page source: \n" + prettyPrintByTransformer(driver.getPageSource(), 2, false));
        switch (actionType) {
            case "click":
                driver.click(webElement);
                break;
            case "tap":
                int x = (Integer) arguments.get("x");
                int y = (Integer) arguments.get("y");
                driver.tap(x, y);
                break;
            case "input":
                String content;
                if (arguments.containsKey("id")) {
                    String id = (String) arguments.get("id");
                    content = keyToInfoMap.get(id);
                } else {
                    content = (String) arguments.get("content");
                }
                if (content == null) {
                    throw new IllegalArgumentException("Trying to input a null String. action index: " + actionInfo.getId());
                }
                if (webElement == null) {
                    driver.sendKeys(content);
                } else {
                    driver.input(webElement, content);
                }
                break;
            case "clear":
                driver.clear(webElement);
                break;
            case "activateApp":
                String appPackageName = (String) arguments.get("appPackageName");
                if (appPackageName == null) {
                    throw new IllegalArgumentException(
                            "App package name should not be null. Please add argument 'appPackageName' in the json. action index: " + actionInfo.getId());
                }
                driver.activateApp(appPackageName);
                break;
            case "terminateApp":
                String removeAppPackageName = (String) arguments.get("appPackageName");
                if (removeAppPackageName == null) {
                    throw new IllegalArgumentException(
                            "App package name should not be null. Please add argument 'appPackageName' in the json. action index: " + actionInfo.getId());
                }
                driver.terminateApp(removeAppPackageName);
                break;
            case "back":
                driver.pressKey(AndroidKey.BACK);
                break;
            case "home":
                driver.backToHome();
                break;
            case "pressKeyCode":
                String keyCode = arguments.get("keyCode") + "";
                driver.pressKeyCode(keyCode);
                break;
            case "move":
                Object xVector = arguments.get("xVector");
                Object yVector = arguments.get("yVector");
                if (xVector == null || yVector == null) {
                    throw new IllegalArgumentException("Destination is not defined. Please add argument 'xVector' and 'yVector' in the json. action index: " + actionInfo.getId());
                }
                int xVectorInt = xVector instanceof Integer ? (Integer) xVector : Integer.getInteger((String) xVector);
                int yVectorInt = yVector instanceof Integer ? (Integer) yVector : Integer.getInteger((String) yVector);
                driver.scroll(webElement, xVectorInt, yVectorInt);
                break;
            case "swipe":
                String direction = (String) arguments.get("direction");
                if (direction == null) {
                    throw new IllegalArgumentException("Direction is not defined. Please add argument 'direction' in the json. action index: " + actionInfo.getId());
                }
                driver.swipe(direction);
                break;
            case "longClick":
                Object durationObj = arguments.get("duration");
                if (durationObj == null) {
                    throw new IllegalArgumentException("Duration is not defined. Please add argument 'duration' in the json. action index: " + actionInfo.getId());
                }
                int duration = durationObj instanceof Integer ? (Integer) durationObj : Integer.getInteger((String) durationObj);
                driver.longClick(duration, webElement);
                break;
            case "assert":
                String attribute = (String) arguments.get("attribute");
                String expectedValue = (String) arguments.get("expectedValue");
                if (attribute == null || expectedValue == null) {
                    throw new IllegalArgumentException(
                            "Assert info is not defined. Please add argument 'attribute' and 'expectedValue' in the json. action index: " + actionInfo.getId());
                }
                driver.assertElementAttribute(webElement, attribute, expectedValue);
                break;
            case "sleep":
                Object timeoutObj = arguments.get("duration");
                long timeout = timeoutObj instanceof Integer ? (Integer) timeoutObj : Long.parseLong((String) arguments.get("duration"));
                safeSleep(timeout);
                break;
            case "getInfo":
                String attributeKey = (String) arguments.get("attribute");
                String id = (String) arguments.get("id");
                if (attributeKey == null || id == null) {
                    throw new IllegalArgumentException("Assert info is not defined. Please add argument 'attribute' and 'id' in the json. action index: " + actionInfo.getId());
                }
                String info = driver.getInfo(webElement, attributeKey);
                keyToInfoMap.put(id, info);
                break;
            case "dragAndDrop":
                Object xVectorDnd = arguments.get("xVector");
                Object yVectorDnd = arguments.get("yVector");
                String toElementStr = (String) arguments.get("toElement");
                if (xVectorDnd != null && yVectorDnd != null) {
                    int xVectorIntDnd = xVectorDnd instanceof Integer ? (Integer) xVectorDnd : Integer.getInteger((String) xVectorDnd);
                    int yVectorIntDnd = yVectorDnd instanceof Integer ? (Integer) yVectorDnd : Integer.getInteger((String) yVectorDnd);
                    driver.dragAndDrop(webElement, xVectorIntDnd, yVectorIntDnd);
                } else if (toElementStr != null) {
                    BaseElementInfo toElementInfo;
                    if (driver instanceof AndroidDriverController) {
                        toElementInfo = JSON.parseObject(toElementStr, AndroidElementInfo.class);
                    } else if (driver instanceof WindowsDriverController) {
                        toElementInfo = JSON.parseObject(toElementStr, WindowsElementInfo.class);
                    } else if (driver instanceof EdgeDriverController) {
                        toElementInfo = JSON.parseObject(toElementStr, EdgeElementInfo.class);
                    } else {
                        throw new IllegalArgumentException("Fail to parse the 'toElement' in the json. action index: " + actionInfo.getId());
                    }
                    WebElement toElement = findElement(driver, toElementInfo, logger);
                    driver.dragAndDrop(webElement, toElement);
                } else {
                    throw new IllegalArgumentException(
                            "Destination is not defined. Please add argument 'xVector' & 'yVector' or 'toElement' in the json. action index: " + actionInfo.getId());
                }
                break;
            case "switchToUrl":
                String url = (String) arguments.get("url");
                driver.switchToUrl(url);
                if (url == null) {
                    throw new IllegalArgumentException("Url is not defined. Please add argument 'url' and 'id' in the json. action index: " + actionInfo.getId());
                }
                break;
            case "copy":
                assert webElement != null;
                driver.copy(webElement);
                break;
            case "paste":
                driver.paste(webElement);
                break;
            case "setClipboard":
                String clipboardText = (String) arguments.get("text");
                driver.setClipboard(clipboardText);
                break;
            case ActionInfo.ACTION_TYPE_INSPECT_BATTERY_USAGE:
                String targetApp = (String) arguments.get("targetApp");
                String description = (String) arguments.get("description");
                boolean isReset = (Boolean) arguments.getOrDefault("isReset", false);
                driver.inspectBatteryUsage(targetApp, description, isReset);
                break;
            case ActionInfo.ACTION_TYPE_INSPECT_MEM_USAGE:
                targetApp = (String) arguments.get("targetApp");
                description = (String) arguments.get("description");
                isReset = (Boolean) arguments.getOrDefault("isReset", false);
                driver.inspectMemoryUsage(targetApp, description, isReset);
                break;
            default:
                throw new IllegalStateException("action fail" +
                        "" +
                        "" +
                        "ed. action index:" + actionInfo.getId() + "/t" + "actionType:" + actionInfo.getActionType());

        }
        // Safe wait if no element required after doing this action to ensure the action is finished
        if (webElement == null && !isSelfTesting) {
            safeSleep(3000);
        }
    }

    public static void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void setSelfTesting(boolean isTesting) {
        isSelfTesting = isTesting;
    }
}
