// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service.generation;

import com.microsoft.hydralab.common.util.PageNode;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhoule
 * @date 07/21/2023
 */

public abstract class AbstractCaseGeneration {
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
            if (source == target) {
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

    private String extractElementAttr(String attrName, String elementStr) {
        String[] attrs = elementStr.split(attrName + ": ");
        if (attrs.length > 1 && !attrs[1].startsWith(",")) {
            return attrs[1].split(",")[0];
        }
        return "";
    }

    /**
     * explore all path of page node
     *
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
            explorePageNodePath(entry.getValue(), StringUtils.isEmpty(nodePath) ? String.valueOf(pageNode.getId()) : nodePath + "_" + pageNode.getId(),
                    StringUtils.isEmpty(action) ? String.valueOf(entry.getKey()) : action + "," + entry.getKey(), explorePaths);
        }
    }

    public abstract File generateCaseFile(PageNode pageNode, List<PageNode.ExplorePath> explorePaths);

    public abstract File generateCaseFile(PageNode pageNode, PageNode.ExplorePath explorePaths, File caseFolder);
}
