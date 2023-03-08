// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.config.AppOptions;
import com.microsoft.hydralab.common.entity.common.AgentUpdateTask;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.util.CommandOutputReceiver;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.ShellUtils;
import com.microsoft.hydralab.common.util.ThreadPoolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author zhoule
 */
@Service
public class AgentManageService {
    private final Logger logger = LoggerFactory.getLogger(AgentManageService.class);
    @Resource
    AgentWebSocketClientService agentWebSocketClientService;
    @Resource
    private AppOptions appOptions;
    @Resource
    StorageServiceClientProxy storageServiceClientProxy;

    public void updateAgentPackage(AgentUpdateTask updateTask, String path) {
        Runnable run = () -> {
            sendMessageToCenter(true, "Download package file.", "", path);
            File downloadToFile = new File(appOptions.getLocation(), updateTask.getPackageInfo().getFileName());
            storageServiceClientProxy.download(downloadToFile, updateTask.getPackageInfo());
            sendMessageToCenter(true, "Download Package Success!", "", path);

            restartAgent(updateTask.getPackageInfo().getFileName(), path);
        };

        ThreadPoolUtil.SCREENSHOT_EXECUTOR.execute(run);
    }

    private void sendMessageToCenter(Boolean isProceed, String message, String errorDesc, String path) {
        logger.info(message);
        AgentUpdateTask.UpdateMsg updateMsg = new AgentUpdateTask.UpdateMsg(isProceed, message, errorDesc);
        Message socketMessage = new Message();
        socketMessage.setPath(path);
        socketMessage.setBody(updateMsg);
        agentWebSocketClientService.send(socketMessage);
    }

    public void restartAgent(String packageFileName, String path) {
        String[] restartArgs;
        String scriptPath;

        sendMessageToCenter(true, "Init command Arr and check restart script exists or not.", "", path);

        String packageName = packageFileName == null ? "" : packageFileName;
        if (!ShellUtils.isConnectedToWindowsOS) {
            scriptPath = appOptions.getLocation() + File.separator + Const.AgentConfig.RESTART_FILE_MAC;
            restartArgs = new String[]{"sh", scriptPath, packageName};
        } else {
            scriptPath = appOptions.getLocation() + File.separator + Const.AgentConfig.RESTART_FILE_WIN;
            restartArgs = new String[]{"cmd.exe", "/c", "Start", scriptPath, packageName};
        }
        File scriptFile = new File(scriptPath);
        if (scriptFile.exists()) {
            sendMessageToCenter(true, "Init Command Success!", "", path);
        } else {
            sendMessageToCenter(false, "Restart Script Didn't Exist!", "", path);
            return;
        }

        sendMessageToCenter(true, "Restart Agent.", "", path);
        Process process = null;
        try {
            Thread.sleep(2000);
            process = Runtime.getRuntime().exec(restartArgs);
            CommandOutputReceiver err = new CommandOutputReceiver(process.getErrorStream(), logger);
            CommandOutputReceiver out = new CommandOutputReceiver(process.getInputStream(), logger);
            err.start();
            out.start();
            process.waitFor();
            sendMessageToCenter(true, "Restart Agent Success! Check The Agent Log For Detail!", "", path);
        } catch (Exception e) {
            e.printStackTrace();
            sendMessageToCenter(false, "Exec Command Failed! Check The Agent Log For Detail!", e.getMessage(),
                    path);
        }
    }
}
