// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.util;

import com.microsoft.hydralab.common.util.CommandOutputReceiver;
import com.microsoft.hydralab.common.util.DownloadUtils;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.agent.config.AppOptions;
import com.microsoft.hydralab.common.entity.common.BlobFileInfo;
import com.microsoft.hydralab.common.entity.common.TestTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class FileLoadUtil {
    static final Logger log = LoggerFactory.getLogger(FileLoadUtil.class);
    @Resource
    private AppOptions appOptions;

    public void clearAttachments(TestTask testTask) {
        List<BlobFileInfo> attachments = testTask.getTestFileSet().getAttachments();
        if (attachments == null) {
            return;
        }
        for (BlobFileInfo attachment : attachments) {
            if (BlobFileInfo.FileType.COMMON_FILE.equals(attachment.getFileType())) {
                File loadFolder = new File(appOptions.getLocation() + "/" + attachment.getLoadDir());
                FileUtil.deleteFile(loadFolder);
            }
        }
        log.info("Clear common file success");
    }

    public void loadAttachments(TestTask testTask) {
        List<BlobFileInfo> attachments = testTask.getTestFileSet().getAttachments();
        if (attachments == null) {
            return;
        }
        for (BlobFileInfo attachment : attachments) {
            switch (attachment.getFileType()) {
                case BlobFileInfo.FileType.WINDOWS_APP:
                    installWinApp(attachment);
                    break;
                case BlobFileInfo.FileType.COMMON_FILE:
                    loadCommonFile(attachment);
                    break;
                case BlobFileInfo.FileType.APP_FILE:
                    File appFile = downloadFromBlob(attachment);
                    Assert.isTrue(appFile != null && appFile.exists(), "Download app file failed!");
                    testTask.setAppFile(appFile);
                    break;
                case BlobFileInfo.FileType.TEST_APP_FILE:
                    File testAppFile = downloadFromBlob(attachment);
                    Assert.isTrue(testAppFile != null && testAppFile.exists(), "Download test app file failed!");
                    testTask.setTestAppFile(testAppFile);
                    break;
                default:
                    break;
            }
        }
    }

    public void installWinApp(BlobFileInfo attachment) {
        try {
            Runtime runtime = Runtime.getRuntime();
            File attachmentFile = downloadFromBlob(attachment.getBlobUrl(), appOptions.getTestPackageLocation(), attachment.getBlobPath());
            String installCommand = "& { Add-AppxPackage -ForceApplicationShutdown -forceupdatefromanyversion -Path '" +
                    attachmentFile.getAbsolutePath() + "' }";
            String[] command = new String[]{"Powershell.exe", "-Command", installCommand};

            log.info("Install Win-App start command array: {}", Arrays.asList(command));
            Process process = runtime.exec(command);
            CommandOutputReceiver err = new CommandOutputReceiver(process.getErrorStream(), log);
            CommandOutputReceiver out = new CommandOutputReceiver(process.getInputStream(), log);
            err.start();
            out.start();
            process.waitFor(60, TimeUnit.SECONDS);
            log.info("Install Win-App success");
        } catch (Exception e) {
            log.error("Install Win-App failed", e);
        }

    }

    public void loadCommonFile(BlobFileInfo attachment) {
        try {
            File loadFolder = new File(appOptions.getLocation() + "/" + attachment.getLoadDir());
            Assert.isTrue(!loadFolder.exists(), "Load file error : folder has been existed!");
            log.info("Load common file start filename:{} path:{}", attachment.getFileName(), loadFolder.getAbsolutePath());
            File attachmentFile = downloadFromBlob(attachment.getBlobUrl(), appOptions.getLocation(), attachment.getLoadDir() + "/" + attachment.getFileName());
            if (BlobFileInfo.LoadType.UNZIP.equals(attachment.getLoadType())) {
                FileUtil.unzipFile(attachmentFile.getAbsolutePath(), loadFolder.getAbsolutePath());
            }
            log.info("Load common file success");
        } catch (Exception e) {
            log.error("Load common file start failed", e);
        }

    }

    private File downloadFromBlob(String blobUrl, String location, String targetFilePath) throws IOException {
        File file = new File(location, targetFilePath);
        log.debug("download file from {} to {}", blobUrl, file.getAbsolutePath());
        DownloadUtils.downloadFileFromUrl(blobUrl, file.getName(), file.getParent());
        return file;
    }

    private File downloadFromBlob(BlobFileInfo attachment) {
        File file = null;
        try {
            file = downloadFromBlob(attachment.getBlobUrl(), appOptions.getTestPackageLocation(), attachment.getBlobPath());
            log.info("Download file success");
        } catch (IOException e) {
            log.error("Download file failed", e);
        }
        return file;
    }
}
