// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.util.DateUtil;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * @author zhoule
 * @date 10/31/2022
 */
@Service
public class XmlBuilder {
    private static final String TEST_RESULT_FILE_SUFFIX = ".xml";
    private static final String TEST_RESULT_FILE_PREFIX = "hydra_result_";
    private static final String TEST_RESULT_FILE_ENCODE_PROPERTY = "encoding";
    private static final String TEST_RESULT_FILE_ENCODE_VALUE = "UTF-8";
    private static final String TEST_RESULT_FILE_OUTPUT_VALUE = "xml";
    private static final String TEST_RESULT_FILE_FORMAT_PROPERTY = "indent-number";
    private static final String TEST_RESULT_FILE_FORMAT_VALUE = "2";
    private static final String TEST_RESULT_FILE_INDENT_VALUE = "yes";

    private static final String TESTSUITE = "testsuite";
    private static final String TESTCASE = "testcase";
    private static final String FAILURE = "failure";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TIME = "time";
    private static final String ATTR_FAILURES = "failures";
    private static final String ATTR_SKIPPED = "skipped";
    private static final String ATTR_TESTS = "tests";
    private static final String ATTR_CLASSNAME = "classname";
    private static final String TIMESTAMP = "timestamp";
    private static final String HOSTNAME = "hostname";

    public String buildTestResultXml(TestTask testTask, DeviceTestTask deviceTestTask) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        Document document = db.newDocument();
        Element testSuite = buildTestSuite(document, testTask, deviceTestTask);
        document.appendChild(testSuite);

        File xmlFile = File.createTempFile(TEST_RESULT_FILE_PREFIX, TEST_RESULT_FILE_SUFFIX, deviceTestTask.getDeviceTestResultFolder());
        transferToFile(document, xmlFile);
        return xmlFile.getAbsolutePath();
    }

    private Element buildTestSuite(Document document, TestTask testTask, DeviceTestTask deviceTestTask) throws UnknownHostException {
        Element testSuite = document.createElement(TESTSUITE);

        testSuite.setAttribute(ATTR_NAME, testTask.getTestSuite());
        testSuite.setAttribute(ATTR_TESTS, String.valueOf(deviceTestTask.getTotalCount()));
        testSuite.setAttribute(ATTR_FAILURES, String.valueOf(deviceTestTask.getFailCount()));
        testSuite.setAttribute(ATTR_TIME, Double.toString((double) (deviceTestTask.getTestEndTimeMillis() - deviceTestTask.getTestStartTimeMillis()) / 1000.f));
        testSuite.setAttribute(TIMESTAMP, DateUtil.appCenterFormat2.format(DateUtil.localToUTC(new Date(deviceTestTask.getTestStartTimeMillis()))));
        testSuite.setAttribute(HOSTNAME, InetAddress.getLocalHost().getHostName());
        if (deviceTestTask.getTestUnitList() != null) {
            testSuite.setAttribute(ATTR_SKIPPED, String.valueOf(deviceTestTask.getTotalCount() - deviceTestTask.getTestUnitList().size()));
            for (AndroidTestUnit unitTest : deviceTestTask.getTestUnitList()) {
                Element testCase = buildTestCase(document, unitTest);
                testSuite.appendChild(testCase);
            }
        } else {
            testSuite.setAttribute(ATTR_SKIPPED, String.valueOf(deviceTestTask.getTotalCount()));
        }
        return testSuite;
    }

    private Element buildTestCase(Document document, AndroidTestUnit unitTest) {
        Element testCase = document.createElement(TESTCASE);
        testCase.setAttribute(ATTR_NAME, unitTest.getTestName());
        testCase.setAttribute(ATTR_CLASSNAME, unitTest.getTestedClass());
        testCase.setAttribute(ATTR_TIME, Double.toString((double) (unitTest.getEndTimeMillis() - unitTest.getStartTimeMillis()) / 1000.f));
        if (!unitTest.isSuccess()) {
            Element failure = document.createElement(FAILURE);
            failure.setTextContent(unitTest.getStack());
            testCase.appendChild(failure);
        }
        return testCase;
    }

    private void transferToFile(Document document, File xmlFile) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(TEST_RESULT_FILE_FORMAT_PROPERTY, Integer.valueOf(TEST_RESULT_FILE_FORMAT_VALUE));
        Source xmlSource = new DOMSource(document);
        Result outputTarget = new StreamResult(xmlFile);

        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(TEST_RESULT_FILE_ENCODE_PROPERTY, TEST_RESULT_FILE_ENCODE_VALUE);
        transformer.setOutputProperty(OutputKeys.METHOD, TEST_RESULT_FILE_OUTPUT_VALUE);
        transformer.setOutputProperty(OutputKeys.INDENT, TEST_RESULT_FILE_INDENT_VALUE);
        transformer.transform(xmlSource, outputTarget);
    }
}
