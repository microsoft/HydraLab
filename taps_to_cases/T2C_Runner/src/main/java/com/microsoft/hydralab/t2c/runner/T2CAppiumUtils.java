// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner;

import com.microsoft.hydralab.t2c.runner.controller.BaseDriverController;
import com.microsoft.hydralab.t2c.runner.elements.BaseElementInfo;
import io.appium.java_client.android.nativekey.AndroidKey;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

public class T2CAppiumUtils {
    static HashMap<String,String> keyToInfoMap = new HashMap<>();
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
        try{
        assert elementFinded != null : "Can not find element.\n" + element.getElementInfo();
        }catch (Throwable e){
            e.printStackTrace();
        }
        return elementFinded;
    }

    public static void doAction(BaseDriverController driver, ActionInfo actionInfo) {
        boolean isOption = actionInfo.isOption();
        if(isOption){
            try {
                chooseActionType(driver, actionInfo);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            chooseActionType(driver, actionInfo);
        }
    }

    public static void chooseActionType(BaseDriverController driver, ActionInfo actionInfo) {
            String ActionType = actionInfo.getActionType();
            BaseElementInfo element = actionInfo.getTestElement();
            WebElement webElement = findElement(driver, element);
            Map<String, Object> arguments = actionInfo.getArguments();

        switch (ActionType){
            case "click":
                driver.click(webElement);
                break;
            case "input":
                String content = (String) arguments.get("content");
                if(arguments.containsKey("id")){
                    String id = (String) arguments.get("id");
                    driver.input(webElement, keyToInfoMap.get(id));
                }else{
                    driver.input(webElement, content);
                }
                break;
            case "clear":
                driver.clear(webElement);
                break;
            case "activateApp":
                String appPackageName = (String) arguments.get("appPackageName");
                driver.activateApp(appPackageName);
                break;
            case "terminateApp":
                String appPackageName1 = (String) arguments.get("appPackageName");
                driver.terminateApp(appPackageName1);
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
                driver.scroll(webElement, xVector, yVector);
                break;
            case "swipe":
                String direction = (String) arguments.get("direction");
                driver.swipe(direction);
                break;
            case "longClick":
                Integer duration = (Integer) arguments.get("duration");
                driver.longClick(duration,webElement);
                break;
            case "assert":
                String attribute = (String) arguments.get("attribute");
                String expectedValue = (String) arguments.get("expectedValue");
                driver.assertElementAttribute(webElement,attribute,expectedValue);
                break;
            case "sleep":
                Integer timeout =(Integer) arguments.get("duration");
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case "getInfo":
                attribute = (String) arguments.get("attribute");
                String id = (String) arguments.get("id");
                String info = driver.getInfo(webElement,attribute);
                keyToInfoMap.put(id,info);
                break;
            case "dragAndDrop":
                xVector = (Integer) arguments.get("xVector");
                yVector = (Integer) arguments.get("yVector");
                driver.dragAndDrop(webElement,xVector,yVector);
                break;
            default:
                throw new IllegalStateException("action fail" +
                        "" +
                        "" +
                        "ed. actionId:" + actionInfo.getId()+ "/t" + "actionType:" + actionInfo.getActionType());

        }
    }
}
