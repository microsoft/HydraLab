// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.appium;

import java.util.Map;

/**
 * TODO: rename this to TestRunThreadContext and move this above to package com.microsoft.hydralab
 */
public class ThreadParam {
    private static final InheritableThreadLocal<AppiumParam> appiumParam = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<Map<String, String>> configMap = new InheritableThreadLocal<>();

    public static void init(AppiumParam appiumParamTemp, Map<String, String> configMapParam) {
        clean();
        appiumParam.set(appiumParamTemp);
        configMap.set(configMapParam);
    }

    public static void clean() {
        appiumParam.remove();
        configMap.remove();
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
