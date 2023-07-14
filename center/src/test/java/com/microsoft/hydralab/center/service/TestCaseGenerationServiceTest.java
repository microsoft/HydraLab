package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.test.BaseTest;
import com.microsoft.hydralab.common.util.PageNode;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

class TestCaseGenerationServiceTest extends BaseTest {

    @Resource
    CaseGenerationService caseGenerationService;

    @Test
    void testParserXMLToPageNode() {
        PageNode rootNode = caseGenerationService.parserXMLToPageNode("src/test/resources/test_route_map.xml");
        System.out.println(rootNode);
    }
}