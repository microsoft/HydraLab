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
        String commitId;
        String commitCount;
        String commitMsg;
        File commandDir = new File(".");
        try {
            commitId = HydraLabClientUtils.getLatestCommitHash(commandDir);
            Assertions.assertNotNull(commitId, "Get commit id error");

            commitCount = HydraLabClientUtils.getCommitCount(commandDir, commitId);
            Assertions.assertNotNull(commitCount, "Get commit count error");

            commitMsg = HydraLabClientUtils.getCommitMessage(commandDir, commitId);
            Assertions.assertNotNull(commitMsg, "Get commit message error");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}