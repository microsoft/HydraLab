// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.analysis.scanner;

import com.microsoft.hydralab.agent.runner.analysis.AnalysisRunner;
import com.microsoft.hydralab.agent.service.TestTaskEngineService;
import com.microsoft.hydralab.common.entity.common.AnalysisTask;
import com.microsoft.hydralab.common.entity.common.TaskResult;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.management.AgentManagementService;
import org.slf4j.Logger;

import java.io.File;

/**
 * @author zhoule
 * @date 11/20/2023
 */

public abstract class Scanner<T extends TaskResult> extends AnalysisRunner {

    public Scanner(AgentManagementService agentManagementService, TestTaskEngineService testTaskEngineService, boolean isEnabled) {
        super(agentManagementService, testTaskEngineService, isEnabled);
    }

    @Override
    public void execute(AnalysisTask analysisTask, TestRun testRun) throws Exception {
        T report = initReport(analysisTask);
        report.setTaskId(analysisTask.getId());
        report.setTaskRunId(testRun.getId());
        for (AnalysisTask.AnalysisConfig config : analysisTask.getAnalysisConfigs()) {
            String analysisType = config.getAnalysisType();
            if (AnalysisTask.AnalysisType.LEAK_INFO.name().equals(analysisType)) {
                report = scanSensitiveWords(report, analysisTask.getAppFile(), testRun.getResultFolder(), config, testRun.getLogger());
            } else if (AnalysisTask.AnalysisType.FILE_SIZE.name().equals(analysisType)) {
                report = analysisPackage(report, analysisTask.getAppFile(), testRun.getResultFolder(), config, testRun.getLogger());
            }
        }
        testRun.setTaskResult(report);
    }

    abstract T initReport(AnalysisTask task);

    abstract T analysisPackage(T result, File file, File outputFolder, AnalysisTask.AnalysisConfig config, Logger logger);

    abstract T scanSensitiveWords(T result, File file, File outputFolder, AnalysisTask.AnalysisConfig config, Logger logger);
}
