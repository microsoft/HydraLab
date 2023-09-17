// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.maestro;

import com.microsoft.hydralab.common.util.CommandOutputReceiver;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.LogUtils;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhoule
 * @date 07/11/2023
 */

public class MaestroResultReceiver extends CommandOutputReceiver {
    private final MaestroListener listener;
    private static final String KEY_CASE_NAME = "caseName";
    private static final String KEY_TEST_SECONDS = "testSeconds";
    private static final String KEY_ERROR = "error";
    private boolean isTestRunFailed = false;

    public MaestroResultReceiver(InputStream inputStream, MaestroListener listener, Logger logger) {
        super(inputStream, logger);
        this.listener = listener;
    }

    @Override
    protected boolean handleEachLine(String line) {
        if (line.startsWith("[Passed]")) {
            //for example: [Passed] test_flow1 (28s)
            Map<String, String> caseInfo = analysisCaseInfo(line);
            // KEY_CASE_NAME: test_flow1
            // KEY_TEST_SECONDS: 28
            listener.testEnded(caseInfo.get(KEY_CASE_NAME), Integer.parseInt(caseInfo.get(KEY_TEST_SECONDS)));
        } else if (line.startsWith("[Failed]")) {
            //for example: [Failed] test_flow2 (20s) (Element not found: UiSelector[CLASS=android.widget.TextView, INSTANCE=1])
            Map<String, String> caseInfo = analysisCaseInfo(line);
            // KEY_CASE_NAME: test_flow2
            // KEY_TEST_SECONDS: 20
            // KEY_ERROR: (Element not found: UiSelector[CLASS=android.widget.TextView, INSTANCE=1])
            listener.testFailed(caseInfo.get(KEY_CASE_NAME), Integer.parseInt(caseInfo.get(KEY_TEST_SECONDS)), caseInfo.get(KEY_ERROR));
        } else if (line.contains("Debug output")) {
            // for example: ==== Debug output (logs & screenshots) ====
            isTestRunFailed = true;
            if (logger != null) {
                logger.info("Start to analysis debug output");
            }
        }
        if (isTestRunFailed) {
            // for example: /Users/xxx/.maestro/test/2023-07-11_110500
            if (LogUtils.isLegalStr(line, Const.RegexString.LINUX_ABSOLUTE_PATH, false)
                    || LogUtils.isLegalStr(line, Const.RegexString.WINDOWS_ABSOLUTE_PATH, false)) {
                listener.testRunFailed(line);
            }
        }
        return super.handleEachLine(line);
    }

    /**
     * analysis case info
     *
     * @param line
     * @return
     * @example
     */
    private Map<String, String> analysisCaseInfo(String line) {
        Map<String, String> infoMap = new HashMap<>();
        String[] msg = line.split(" ");
        if (msg.length < 3) {
            infoMap.put(KEY_CASE_NAME, "caseParseError");
            infoMap.put(KEY_TEST_SECONDS, "0");
        }
        infoMap.put(KEY_CASE_NAME, msg[1]);
        // for example: (28s), except result 28
        String testSeconds = msg[2].replace("s", "").replace("(", "").replace(")", "");
        try {
            Integer.parseInt(testSeconds);
        } catch (NumberFormatException e) {
            if (logger != null) {
                logger.info("Exception:" + e);
            }
            testSeconds = "0";
        }
        infoMap.put(KEY_TEST_SECONDS, testSeconds);
        if (msg.length >= 4) {
            infoMap.put(KEY_ERROR, line.substring(line.indexOf(msg[3])));
        }
        return infoMap;
    }
}