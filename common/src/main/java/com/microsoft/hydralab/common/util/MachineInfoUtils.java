// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class MachineInfoUtils {

    public static boolean isOnMacOS() {
        Properties props = System.getProperties();
        String osName = props.getProperty("os.name");
        return osName.contains("Mac");
    }

    public static boolean isOnWindows() {
        Properties props = System.getProperties();
        String osName = props.getProperty("os.name");
        return osName.toLowerCase(Locale.US).contains("windows");
    }

    public static String getCountryNameFromCode(String code) {
        if (code == null) {
            return null;
        }
        for (Locale availableLocale : Locale.getAvailableLocales()) {
            if (availableLocale == null) {
                continue;
            }
            if (code.toUpperCase().equalsIgnoreCase(availableLocale.getCountry())) {
                return availableLocale.getDisplayCountry();
            }
        }
        return null;
    }
}
