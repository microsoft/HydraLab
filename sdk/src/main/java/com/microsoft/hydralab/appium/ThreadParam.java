// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.appium;

import com.microsoft.hydralab.performance.PerformanceInspectionService;

import java.util.Map;

public class ThreadParam {
    private static InheritableThreadLocal<AppiumParam> appiumParam = new InheritableThreadLocal<>();
    private static InheritableThreadLocal<Map<String, String>> configMap = new InheritableThreadLocal<>();
    private static InheritableThreadLocal<PerformanceInspectionService> performanceExecutor = new InheritableThreadLocal<>();

    public static void init(AppiumParam appiumParamTemp, Map<String, String> configMapParam, PerformanceInspectionService performanceInspectionServiceTemp) {
        clean();
        appiumParam.set(appiumParamTemp);
        configMap.set(configMapParam);
        performanceExecutor.set(performanceInspectionServiceTemp);
    }

    public static void clean() {
        appiumParam.remove();
        configMap.remove();
        performanceExecutor.remove();
    }

    public static AppiumParam getAppiumParam() {
        if (appiumParam == null) {
            return new AppiumParam();
        }
        AppiumParam temp = appiumParam.get();
        if (temp == null) {
            return new AppiumParam();
        }
        return temp;
    }

    public static PerformanceInspectionService getPerformanceExecutor() {

        PerformanceInspectionService manager = performanceExecutor.get();

        return manager;
    }

    public static String getConfigString(String key) {
        if (configMap == null) {
            return null;
        }
        Map<String, String> temp = configMap.get();
        if (temp == null) {
            return null;
        }
        return temp.get(key);
    }

    public static String getConfigString(String key, String defaultValue) {

        String temp = getConfigString(key);
        if (temp == null) {
            return defaultValue;
        }
        return temp;
    }
}
