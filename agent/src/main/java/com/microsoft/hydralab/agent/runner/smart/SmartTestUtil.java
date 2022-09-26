// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.smart;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.agent.SmartTestParam;
import com.microsoft.hydralab.common.util.CommandOutputReceiver;
import com.microsoft.hydralab.common.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Objects;
import java.util.Set;


public class SmartTestUtil {
    private static String filePath = "";
    private static String folderPath = "";
    private static String stringFolderPath = "";
    Logger log = LoggerFactory.getLogger(SmartTestUtil.class);

    public SmartTestUtil(String location) {
        File testBaseDir = new File(location);
        String name = Const.SmartTestConfig.zipFileName;
        String folderName = Const.SmartTestConfig.zipFolderName;

        folderPath = testBaseDir.getAbsolutePath() + "/" + Const.SmartTestConfig.zipFolderName + "/";
        stringFolderPath = testBaseDir.getAbsolutePath() + "/" + Const.SmartTestConfig.stringFolderName + "/";

        try {
            InputStream resourceAsStream = FileUtils.class.getClassLoader().getResourceAsStream(name);
            if (resourceAsStream == null) {
                return;
            }
            File smartTestZip = new File(testBaseDir, name);
            File smartTestFolder = new File(testBaseDir, folderName);
            if (smartTestZip.exists()) {
                FileUtil.deleteFileRecursively(smartTestZip);
            }
            if (smartTestFolder.exists()) {
                FileUtil.deleteFileRecursively(smartTestFolder);
            }
            OutputStream out = new FileOutputStream(smartTestZip);
            IOUtils.copy(Objects.requireNonNull(resourceAsStream), out);
            out.close();
            FileUtil.unzipFile(smartTestZip.getAbsolutePath(), testBaseDir.getAbsolutePath());
            if (smartTestZip.exists()) {
                FileUtil.deleteFileRecursively(smartTestZip);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        initStringPool();
        filePath = folderPath + Const.SmartTestConfig.pyFileName;
        String requireFilePath = folderPath + Const.SmartTestConfig.requireFileName;
        String[] command = new String[]{"pip3", "install", "-r", requireFilePath};
        try {
            Process proc = Runtime.getRuntime().exec(command);
            CommandOutputReceiver err = new CommandOutputReceiver(proc.getErrorStream(), log);
            CommandOutputReceiver out = new CommandOutputReceiver(proc.getInputStream(), log);
            err.start();
            out.start();
            proc.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String runPYFunction(SmartTestParam smartTestParam, Logger logger) throws Exception {
        String res = null;
        String[] runArgs = new String[7];
        runArgs[0] = "python";
        runArgs[1] = filePath;
        runArgs[2] = smartTestParam.apkPath;
        runArgs[3] = smartTestParam.deviceInfo;
        runArgs[4] = smartTestParam.modelInfo;
        runArgs[5] = smartTestParam.testSteps;
        runArgs[6] = smartTestParam.stringTextFolder;

        for (String tempArg : runArgs) {
            logger.info(tempArg);
        }
        Process proc = Runtime.getRuntime().exec(runArgs);
        SmartTestLog err = new SmartTestLog(proc.getErrorStream(), logger);
        SmartTestLog out = new SmartTestLog(proc.getInputStream(), logger);
        err.start();
        out.start();
        res = out.getContent();
        proc.waitFor();

        return res;
    }

    public JSONObject analysisRes(JSONObject data) {
        JSONObject coverage = data.getJSONObject(Const.SmartTestConfig.coverageTag);
        JSONObject result = new JSONObject();
        Set<String> activityKeys = coverage.keySet();
        int totalActivity = activityKeys.size();
        int totalElement = 0;
        int visitedActivity = 0;
        int visitedElement = 0;

        for (String activityKey : activityKeys) {
            JSONObject activityInfo = coverage.getJSONObject(activityKey);
            if (!activityInfo.getBoolean(Const.SmartTestConfig.visitTag)) {
                continue;
            }
            Set<String> elementKeys = activityInfo.keySet();
            elementKeys.remove(Const.SmartTestConfig.visitTag);
            totalElement = totalElement + elementKeys.size();
            for (String elementKey : elementKeys) {
                if (activityInfo.getBoolean(elementKey)) {
                    visitedElement++;
                }
            }
            visitedActivity++;
        }
        result.put("activity", visitedActivity + "/" + totalActivity);
        result.put("element", visitedElement + "/" + visitedElement);
        return result;
    }


    public void initStringPool() {
        File stringDir = new File(stringFolderPath);
        if (!stringDir.exists()) {
            if (!stringDir.mkdirs()) {
                throw new RuntimeException("mkdirs fail for: " + stringDir);
            }
        }
        String[] fileNames = Const.SmartTestConfig.stringFileNames.split(",");
        for (String fileName : fileNames) {
            creatTxtFile(stringFolderPath, fileName);
        }
    }

    public void creatTxtFile(String path, String name) {
        String filenameTemp = path + name + ".txt";
        File filename = new File(filenameTemp);
        //generate string txt file if not exist
        if (!filename.exists()) {
            try {
                filename.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getStringFolderPath() {
        return stringFolderPath;
    }
}
