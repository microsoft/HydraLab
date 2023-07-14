// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.util.PageNode;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;

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
        // leverage dom4j to load xml
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

        for (Element page : pages) {
            PageNode pageNode = new PageNode();
            int id = Integer.parseInt(page.attributeValue("id"));
            pageNode.setId(id);
            pageNodes.put(id, pageNode);
        }

        for (Element action : actions) {
            int source = Integer.parseInt(action.attributeValue("source"));
            int target = Integer.parseInt(action.attributeValue("target"));
            if(source == target) {
                continue;
            }
            int actionId = Integer.parseInt(action.attributeValue("id"));
            pageNodes.get(source).getActionInfoList().add(parserAction(action));
            pageNodes.get(source).getChildPageNodeMap().put(actionId, pageNodes.get(target));
        }

        return pageNodes.get(0);
    }

    private PageNode.ActionInfo parserAction(Element element) {
        PageNode.ActionInfo actionInfo = new PageNode.ActionInfo();
        actionInfo.setId(Integer.parseInt(element.attributeValue("id")));
        actionInfo.setActionType("click");

        PageNode.ElementInfo elementInfo = new PageNode.ElementInfo();
        String sourceCode = element.element("attvalues").element("attvalue").attributeValue("value");
        elementInfo.setText(extractElementAttr("Text", sourceCode));
        elementInfo.setClassName(extractElementAttr("Class", sourceCode));
        elementInfo.setClickable(Boolean.parseBoolean(extractElementAttr("Clickable", sourceCode)));
        elementInfo.setResourceId(extractElementAttr("ResourceID", sourceCode));
        actionInfo.setTestElement(elementInfo);
        return actionInfo;
    }

    private static String extractElementAttr(String attrName, String elementStr) {
        String[] attrs = elementStr.split(attrName + ": ");
        if (attrs.length > 1 && !attrs[1].startsWith(",")) {
            return attrs[1].split(",")[0];
        }
        return "";
    }
}
