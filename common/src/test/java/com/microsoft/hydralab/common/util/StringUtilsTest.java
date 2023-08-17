package com.microsoft.hydralab.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilsTest {
    @Test
    public void testPathVerification(){
        Assertions.assertTrue(LogUtils.isLegalStr("/opt/homebrew/android_sdk/34.0.0", Const.RegexString.LINUX_ABSOLUTE_PATH,false));
        Assertions.assertFalse(LogUtils.isLegalStr("opt/homebrew/android_sdk/", Const.RegexString.LINUX_ABSOLUTE_PATH,false));
        Assertions.assertTrue(LogUtils.isLegalStr("/opt/homebrew", Const.RegexString.LINUX_ABSOLUTE_PATH,false));
        Assertions.assertTrue(LogUtils.isLegalStr("/Path/To/ab-c/abc 2/12.3", Const.RegexString.LINUX_ABSOLUTE_PATH,false));
        Assertions.assertTrue(LogUtils.isLegalStr("/Path/安卓系统/ab-c/abc 2/12.3", Const.RegexString.LINUX_ABSOLUTE_PATH,false));
        Assertions.assertTrue(LogUtils.isLegalStr("/path/.git/.jdk/to/ab-c/abc 2/12.3?", Const.RegexString.LINUX_ABSOLUTE_PATH,false));
        Assertions.assertFalse(LogUtils.isLegalStr("0", Const.RegexString.LINUX_ABSOLUTE_PATH,false));
        Assertions.assertFalse(LogUtils.isLegalStr("abc", Const.RegexString.LINUX_ABSOLUTE_PATH,false));
        Assertions.assertFalse(LogUtils.isLegalStr("~", Const.RegexString.LINUX_ABSOLUTE_PATH, false));
        Assertions.assertTrue(LogUtils.isLegalStr("/Users/username/.maestro/tests/2023-07-19_160946", Const.RegexString.LINUX_ABSOLUTE_PATH, false));

        Assertions.assertTrue(LogUtils.isLegalStr("C:\\123.5\\AndroidSDK\\a", Const.RegexString.WINDOWS_ABSOLUTE_PATH, false));
        Assertions.assertTrue(LogUtils.isLegalStr("C:\\Program Files (x86)\\Common Files", Const.RegexString.WINDOWS_ABSOLUTE_PATH, false));
        Assertions.assertTrue(LogUtils.isLegalStr("C:\\Program Files (x86)\\安卓SDK", Const.RegexString.WINDOWS_ABSOLUTE_PATH, false));
        Assertions.assertTrue(LogUtils.isLegalStr("C:\\Program Files (x86)\\Common Files\\Microsoft Shared\\Phone Tools\\15.0", Const.RegexString.WINDOWS_ABSOLUTE_PATH, false));
        Assertions.assertFalse(LogUtils.isLegalStr("~", Const.RegexString.WINDOWS_ABSOLUTE_PATH, false));
        Assertions.assertFalse(LogUtils.isLegalStr("Common Files\\Microsoft Shared\\Phone Tools", Const.RegexString.WINDOWS_ABSOLUTE_PATH, false));
        Assertions.assertFalse(LogUtils.isLegalStr("\\Program Files (x86)", Const.RegexString.WINDOWS_ABSOLUTE_PATH, false));
    }

}
