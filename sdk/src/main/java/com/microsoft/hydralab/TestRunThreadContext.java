// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab;

import com.microsoft.hydralab.appium.AppiumParam;

import java.util.Map;

public class TestRunThreadContext {
    private static final InheritableThreadLocal<AppiumParam> appiumParam = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<ITestRun> testRunThreadLocal = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<Map<String, String>> configMap = new InheritableThreadLocal<>();

    public static void init(ITestRun testRun, AppiumParam appiumParamTemp, Map<String, String> configMapParam) {
        clean();
        testRunThreadLocal.set(testRun);
        appiumParam.set(appiumParamTemp);
        configMap.set(configMapParam);
    }

    public static void clean() {
        appiumParam.remove();
        configMap.remove();
    }

    public static AppiumParam getAppiumParam() {
        AppiumParam temp = appiumParam.get();
        if (temp == null) {
            return new AppiumParam();
        }
        return temp;
    }

    public static String getConfigString(String key) {
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

    public static ITestRun getTestRun() {
        return testRunThreadLocal.get();
    }
}
