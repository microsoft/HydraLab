package com.microsoft.hydralab.utils;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class HydraLabClientUtilsTest {

    @Test
    public void runTestOnDeviceWithApp() {
    }

    @Test
    public void getLatestCommitInfo() {
        String commitId = null;
        String commitCount = null;
        String commitMsg = null;
        File commandDir = new File(".");
        try {
            commitId = HydraLabClientUtils.getLatestCommitHash(commandDir);
            commitCount = HydraLabClientUtils.getCommitCount(commandDir, commitId);
            commitMsg = HydraLabClientUtils.getCommitMessage(commandDir, commitId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assertions.assertNotNull(commitId, "Get commit id error");
        Assertions.assertNotNull(commitCount, "Get commit count error");
        Assertions.assertNotNull(commitMsg, "Get commit message error");
    }
}