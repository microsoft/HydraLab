// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;

/**
 * @author taoran
 * @date 3/28/2023
 */

public final class RobotUtils {
    private static Robot robot;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private RobotUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void keyPressString(String content) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable tText = new StringSelection(content);
        //Set to clipboard
        clip.setContents(tText, null);
        keyPressWithCtrl(KeyEvent.VK_V);
        robot.delay(100);
    }

    public static void keyPress(int key) {
        robot.keyPress(key);
        robot.keyRelease(key);
        robot.delay(100);
    }

    public static void keyPressWithCtrl(int key) {
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(key);
        robot.keyRelease(key);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.delay(100);
    }
}
