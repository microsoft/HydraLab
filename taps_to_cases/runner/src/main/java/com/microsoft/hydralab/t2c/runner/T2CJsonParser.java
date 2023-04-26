// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.t2c.runner.elements.AndroidElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.EdgeElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.IOSElementInfo;
import com.microsoft.hydralab.t2c.runner.elements.WindowsElementInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class T2CJsonParser {
    private final Map<String, String> driveIdToTypeMap = new HashMap<>();
    private final Logger logger;

    public T2CJsonParser(Logger logger) {
        this.logger = logger;
    }

    private TestInfo parseJson(String json) {
        JSONObject jsonObject = JSON.parseObject(json);

        JSONArray driverJsonArray = jsonObject.getJSONArray("drivers");
        //Keep this to compat the old json format
        JSONArray caseJsonArray = jsonObject.getJSONArray("cases");
        if (caseJsonArray == null) {
            caseJsonArray = jsonObject.getJSONArray("actions");
        }

        ArrayList<DriverInfo> driverList = getDriverList(driverJsonArray);
        ArrayList<ActionInfo> caseList = getActionList(caseJsonArray, driverList);

        return new TestInfo(driverList, caseList);
    }

    private ArrayList<DriverInfo> getDriverList(JSONArray driverJsonArray) {
        ArrayList<DriverInfo> driverList = new ArrayList<>();
        for (Object driverJson : driverJsonArray) {
            JSONObject driverJsonObject = (JSONObject) driverJson;
            logger.info("Driver: " + driverJsonObject.toJSONString());
            String id = driverJsonObject.getString("id");
            String platform = driverJsonObject.getString("platform");
            driveIdToTypeMap.put(id, platform);
            //Keep this to compat the old json format
            JSONObject initMassage = driverJsonObject.getJSONObject("init");
            if (initMassage == null) {
                initMassage = driverJsonObject.getJSONObject("setup");
            }
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

    private ArrayList<ActionInfo> getActionList(JSONArray caseJsonArray, ArrayList<DriverInfo> driverList) {
        ArrayList<ActionInfo> actionListInJson = new ArrayList<>();
        PerformanceActionInitializer batteryTestInitializer = new PerformanceActionInitializer(ActionInfo.ACTION_TYPE_INSPECT_BATTERY_USAGE);
        PerformanceActionInitializer memoryTestInitializer = new PerformanceActionInitializer(ActionInfo.ACTION_TYPE_INSPECT_MEM_USAGE);
        ActionInfo actionInfo = null;
        AndroidElementInfo androidElement = null;
        WindowsElementInfo windowsElement = null;
        EdgeElementInfo edgeElement = null;
        IOSElementInfo iosElement = null;

        for (int i = 0; i < caseJsonArray.size(); i++) {
            JSONObject caseJsonObject = caseJsonArray.getJSONObject(i);
            String driverId = caseJsonObject.getString("driverId");
            String description = "";
            if (caseJsonObject.containsKey("description")) {
                description = caseJsonObject.getString("description");
            }
            JSONObject elementInfo = caseJsonObject.getJSONObject("elementInfo");
            //get element:android/ios/windows/web

            JSONObject action = caseJsonObject.getJSONObject("action");
            logger.info("Action: " + action.toJSONString());
            String actionType = action.getString("actionType");
            Map<String, Object> arguments = action.getJSONObject("arguments");
            boolean isOptional = caseJsonObject.containsKey("isOptional") ? caseJsonObject.getBoolean("isOptional") :
                    caseJsonObject.containsKey("isOption") ? caseJsonObject.getBoolean("isOption") :
                            false;

            if (elementInfo != null && !elementInfo.isEmpty()) {
                if (driveIdToTypeMap.get(driverId).equalsIgnoreCase("android")) {
                    androidElement = JSON.parseObject(caseJsonObject.getString("elementInfo"), AndroidElementInfo.class);
                    actionInfo = new ActionInfo(i, androidElement, actionType, arguments, driverId, description, isOptional);
                }
                if (driveIdToTypeMap.get(driverId).equalsIgnoreCase("windows")) {
                    windowsElement = JSON.parseObject(caseJsonObject.getString("elementInfo"), WindowsElementInfo.class);
                    actionInfo = new ActionInfo(i, windowsElement, actionType, arguments, driverId, description, isOptional);
                }
                if (driveIdToTypeMap.get(driverId).equalsIgnoreCase("browser")) {
                    edgeElement = JSON.parseObject(caseJsonObject.getString("elementInfo"), EdgeElementInfo.class);
                    actionInfo = new ActionInfo(i, edgeElement, actionType, arguments, driverId, description, isOptional);
                }
                if (driveIdToTypeMap.get(driverId).equalsIgnoreCase("ios")) {
                    iosElement = JSON.parseObject(caseJsonObject.getString("elementInfo"), IOSElementInfo.class);
                    actionInfo = new ActionInfo(i, iosElement, actionType, arguments, driverId, description, isOptional);
                }
            } else {
                actionInfo = new ActionInfo(i, null, actionType, arguments, driverId, description, isOptional);
            }
            actionListInJson.add(actionInfo);
            if (ActionInfo.ACTION_TYPE_INSPECT_BATTERY_USAGE.equalsIgnoreCase(actionType)) {
                String targetApp = (String) arguments.get("targetApp");
                batteryTestInitializer.add(driverId, targetApp);

            }
            if (ActionInfo.ACTION_TYPE_INSPECT_MEM_USAGE.equalsIgnoreCase(actionType)) {
                String targetApp = (String) arguments.get("targetApp");
                memoryTestInitializer.add(driverId, targetApp);
            }
        }

        ArrayList<ActionInfo> actionsToInitDriver = new ArrayList<>();
        //Setup Activate App for each driver
        for (DriverInfo driver : driverList) {
            if (driver.getPlatform().equalsIgnoreCase("ios")
                    || driver.getPlatform().equalsIgnoreCase("android")) {
                if (!StringUtils.isEmpty(driver.getLauncherApp())) {
                    Map<String, Object> arguments = new HashMap<>() {
                        {
                            put("appPackageName", driver.getLauncherApp());
                        }
                    };
                    ActionInfo initAction = new ActionInfo(-1, null, "activateApp", arguments, driver.getId(), "Activate App: " + driver.getLauncherApp(), true);
                    actionsToInitDriver.add(initAction);
                } else {
                    Map<String, Object> arguments = new HashMap<>();
                    ActionInfo initAction = new ActionInfo(-1, null, "home", arguments, driver.getId(), "Back To Home", true);
                    actionsToInitDriver.add(initAction);
                }
            }
        }

        ArrayList<ActionInfo> fullList = new ArrayList<>();
        fullList.addAll(memoryTestInitializer.exportInitActionInfoList());
        fullList.addAll(batteryTestInitializer.exportInitActionInfoList());
        fullList.addAll(actionsToInitDriver);
        fullList.addAll(actionListInJson);
        return fullList;
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

    private static class PerformanceActionInitializer {

        private final String actionType;
        private final ArrayList<PerfInitActionMetaData> mPerfInitActions = new ArrayList<>();

        PerformanceActionInitializer(String perfActionType) {
            this.actionType = perfActionType;
        }

        public void add(String deviceId, String targetApp) {
            PerfInitActionMetaData newAction = new PerfInitActionMetaData(deviceId, targetApp);
            boolean isExist = false;
            for (PerfInitActionMetaData action : mPerfInitActions) {
                if (action.equalTo(newAction)) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                mPerfInitActions.add(newAction);
            }
        }

        public ArrayList<ActionInfo> exportInitActionInfoList() {
            ArrayList<ActionInfo> initActionInfoList = new ArrayList<>();
            for (PerfInitActionMetaData actionMetaData : mPerfInitActions) {
                String description = String.format("Init %s module for driver: %s, app: %s.", actionType, actionMetaData.getDriverId(), actionMetaData.getTargetApp());
                Map<String, Object> arguments = new HashMap<>() {
                    {
                        put("targetApp", actionMetaData.getTargetApp());
                        put("description", description);
                        put("isReset", true);
                    }
                };
                ActionInfo actionInfo = new ActionInfo(-1, null, actionType, arguments, actionMetaData.getDriverId(), description, true);
                initActionInfoList.add(actionInfo);
            }
            return initActionInfoList;
        }
    }

    private static class PerfInitActionMetaData {
        private final String driverIdInJson;
        private final String targetAppInJson;

        PerfInitActionMetaData(String deviceId, String targetApp) {
            driverIdInJson = deviceId;
            targetAppInJson = targetApp;
        }

        boolean equalTo(PerfInitActionMetaData performanceInitAction) {
            return performanceInitAction != null
                    && this.driverIdInJson.equals(performanceInitAction.driverIdInJson)
                    && this.targetAppInJson.equals(performanceInitAction.targetAppInJson);
        }

        public String getDriverId() {
            return driverIdInJson;
        }

        public String getTargetApp() {
            return targetAppInJson;
        }
    }
}
