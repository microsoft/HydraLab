// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.util;

import com.microsoft.identity.service.essentials.Mise;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * @author Li Shen
 * @date 3/10/2025
 */

public final class SecurityUtil {
    private SecurityUtil() {
    }

    public static Mise generateMISEClient(Logger logger) {
        Mise mise = Mise.createClient();
//        mise.assignLogMessageCallback(new Mise.ILogCallback() {
//            @Override
//            public void callback(MiseLogLevel level, String message, Object arg2) {
//                logger.info("[MISE]" + message);
//            }
//        }, null);

        String miseConfig = getMISEConfiguration(logger);
        mise.configure(miseConfig, "AzureAD");

        return mise;
    }

    public static String getMISEConfiguration(Logger logger) {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        try (InputStream inputStream = systemClassLoader.getResourceAsStream("MISE/appSettings.json")) {
            if (inputStream != null) {
                try (InputStreamReader streamReader = new InputStreamReader(inputStream)){

                    BufferedReader reader = new BufferedReader(streamReader);
                    return reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }
        } catch (IOException e) {
            logger.error("[MISE] error: " + e);
        }
        return null;
    }
}
