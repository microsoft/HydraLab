// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.t2c.runner.elements.AndroidElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.EdgeElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.WindowsElementInfo;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class T2CJsonParser {
    private Map<String, String> driveIdToTypeMap = new HashMap<>();
    private final Logger logger;

    public T2CJsonParser(Logger logger) {
        this.logger = logger;
    }

    private TestInfo parseJson(String json) {
        JSONObject jsonObject = JSON.parseObject(json);

        JSONArray driverJsonArray = jsonObject.getJSONArray("drivers");
        JSONArray caseJsonArray = jsonObject.getJSONArray("cases");

        ArrayList<DriverInfo> driverList = getDriverList(driverJsonArray);
        ArrayList<ActionInfo> caseList = getActionList(caseJsonArray);

        return new TestInfo(driverList, caseList);
    }

    private ArrayList<DriverInfo> getDriverList(JSONArray driverJsonArray) {
        ArrayList<DriverInfo> driverList = new ArrayList<>();
        for (Iterator iterator = driverJsonArray.iterator(); iterator.hasNext(); ) {
            JSONObject driverJsonObject = (JSONObject) iterator.next();
            logger.info("Driver: " + driverJsonObject.toJSONString());
            String id = driverJsonObject.getString("id");
            String platform = driverJsonObject.getString("platform");
            driveIdToTypeMap.put(id, platform);
            JSONObject initMassage = driverJsonObject.getJSONObject("init");
            String launcherApp = "";
            String initUrl = "";
            if (initMassage.containsKey("launcherApp")) {
                launcherApp = initMassage.getString("launcherApp");
            }
            if (initMassage.containsKey("initUrl")) {
                initUrl = initMassage.getString("initUrl");
            }
            DriverInfo driverInfo = new DriverInfo(id, platform, launcherApp, initUrl);
            driverList.add(driverInfo);
        }
        return driverList;
    }

    private ArrayList<ActionInfo> getActionList(JSONArray caseJsonArray) {
        ArrayList<ActionInfo> caseList = new ArrayList<>();
        ActionInfo actionInfo = null;
        AndroidElementInfo androidElement = null;
        WindowsElementInfo windowsElement = null;
        EdgeElementInfo edgeElement = null;

        for (Iterator iterator = caseJsonArray.iterator(); iterator.hasNext(); ) {
            JSONObject caseJsonObject = (JSONObject) iterator.next();

            Integer id = caseJsonObject.getInteger("index");
            String driverId = caseJsonObject.getString("driverId");

            JSONObject elementInfo = caseJsonObject.getJSONObject("elementInfo");
            //get element:android/ios/windows/web

            JSONObject action = caseJsonObject.getJSONObject("action");
            logger.info("Action: " + action.toJSONString());
            String actionType = action.getString("actionType");
            Map<String, Object> arguments = (Map<String, Object>) action.getJSONObject("arguments");
            boolean isOptional = caseJsonObject.containsKey("isOptional") ? caseJsonObject.getBoolean("isOptional") :
                    caseJsonObject.containsKey("isOption") ? caseJsonObject.getBoolean("isOption") :
                            false;

            if (elementInfo != null && !elementInfo.isEmpty()) {
                if (driveIdToTypeMap.get(driverId).equals("android")) {
                    androidElement = AndroidElementInfo.getAndroidElementFromJson(elementInfo);
                    actionInfo = new ActionInfo(id, androidElement, actionType, arguments, driverId, isOptional);
                }
                if (driveIdToTypeMap.get(driverId).equals("windows")) {
                    windowsElement = JSON.parseObject(caseJsonObject.getString("elementInfo"), WindowsElementInfo.class);
                    actionInfo = new ActionInfo(id, windowsElement, actionType, arguments, driverId, isOptional);
                }
                if (driveIdToTypeMap.get(driverId).equals("browser")) {
                    edgeElement = JSON.parseObject(caseJsonObject.getString("elementInfo"), EdgeElementInfo.class);
                    actionInfo = new ActionInfo(id, edgeElement, actionType, arguments, driverId, isOptional);
                }
            }else {
                actionInfo = new ActionInfo(id, null, actionType, arguments, driverId, isOptional);
            }
            caseList.add(actionInfo);
            Comparator<ActionInfo> comparator = (o1, o2) -> {
                if (Objects.equals(o1.getId(), o2.getId())) {
                    throw new RuntimeException("Same Index Found In The Action Info");
                }
                if (o1.getId() > o2.getId()) {
                    return 1;
                } else {
                    return -1;
                }
            };

            caseList.sort(comparator);
        }

        return caseList;
    }

    public TestInfo parseJsonFile(String path) {
        String json = "";
        File jsonFile = new File(path);
        FileReader fileReader;
        try {
            fileReader = new FileReader(jsonFile);
            int ch = 0;
            StringBuffer stringBuffer = new StringBuffer();
            while ((ch = fileReader.read()) != -1) {
                stringBuffer.append((char) ch);
            }
            fileReader.close();
            json = stringBuffer.toString();
            return parseJson(json);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
