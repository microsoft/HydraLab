// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.maestro;

import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhoule
 * @date 07/11/2023
 */

public class MaestroResultReceiver extends Thread {
    private final InputStream inputStream;
    private final Logger logger;
    private final MaestroListener listener;
    private static final String KEY_CASE_NAME = "caseName";
    private static final String KEY_TEST_SECONDS = "testSeconds";
    private static final String KEY_ERROR = "error";
    private boolean isTestRunFailed = false;

    public MaestroResultReceiver(InputStream inputStream, MaestroListener listener, Logger logger) {
        this.inputStream = inputStream;
        this.logger = logger;
        this.listener = listener;
    }

    public void run() {
        listener.testRunStarted();
        try {
            InputStreamReader isr = new InputStreamReader(inputStream, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                logger.info(line);
                if (line.startsWith("[Passed]")) {
                    Map<String, String> caseInfo = analysisCaseInfo(line);
                    listener.testEnded(caseInfo.get(KEY_CASE_NAME), Integer.parseInt(caseInfo.get(KEY_TEST_SECONDS)));
                } else if (line.startsWith("[Failed]")) {
                    Map<String, String> caseInfo = analysisCaseInfo(line);
                    listener.testFailed(caseInfo.get(KEY_CASE_NAME), Integer.parseInt(caseInfo.get(KEY_TEST_SECONDS)), caseInfo.get(KEY_ERROR));
                } else if (line.contains("Debug output")) {
                    isTestRunFailed = true;
                    logger.info("Start to analysis debug output");
                }
                if (isTestRunFailed) {
                    if (LogUtils.isLegalStr(line, Const.RegexString.LINUX_ABSOLUTE_PATH, false)
                            || LogUtils.isLegalStr(line, Const.RegexString.WINDOWS_ABSOLUTE_PATH, false)) {
                        listener.testRunFailed(line);
                    }
                }
            }
            listener.testRunEnded();
            isr.close();
            bufferedReader.close();
        } catch (IOException e) {
            logger.info("Exception:" + e);
            e.printStackTrace();
        } finally {
            synchronized (this) {
                notify();
            }
        }
    }

    private Map<String, String> analysisCaseInfo(String line) {
        Map<String, String> infoMap = new HashMap<>();
        String[] msg = line.split(" ");
        if (msg.length < 3) {
            infoMap.put(KEY_CASE_NAME, "caseParseError");
            infoMap.put(KEY_TEST_SECONDS, "0");
        }
        infoMap.put(KEY_CASE_NAME, msg[1]);
        String testSeconds = msg[2].replace("s", "").replace("(", "").replace(")", "");
        try {
            Integer.parseInt(testSeconds);
        } catch (NumberFormatException e) {
            logger.info("Exception:" + e);
            testSeconds = "0";
        }
        infoMap.put(KEY_TEST_SECONDS, testSeconds);
        if (msg.length >= 4) {
            infoMap.put(KEY_ERROR, line.substring(line.indexOf(msg[3])));
        }
        return infoMap;
    }
}