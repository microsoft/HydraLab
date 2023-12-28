// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner.analysis.scanner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.microsoft.hydralab.agent.runner.analysis.scanner.apk.ApkAnalyzeExecutor;
import com.microsoft.hydralab.agent.runner.analysis.scanner.apk.ApkCanaryExecutor;
import com.microsoft.hydralab.agent.runner.analysis.scanner.apk.ApkLeaksExecutor;
import com.microsoft.hydralab.agent.service.TestTaskEngineService;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.common.AnalysisTask;
import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.scanner.ApkReport;
import com.microsoft.hydralab.common.management.AgentManagementService;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

/**
 * @author zhoule
 * @date 11/20/2023
 */

public class APKScanner extends Scanner<ApkReport> {

    private static final int MAJOR_PYTHON_VERSION = 3;
    private static final int MINOR_PYTHON_VERSION = 8;

    private static final int MAJOR_APK_LEAKS_VERSION = 2;
    private static final int MINOR_APK_LEAKS_VERSION = 2;

    public APKScanner(AgentManagementService agentManagementService, TestTaskEngineService testTaskEngineService, boolean isEnabled) {
        super(agentManagementService, testTaskEngineService, isEnabled);
    }

    @Override
    ApkReport initReport(AnalysisTask task) {
        return new ApkReport(task.getPkgName());
    }

    @Override
    public ApkReport analysisPackage(ApkReport report, File file, File outputFolder, AnalysisTask.AnalysisConfig config, Logger logger) {
        Assertions.assertTrue(file.exists(), "apk file not exist: " + file.getAbsolutePath());

        if (ApkCanaryExecutor.EXECUTOR_TYPE.equals(config.getExecutor())) {
            ApkCanaryExecutor apkCanaryExecutor = new ApkCanaryExecutor(outputFolder);
            apkCanaryExecutor.analyzeApk(report, file.getAbsolutePath(), logger);
            logger.info(JSON.toJSONString(report, SerializerFeature.PrettyFormat));
        } else if (ApkAnalyzeExecutor.EXECUTOR_TYPE.equals(config.getExecutor())) {
            ApkAnalyzeExecutor apkAnalyzeExecutor = new ApkAnalyzeExecutor(outputFolder);
            apkAnalyzeExecutor.analyzeApk(report, file.getAbsolutePath(), logger);
        }
        return report;
    }

    @Override
    public ApkReport scanSensitiveWords(ApkReport report, File file, File outputFolder, AnalysisTask.AnalysisConfig config, Logger logger) {

        ApkLeaksExecutor apkLeaksExecutor = new ApkLeaksExecutor(outputFolder);
        apkLeaksExecutor.analyzeLeaks(report, file.getAbsolutePath(), config.getAnalysisConfig(), logger);
        return report;
    }

    @Override
    protected List<EnvCapabilityRequirement> getEnvCapabilityRequirements() {
        return List.of(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.python, MAJOR_PYTHON_VERSION, MINOR_PYTHON_VERSION),
                new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.apkleaks, MAJOR_APK_LEAKS_VERSION, MINOR_APK_LEAKS_VERSION),
                new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.apkanalyzer, -1, -1));
    }

    @Override
    protected String getFunctionName() {
        return Task.RunnerType.APK_SCANNER.name();
    }
}
