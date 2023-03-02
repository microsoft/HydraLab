// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.appium;

import java.util.Map;

/**
 * TODO: rename this to TestRunThreadContext and move this above to package com.microsoft.hydralab
 */
public final class ThreadParam {
    private static final InheritableThreadLocal<AppiumParam> APPIUM_PARAM = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<Map<String, String>> CONFIG_MAP = new InheritableThreadLocal<>();

    private ThreadParam() {
    }

    public static void init(AppiumParam appiumParamTemp, Map<String, String> configMapParam) {
        clean();
        APPIUM_PARAM.set(appiumParamTemp);
        CONFIG_MAP.set(configMapParam);
    }

    public static void clean() {
        APPIUM_PARAM.remove();
        CONFIG_MAP.remove();
    }

    public static AppiumParam getAppiumParam() {
        if (APPIUM_PARAM == null) {
            return new AppiumParam();
        }
        AppiumParam temp = APPIUM_PARAM.get();
        if (temp == null) {
            return new AppiumParam();
        }
        return temp;
    }

    public static String getConfigString(String key) {
        if (CONFIG_MAP == null) {
            return null;
        }
        Map<String, String> temp = CONFIG_MAP.get();
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
