// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.util.CenterConstant;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.PageNode;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhoule
 * @date 07/14/2023
 */

@Service
public class CaseGenerationService {

    public PageNode parserXMLToPageNode(String xmlFilePath) {
        // read xml file, get page node and action info
        Document document = null;
        SAXReader saxReader = new SAXReader();
        try {
            document = saxReader.read(xmlFilePath);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        List<Element> pages = document.getRootElement().element("graph").element("nodes").elements("node");
        List<Element> actions = document.getRootElement().element("graph").element("edges").elements("edge");

        Map<Integer, PageNode> pageNodes = new HashMap<>();
        // init page node
        for (Element page : pages) {
            PageNode pageNode = new PageNode();
            int id = Integer.parseInt(page.attributeValue("id"));
            pageNode.setId(id);
            pageNodes.put(id, pageNode);
        }
        // init action info
        for (Element action : actions) {
            int source = Integer.parseInt(action.attributeValue("source"));
            int target = Integer.parseInt(action.attributeValue("target"));
            if(source == target) {
                continue;
            }
            int actionId = Integer.parseInt(action.attributeValue("id"));
            //link action to page
            pageNodes.get(source).getActionInfoList().add(parserAction(action));
            //link page to page
            pageNodes.get(source).getChildPageNodeMap().put(actionId, pageNodes.get(target));
        }
        return pageNodes.get(0);
    }

    /**
     * explore all path of page node
     * @param pageNode
     * @param nodePath
     * @param action
     * @param explorePaths
     */
    public void explorePageNodePath(PageNode pageNode, String nodePath, String action, List<PageNode.ExplorePath> explorePaths) {
        if (pageNode.getChildPageNodeMap().isEmpty()) {
            explorePaths.add(new PageNode.ExplorePath(nodePath + "_" + pageNode.getId(), action));
            return;
        }

        for (Map.Entry<Integer, PageNode> entry : pageNode.getChildPageNodeMap().entrySet()) {
            explorePageNodePath(entry.getValue(),
                    StringUtils.isEmpty(nodePath) ? String.valueOf(pageNode.getId()) : nodePath + "_" + pageNode.getId(),
                    StringUtils.isEmpty(action) ? String.valueOf(entry.getKey()) : action + "," + entry.getKey(), explorePaths);
        }
    }

    private PageNode.ActionInfo parserAction(Element element) {
        PageNode.ActionInfo actionInfo = new PageNode.ActionInfo();
        Map<String, Object> arguments = new HashMap<>();
        actionInfo.setId(Integer.parseInt(element.attributeValue("id")));
        actionInfo.setActionType("click");

        PageNode.ElementInfo elementInfo = new PageNode.ElementInfo();
        String sourceCode = element.element("attvalues").element("attvalue").attributeValue("value");
        elementInfo.setText(extractElementAttr("Text", sourceCode));
        elementInfo.setClassName(extractElementAttr("Class", sourceCode));
        elementInfo.setClickable(Boolean.parseBoolean(extractElementAttr("Clickable", sourceCode)));
        elementInfo.setResourceId(extractElementAttr("ResourceID", sourceCode));
        actionInfo.setTestElement(elementInfo);
        if (!StringUtils.isEmpty(elementInfo.getText())) {
            arguments.put("defaultValue", elementInfo.getText());
        } else if (!StringUtils.isEmpty(elementInfo.getResourceId())) {
            arguments.put("id", elementInfo.getResourceId());
        }
        actionInfo.setArguments(arguments);
        return actionInfo;
    }

    private static String extractElementAttr(String attrName, String elementStr) {
        String[] attrs = elementStr.split(attrName + ": ");
        if (attrs.length > 1 && !attrs[1].startsWith(",")) {
            return attrs[1].split(",")[0];
        }
        return "";
    }

    /**
     * generate maestro case files and zip them
     * @param pageNode
     * @param explorePaths
     * @return
     */
    public File generateMaestroCases(PageNode pageNode, List<PageNode.ExplorePath> explorePaths) {
        // create temp folder to store case files
        File tempFolder = new File(CenterConstant.CENTER_TEMP_FILE_DIR, DateUtil.fileNameDateFormat.format(new Date()));
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }
        // generate case files
        for (PageNode.ExplorePath explorePath : explorePaths) {
            generateMaestroCase(pageNode, explorePath, tempFolder);
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

    private File generateMaestroCase(PageNode pageNode, PageNode.ExplorePath explorePath, File caseFolder) {
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

    public String buildCommandSection(String actionType, Map<String, Object> arguments) {
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
