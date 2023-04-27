// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.util;

import com.microsoft.hydralab.agent.config.AppOptions;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.util.CommandOutputReceiver;
import com.microsoft.hydralab.common.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class FileLoadUtil {
    @SuppressWarnings("constantname")
    static final Logger log = LoggerFactory.getLogger(FileLoadUtil.class);
    @Resource
    private AppOptions appOptions;
    @Resource
    StorageServiceClientProxy storageServiceClientProxy;

    private static final Set<String> BLACKLIST_FOLDER = Set.of("config", "hydra", "logs", "SmartTest", "SmartTestString", "storage");

    public void clearAttachments(TestTask testTask) {
        List<StorageFileInfo> attachments = testTask.getTestFileSet().getAttachments();
        if (attachments == null) {
            return;
        }
        for (StorageFileInfo attachment : attachments) {
            if (StorageFileInfo.FileType.COMMON_FILE.equals(attachment.getFileType())) {
                File loadFolder = new File(appOptions.getLocation() + "/" + attachment.getLoadDir());
                FileUtil.deleteFile(loadFolder);
            }
        }
        log.info("Clear common file success");
    }

    public void loadAttachments(TestTask testTask) {
        List<StorageFileInfo> attachments = testTask.getTestFileSet().getAttachments();
        if (attachments == null) {
            return;
        }
        for (StorageFileInfo attachment : attachments) {
            switch (attachment.getFileType()) {
                case StorageFileInfo.FileType.WINDOWS_APP:
                    installWinApp(attachment);
                    break;
                case StorageFileInfo.FileType.COMMON_FILE:
                    loadCommonFile(attachment);
                    break;
                case StorageFileInfo.FileType.APP_FILE:
                    File appFile = downloadFile(attachment);
                    if (testTask.getNeedReinstall()) {
                        Assert.isTrue(appFile != null && appFile.exists(), "Download app file failed!");
                    }
                    testTask.setAppFile(appFile);
                    break;
                case StorageFileInfo.FileType.TEST_APP_FILE:
                    File testAppFile = downloadFile(attachment);
                    Assert.isTrue(testAppFile != null && testAppFile.exists(), "Download test app file failed!");
                    testTask.setTestAppFile(testAppFile);
                    break;
                case StorageFileInfo.FileType.T2C_JSON_FILE:
                    File testJsonFile = downloadFile(attachment);
                    Assert.isTrue(testJsonFile != null && testJsonFile.exists(), "Download test json file failed!");
                    testTask.addTestJsonFile(testJsonFile);
                    break;
                default:
                    break;
            }
        }
    }

    public void installWinApp(StorageFileInfo attachment) {
        try {
            Runtime runtime = Runtime.getRuntime();
            File attachmentFile = downloadFile(attachment, appOptions.getTestPackageLocation(), attachment.getBlobPath());
            String installCommand = "& { Add-AppxPackage -ForceApplicationShutdown -forceupdatefromanyversion -Path '" +
                    attachmentFile.getAbsolutePath() + "' }";
            String[] command = new String[]{"Powershell.exe", "-Command", installCommand};

            log.info("Install Win-App start command array: {}", Arrays.asList(command));
            Process process = runtime.exec(command);
            CommandOutputReceiver err = new CommandOutputReceiver(process.getErrorStream(), log);
            CommandOutputReceiver out = new CommandOutputReceiver(process.getInputStream(), log);
            err.start();
            out.start();
            process.waitFor(300, TimeUnit.SECONDS);
            log.info("Install Win-App success");
        } catch (Exception e) {
            log.error("Install Win-App failed", e);
        }

    }

    public void loadCommonFile(StorageFileInfo attachment) {
        try {
            File loadFolder = new File(appOptions.getLocation() + "/" + attachment.getLoadDir());
            String firstLevelFolder = attachment.getLoadDir().split("/")[0];
            Assert.isTrue(!BLACKLIST_FOLDER.contains(firstLevelFolder),
                    "Load file error : " + loadFolder.getAbsolutePath() + " was contained in blacklist:" + BLACKLIST_FOLDER);
            log.info("Load common file start filename:{} path:{}", attachment.getFileName(), loadFolder.getAbsolutePath());
            File attachmentFile = downloadFile(attachment, appOptions.getLocation(), attachment.getLoadDir() + "/" + attachment.getFileName());
            if (StorageFileInfo.LoadType.UNZIP.equalsIgnoreCase(attachment.getLoadType())) {
                FileUtil.unzipFile(attachmentFile.getAbsolutePath(), loadFolder.getAbsolutePath());
            }
            log.info("Load common file success");
        } catch (Exception e) {
            log.error("Load common file start failed", e);
        }
    }

    private File downloadFile(StorageFileInfo attachment, String location, String targetFilePath) throws IOException {
        File file = new File(location, targetFilePath);
        log.debug("download file from {} to {}", attachment.getBlobUrl(), file.getAbsolutePath());
        storageServiceClientProxy.download(file, attachment);
        return file;
    }

    private File downloadFile(StorageFileInfo attachment) {
        File file = null;
        try {
            file = downloadFile(attachment, appOptions.getTestPackageLocation(), attachment.getBlobPath());
            log.info("Download file success");
        } catch (IOException e) {
            log.error("Download file failed", e);
        }
        return file;
    }
}
