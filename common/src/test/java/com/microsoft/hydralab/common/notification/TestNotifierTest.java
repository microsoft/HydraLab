// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.notification;

import com.microsoft.hydralab.notification.TestNotifier;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @author taoran
 * @date 7/3/2023
 */

public class TestNotifierTest {
    @Test
    public void testSendTestNotificationWithNull() {
        TestNotifier testNotifier = new TestNotifier();
        TestNotifier.TestNotification notification = new TestNotifier.TestNotification();
        testNotifier.sendTestNotification(null, notification, LoggerFactory.getLogger(TestNotifierTest.class));
    }

    @Test
    public void testSendTestNotificationWithEmpty() {
        TestNotifier testNotifier = new TestNotifier();
        TestNotifier.TestNotification notification = new TestNotifier.TestNotification();
        testNotifier.sendTestNotification("", notification, LoggerFactory.getLogger(TestNotifierTest.class));
    }
}
