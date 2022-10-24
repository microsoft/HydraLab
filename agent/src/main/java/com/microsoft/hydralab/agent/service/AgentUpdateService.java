// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.config.AppOptions;
import com.microsoft.hydralab.agent.runner.TestThreadPool;
import com.microsoft.hydralab.common.entity.common.AgentUpdateTask;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.management.impl.IOSDeviceManager;
import com.microsoft.hydralab.common.util.CommandOutputReceiver;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.DownloadUtils;
import com.microsoft.hydralab.common.util.blob.BlobStorageClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

@Service
public class AgentUpdateService {
    private final Logger logger = LoggerFactory.getLogger(AgentUpdateService.class);
    @Resource
    DeviceManager deviceManager;
    @Resource
    AgentWebSocketClientService agentWebSocketClientService;
    @Resource
    private AppOptions appOptions;
    @Resource
    BlobStorageClient blobStorageClient;

    public void updateAgentPackage(AgentUpdateTask updateTask) {
        Runnable run = () -> {
            sendMessageToCenter(true, "Download package file.", "");
            File downloadToFile = new File(appOptions.getLocation(), updateTask.getPackageInfo().getFileName());
            blobStorageClient.downloadFileFromBlob(downloadToFile, updateTask.getPackageInfo().getBlobContainer(), updateTask.getPackageInfo().getBlobPath());
            sendMessageToCenter(true, "Download Package Success!", "");

            sendMessageToCenter(true, "Init command Arr and check restart script exists or not.", "");
            String[] restartArgs;
            String scriptPath;
            if (deviceManager instanceof IOSDeviceManager && !((IOSDeviceManager) deviceManager).isDeviceConnectedToWindows()) {
                scriptPath = appOptions.getLocation() + File.separator + Const.AgentConfig.restartFileIOS;
                restartArgs = new String[]{"sh", scriptPath, updateTask.getPackageInfo().getFileName()};
            } else {
                scriptPath = appOptions.getLocation() + File.separator + Const.AgentConfig.restartFileWin;
                restartArgs = new String[]{"cmd.exe", "/c", "Start", scriptPath, updateTask.getPackageInfo().getFileName()};
            }
            File scriptFile = new File(scriptPath);
            if (scriptFile.exists()) {
                sendMessageToCenter(true, "Init Command Success!", "");
            } else {
                sendMessageToCenter(false, "Restart Script Didn't Exist!", "");
                return;
            }

            sendMessageToCenter(true, "Restart Agent.", "");
            Process process = null;
            try {
                Thread.sleep(2000);
                process = Runtime.getRuntime().exec(restartArgs);
                CommandOutputReceiver err = new CommandOutputReceiver(process.getErrorStream(), logger);
                CommandOutputReceiver out = new CommandOutputReceiver(process.getInputStream(), logger);
                err.start();
                out.start();
                process.waitFor();
                sendMessageToCenter(true, "Restart Agent Success! Check The Agent Log For Detail!", "");
            } catch (Exception e) {
                e.printStackTrace();
                sendMessageToCenter(false, "Exec Command Failed! Check The Agent Log For Detail!", e.getMessage());
            }
        };

        TestThreadPool.executor.execute(run);
    }

    public void sendMessageToCenter(Boolean isProceed, String message, String errorDesc) {
        logger.info(message);
        AgentUpdateTask.UpdateMsg updateMsg = new AgentUpdateTask.UpdateMsg(isProceed, message, errorDesc);
        Message socketMessage = new Message();
        socketMessage.setPath(Const.Path.AGENT_UPDATE);
        socketMessage.setBody(updateMsg);
        agentWebSocketClientService.send(socketMessage);
    }
}
