package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.service.generation.MaestroCaseGenerationService;
import com.microsoft.hydralab.center.test.BaseTest;
import com.microsoft.hydralab.common.util.PageNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class TestCaseGenerationServiceTest extends BaseTest {

    @Resource
    MaestroCaseGenerationService caseGenerationService;

    @Test
    void testParserXMLToPageNode() {
        PageNode rootNode = caseGenerationService.parserXMLToPageNode("src/test/resources/test_route_map.xml");
        Assertions.assertNotNull(rootNode, "parser xml to page node failed");
        rootNode.setPageName("com.microsoft.appmanager");
        System.out.println(rootNode);
        List<PageNode.ExplorePath> explorePaths = new ArrayList<>();
        caseGenerationService.explorePageNodePath(rootNode, "", "", explorePaths);
        Assertions.assertEquals(explorePaths.size(), 16, "explore path size is not correct");
        File caseZipFile = caseGenerationService.generateCaseFile(rootNode, explorePaths);
        Assertions.assertTrue(caseZipFile.exists());
    }
}