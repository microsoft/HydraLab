// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.appium;

import com.microsoft.hydralab.performance.PerformanceManager;

import java.util.Map;

public class ThreadParam {
    private static InheritableThreadLocal<AppiumParam> appiumParam = new InheritableThreadLocal<>();
    private static InheritableThreadLocal<Map<String, String>> configMap = new InheritableThreadLocal<>();
    private static InheritableThreadLocal<PerformanceManager> performanceManager = new InheritableThreadLocal<>();

    public static void init(AppiumParam appiumParamTemp, Map<String, String> configMapParam, PerformanceManager performanceManagerTemp) {
        clean();
        appiumParam.set(appiumParamTemp);
        configMap.set(configMapParam);
        performanceManager.set(performanceManagerTemp);
    }

    public static void clean() {
        appiumParam.remove();
        configMap.remove();
        performanceManager.remove();
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
    public static PerformanceManager getPerformanceManager() {

        PerformanceManager manager = performanceManager.get();

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
