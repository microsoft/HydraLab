// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.notification;

import com.microsoft.hydralab.notification.TestNotifier;
import com.microsoft.hydralab.performance.PerformanceInspector;
import com.microsoft.hydralab.performance.PerformanceResultParser;
import com.microsoft.hydralab.performance.PerformanceTestResult;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author taoran
 * @date 7/3/2023
 */

public class TestNotifierTest {
    @Test
    public void test() {
        TestNotifier testNotifier = new TestNotifier();
        PerformanceTestResult performanceTestResult = new PerformanceTestResult();
        performanceTestResult.inspectorType = PerformanceInspector.PerformanceInspectorType.INSPECTOR_ANDROID_MEMORY_INFO;
        performanceTestResult.parserType = PerformanceResultParser.PerformanceResultParserType.PARSER_ANDROID_MEMORY_INFO;

        List<PerformanceTestResult> resultList = List.of(new PerformanceTestResult());

        TestNotifier.TestNotification notification = new TestNotifier.TestNotification();
        notification.testTaskId = "1234";
        notification.reportLink = "http://hydradevicenetwork.azurewebsites.net/portal/index.html#/";
        notification.content = resultList;
        notification.testStartTime = "2023-07-01";
        testNotifier.sendTestNotification("111", notification, LoggerFactory.getLogger(TestNotifierTest.class));
    }
}
