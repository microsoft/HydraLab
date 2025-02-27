// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.performance.InspectionStrategy;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ToString
public class TestTaskSpec {
    public String testTaskId;
    public String deviceIdentifier;
    public String groupTestType;
    public String groupDevices;
    public String accessKey;
    public String reportAudience;
    public String pkgName;
    public String testPkgName;
    public String type = "API";
    public String fileSetId;
    public TestFileSet testFileSet;
    public int testTimeOutSec = -1;
    public boolean isPerfTest;
    public boolean skipInstall = false;
    public boolean needUninstall = true;
    public boolean needClearData = true;
    // todo: remove this field when update overall center-ADO/Gradle plugins compatibility
    @Deprecated
    public Map<String, String> instrumentationArgs;
    public Map<String, String> testRunArgs;
    public Set<String> agentIds = new HashSet<>();
    public String runningType;
    public String testPlan;
    public int maxStepCount = 100;
    public int deviceTestCount = 1;
    public String pipelineLink;
    public int retryTime = 0;
    public String frameworkType;
    public List<String> neededPermissions;
    public String teamId;
    public String teamName;
    public String testRunnerName;
    public String testScope;
    public String testSuiteClass;
    public Map<String, List<DeviceAction>> deviceActions;
    public List<InspectionStrategy> inspectionStrategies;
    public List<AnalysisTask.AnalysisConfig> analysisConfigs;
    public boolean enablePerformanceSuggestion;
    public String notifyUrl;
    public boolean disableRecording = false;
    public boolean enableNetworkMonitor;
    public String networkMonitorRule;
    public boolean enableTestOrchestrator = false;
    public boolean blockDevice = false;
    public boolean unblockDevice = false;
    public String blockedDeviceSerialNumber;
    public String unblockedDeviceSerialNumber;
    public String unblockDeviceSecretKey;

    public void updateWithDefaultValues() {
        determineScopeOfTestCase();

        if (StringUtils.isBlank(runningType)) {
            runningType = Task.RunnerType.INSTRUMENTATION.name();
        }
        if (StringUtils.isBlank(testSuiteClass)) {
            testSuiteClass = pkgName;
        }
        if (enableNetworkMonitor && StringUtils.isBlank(networkMonitorRule)) {
            networkMonitorRule = pkgName;
        }
    }

    private void determineScopeOfTestCase() {
        if (!StringUtils.isEmpty(testScope)) {
            return;
        }
        testScope = TestTask.TestScope.CLASS;
        if (StringUtils.isEmpty(testSuiteClass)) {
            testScope = TestTask.TestScope.TEST_APP;
        }
    }
}
