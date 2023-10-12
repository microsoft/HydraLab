// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service.generation;

import com.microsoft.hydralab.center.util.CenterConstant;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.PageNode;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author zhoule
 * @date 07/14/2023
 */

@Service
public class MaestroCaseGenerationService extends AbstractCaseGeneration {
    /**
     * generate maestro case files and zip them
     *
     * @param pageNode
     * @param explorePaths
     * @return
     */
    @Override
    public File generateCaseFile(PageNode pageNode, List<PageNode.ExplorePath> explorePaths) {
        // create temp folder to store case files
        File tempFolder = new File(CenterConstant.CENTER_TEMP_FILE_DIR, DateUtil.fileNameDateFormat.format(new Date()));
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }
        // generate case files
        for (PageNode.ExplorePath explorePath : explorePaths) {
            generateCaseFile(pageNode, explorePath, tempFolder);
        }
        if (tempFolder.listFiles().length == 0) {
            return null;
        }
        // zip temp folder
        File zipFile = new File(tempFolder.getParent() + "/" + tempFolder.getName() + ".zip");
        FileUtil.zipFile(tempFolder.getAbsolutePath(), zipFile.getAbsolutePath());
        FileUtil.deleteFile(tempFolder);
        return zipFile;
    }

    @Override
    public File generateCaseFile(PageNode pageNode, PageNode.ExplorePath explorePath, File caseFolder) {
        File maestroCaseFile = new File(caseFolder, explorePath.getPath() + ".yaml");
        String caseContent = buildConfigSection(pageNode.getPageName());
        caseContent += buildDelimiter();
        caseContent += buildCommandSection("launch", null);
        String[] actionIds = explorePath.getActions().split(",");
        PageNode pageNodeCopy = pageNode;
        for (String actionId : actionIds) {
            PageNode.ActionInfo action = pageNodeCopy.getActionInfoList().stream().filter(actionInfo -> actionInfo.getId() == Integer.parseInt(actionId)).findFirst().get();
            caseContent += buildCommandSection(action.getActionType(), action.getArguments());
            pageNodeCopy = pageNodeCopy.getChildPageNodeMap().get(Integer.parseInt(actionId));
        }
        caseContent += buildCommandSection("stop", null);
        FileUtil.writeToFile(caseContent, maestroCaseFile.getAbsolutePath());
        return maestroCaseFile;
    }

    private String buildConfigSection(String appId) {
        return "appId: " + appId + "\n";
    }

    private String buildDelimiter() {
        return "---\n";
    }

    private String buildCommandSection(String actionType, Map<String, Object> arguments) {
        String command = "-";
        switch (actionType) {
            case "launch":
                command = command + " launchApp\n";
                break;
            case "click":
                command = command + " tapOn:";
                if (arguments.size() == 0) {
                    throw new HydraLabRuntimeException("arguments is empty");
                }
                if (arguments.containsKey("defaultValue")) {
                    command = command + " " + arguments.get("defaultValue") + "\n";
                    break;
                }
                command = command + "\n";
                for (String key : arguments.keySet()) {
                    command = command + "    " + key + ": \"" + arguments.get(key) + "\"\n";
                }
                break;
            case "stop":
                command = command + " stopApp\n";
                break;
            default:
                throw new HydraLabRuntimeException("Unsupported action type: " + actionType);
        }
        return command;
    }
}
