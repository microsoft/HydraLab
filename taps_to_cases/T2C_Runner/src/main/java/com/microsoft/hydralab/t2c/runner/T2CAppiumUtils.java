// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.t2c.runner.controller.AndroidDriverController;
import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.controller.EdgeDriverController;
import com.microsoft.hydralab.t2c.runner.controller.WindowsDriverController;
import com.microsoft.hydralab.t2c.runner.elements.AndroidElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.BaseElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.EdgeElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.WindowsElementInfo;
import io.appium.java_client.android.nativekey.AndroidKey;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

public class T2CAppiumUtils {
    static HashMap<String, String> keyToInfoMap = new HashMap<>();

    public static WebElement findElement(BaseDriverController driver, BaseElementInfo element) {
        WebElement elementFinded = null;
        if (element == null) return null;
        Map<String, String> keyToVal = element.getBasisSearchedBy();
        if (keyToVal.get("accessibilityId") != null && keyToVal.get("accessibilityId").length() != 0) {
            elementFinded = driver.findElementByAccessibilityId(keyToVal.get("accessibilityId"));
            if (elementFinded != null) return elementFinded;
        }
        if (keyToVal.get("text") != null && keyToVal.get("text").length() != 0) {
            elementFinded = driver.findElementByName(keyToVal.get("text"));
            if (elementFinded != null) return elementFinded;
        }
        if (keyToVal.get("xpath") != null && keyToVal.get("xpath").length() != 0) {
            elementFinded = driver.findElementByXPath(keyToVal.get("xpath"));
            if (elementFinded != null) return elementFinded;
        }
        throw new IllegalArgumentException("Element can not be found in current UI. Element info is " + element.getElementInfo());
    }

    public static void doAction(BaseDriverController driver, ActionInfo actionInfo) {
        boolean isOption = actionInfo.isOption();
        try {
            chooseActionType(driver, actionInfo);
        } catch (Exception e) {
            e.printStackTrace();
            if (!isOption) {
                throw e;
            }
        }
    }

    public static void chooseActionType(BaseDriverController driver, ActionInfo actionInfo) {
        String ActionType = actionInfo.getActionType();
        BaseElementInfo element = actionInfo.getTestElement();
        WebElement webElement = findElement(driver, element);
        Map<String, Object> arguments = actionInfo.getArguments();

        switch (ActionType) {
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
                    throw new IllegalArgumentException("Trying to input a null String. actionId: " + actionInfo.getId());
                }
                driver.input(webElement, content);
                break;
            case "clear":
                driver.clear(webElement);
                break;
            case "activateApp":
                String appPackageName = (String) arguments.get("appPackageName");
                if (appPackageName == null) {
                    throw new IllegalArgumentException("App package name should not be null. Please add argument 'appPackageName' in the json. actionId: " + actionInfo.getId());
                }
                driver.activateApp(appPackageName);
                break;
            case "terminateApp":
                String removeAppPackageName = (String) arguments.get("appPackageName");
                if (removeAppPackageName == null) {
                    throw new IllegalArgumentException("App package name should not be null. Please add argument 'appPackageName' in the json. actionId: " + actionInfo.getId());
                }
                driver.terminateApp(removeAppPackageName);
                break;
            case "back":
                driver.pressKey(AndroidKey.BACK);
                break;
            case "home":
                driver.pressKey(AndroidKey.HOME);
                break;
            case "move":
                Integer xVector = (Integer) arguments.get("xVector");
                Integer yVector = (Integer) arguments.get("yVector");
                if (xVector == null || yVector == null) {
                    throw new IllegalArgumentException("Destination is not defined. Please add argument 'xVector' and 'yVector' in the json. actionId: " + actionInfo.getId());
                }
                driver.scroll(webElement, xVector, yVector);
                break;
            case "swipe":
                String direction = (String) arguments.get("direction");
                if (direction == null) {
                    throw new IllegalArgumentException("Direction is not defined. Please add argument 'direction' in the json. actionId: " + actionInfo.getId());
                }
                driver.swipe(direction);
                break;
            case "longClick":
                Integer duration = (Integer) arguments.get("duration");
                if (duration == null) {
                    throw new IllegalArgumentException("Duration is not defined. Please add argument 'duration' in the json. actionId: " + actionInfo.getId());
                }
                driver.longClick(duration, webElement);
                break;
            case "assert":
                String attribute = (String) arguments.get("attribute");
                String expectedValue = (String) arguments.get("expectedValue");
                if (attribute == null || expectedValue == null) {
                    throw new IllegalArgumentException("Assert info is not defined. Please add argument 'attribute' and 'expectedValue' in the json. actionId: " + actionInfo.getId());
                }
                driver.assertElementAttribute(webElement, attribute, expectedValue);
                break;
            case "sleep":
                Integer timeout = (Integer) arguments.get("duration");
                if (timeout == null) {
                    throw new IllegalArgumentException("Duration is not defined. Please add argument 'timeout' in the json. actionId: " + actionInfo.getId());
                }
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case "getInfo":
                String attributeKey = (String) arguments.get("attribute");
                String id = (String) arguments.get("id");
                if (attributeKey == null || id == null) {
                    throw new IllegalArgumentException("Assert info is not defined. Please add argument 'attribute' and 'id' in the json. actionId: " + actionInfo.getId());
                }
                String info = driver.getInfo(webElement, attributeKey);
                keyToInfoMap.put(id, info);
                break;
            case "dragAndDrop":
                xVector = (Integer) arguments.get("xVector");
                yVector = (Integer) arguments.get("yVector");
                String toElementStr = (String) arguments.get("toElement");
                if (xVector != null && yVector != null) {
                    driver.dragAndDrop(webElement, xVector, yVector);
                } else if (toElementStr != null) {
                    BaseElementInfo toElementInfo;
                    if (driver instanceof AndroidDriverController) {
                        toElementInfo = AndroidElementInfo.getAndroidElementFromJson(JSONObject.parseObject(toElementStr));
                    } else if (driver instanceof WindowsDriverController) {
                        toElementInfo = JSON.parseObject(toElementStr, WindowsElementInfo.class);
                    } else if (driver instanceof EdgeDriverController) {
                        toElementInfo = JSON.parseObject(toElementStr, EdgeElementInfo.class);
                    } else {
                        throw new IllegalArgumentException("Fail to parse the 'toElement' in the json. actionId: " + actionInfo.getId());
                    }
                    WebElement toElement = findElement(driver, toElementInfo);
                    driver.dragAndDrop(webElement, toElement);
                } else {
                    throw new IllegalArgumentException("Destination is not defined. Please add argument 'xVector' & 'yVector' or 'toElement' in the json. actionId: " + actionInfo.getId());
                }
                break;
            case "switchToUrl":
                String url = (String) arguments.get("url");
                driver.switchToUrl(url);
                if (url == null) {
                    throw new IllegalArgumentException("Url is not defined. Please add argument 'url' and 'id' in the json. actionId: " + actionInfo.getId());
                }
                break;
            default:
                throw new IllegalStateException("action fail" +
                        "" +
                        "" +
                        "ed. actionId:" + actionInfo.getId() + "/t" + "actionType:" + actionInfo.getActionType());

        }
    }
}
