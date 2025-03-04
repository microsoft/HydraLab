// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.maestro;

import com.microsoft.hydralab.agent.runner.TestRunDeviceOrchestrator;
import com.microsoft.hydralab.agent.runner.TestRunner;
import com.microsoft.hydralab.agent.runner.TestTaskRunCallback;
import com.microsoft.hydralab.agent.runner.XmlBuilder;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.LogUtils;
import com.microsoft.hydralab.performance.PerformanceTestManagementService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author zhoule
 * @date 07/10/2023
 */

public class MaestroRunner extends TestRunner {
    private static final String TEST_CASE_FOLDER = "caseFolder";

    private static final int MAJOR_MAESTRO_VERSION = 1;
    private static final int MINOR_MAESTRO_VERSION = -1;

    public MaestroRunner(AgentManagementService agentManagementService, TestTaskRunCallback testTaskRunCallback, TestRunDeviceOrchestrator testRunDeviceOrchestrator,
                         PerformanceTestManagementService performanceTestManagementService) {
        super(agentManagementService, testTaskRunCallback, testRunDeviceOrchestrator, performanceTestManagementService);
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.maestro, MAJOR_MAESTRO_VERSION, MINOR_MAESTRO_VERSION));
    }

    @Override
    protected void run(TestRunDevice testRunDevice, TestTask testTask, TestRun testRun) throws Exception {
        Logger logger = testRun.getLogger();

        /* run the test */
        logger.info("Start Maestro test");
        checkTestTaskCancel(testTask);
        testRun.setTestStartTimeMillis(System.currentTimeMillis());
        performanceTestManagementService.testRunStarted();

        loadCaseFiles(testRun.getResultFolder(), testTask.getTestAppFile());
        MaestroListener maestroListener = new MaestroListener(agentManagementService, testRunDevice,
                testRun, testTask, testRunDeviceOrchestrator, performanceTestManagementService);
        maestroListener.startRecording(testTask.getTimeOutSecond());
        File xmlFile = generateResultXMLFile(testRun);
        String command = buildCommand(testRunDevice, testRun, testTask.getTaskRunArgs(), xmlFile);
        checkTestTaskCancel(testTask);
        try {
            Process process = Runtime.getRuntime().exec(command);
            MaestroResultReceiver resultReceiver = new MaestroResultReceiver(process.getInputStream(), maestroListener, logger);
            resultReceiver.start();
            process.waitFor();
            /* set paths */
            testRun.setTestXmlReportPath(
                    agentManagementService.getTestBaseRelPathInUrl(xmlFile));
            File gifFile = maestroListener.getGifFile();
            if (gifFile.exists() && gifFile.length() > 0) {
                testRun.setTestGifPath(agentManagementService.getTestBaseRelPathInUrl(gifFile));
            }
        } catch (Exception e) {
            logger.error("Maestro test failed", e);
            testRun.setTestErrorMessage(e.getMessage());
        }
        checkTestTaskCancel(testTask);
    }

    private void loadCaseFiles(File resultFolder, File testAppFile) {
        File caseFolder = new File(resultFolder, TEST_CASE_FOLDER);
        if (!caseFolder.exists()) {
            caseFolder.mkdirs();
        }
        FileUtil.unzipFile(testAppFile.getAbsolutePath(), caseFolder.getAbsolutePath());
    }

    private File generateResultXMLFile(TestRun testRun) {
        File xmlFile;
        try {
            xmlFile = File.createTempFile(XmlBuilder.TEST_RESULT_FILE_PREFIX, ".xml", testRun.getResultFolder());
        } catch (IOException e) {
            throw new HydraLabRuntimeException("Failed to create xml result file", e);
        }
        return xmlFile;
    }

    private String buildCommand(TestRunDevice testRunDevice, TestRun testRun, Map<String, String> taskRunArgs, File xmlFile) {
        // sample: maestro --device 123456 test -e var=aaa --format junit --output /tmp/result.xml /tmp/caseFolder/
        StringBuilder argString = new StringBuilder();
        if (taskRunArgs != null && !taskRunArgs.isEmpty()) {
            taskRunArgs.forEach((k, v) -> argString.append(" -e ").append(k.replaceAll("\\s|\"", "")).append("=").append(v.replaceAll("\\s|\"", "")));
        }
        String commFormat;
        if (StringUtils.isBlank(argString.toString())) {
            commFormat = "maestro --device %s test --format junit --output %s %s/";
        } else {
            commFormat = "maestro --device %s test " + argString + " --format junit --output %s %s/";
        }

        File caseFolder = new File(testRun.getResultFolder(), TEST_CASE_FOLDER);

        String command = String.format(commFormat, testRunDevice.getDeviceInfo().getSerialNum(), xmlFile.getAbsolutePath(), caseFolder.getAbsolutePath());
        testRun.getLogger().info("Maestro command: " + LogUtils.scrubSensitiveArgs(command));

        return command;
    }
}
