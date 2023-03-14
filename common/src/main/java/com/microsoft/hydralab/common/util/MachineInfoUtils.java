// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public final class MachineInfoUtils {

    private MachineInfoUtils() {

    }

    protected static Logger classLogger = LoggerFactory.getLogger(MachineInfoUtils.class);

    private static final String DETECT_WINDOWS_LAPTOP_SCRIPT_NAME = "DetectWindowsLaptop.ps1";

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

    public static boolean isOnWindowsLaptop() {
        if (!isOnWindows()) {
            return false;
        }

        File scriptFile = new File(DETECT_WINDOWS_LAPTOP_SCRIPT_NAME);
        if (!scriptFile.exists()) {
            try {
                InputStream resourceAsStream =
                        FileUtils.class.getClassLoader().getResourceAsStream(DETECT_WINDOWS_LAPTOP_SCRIPT_NAME);
                OutputStream out = new FileOutputStream(scriptFile);
                IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
                out.close();
            } catch (IOException e) {
                classLogger.error("Failed to find app handler script", e);
                return false;
            }
        }

        String result = ShellUtils.execLocalCommandWithResult(scriptFile.getAbsolutePath(), classLogger);
        return result != null && "Yes".equalsIgnoreCase(result);
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
