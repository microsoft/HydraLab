// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.performance.InspectionStrategy;
import lombok.ToString;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ToString
public class TestTaskSpec {
    @SuppressWarnings("VisibilityModifier")
    public String testTaskId;
    @SuppressWarnings("VisibilityModifier")
    public String deviceIdentifier;
    @SuppressWarnings("VisibilityModifier")
    public String groupTestType;
    @SuppressWarnings("VisibilityModifier")
    public String groupDevices;
    @SuppressWarnings("VisibilityModifier")
    public String accessKey;
    @SuppressWarnings("VisibilityModifier")
    public String reportAudience;
    @SuppressWarnings("VisibilityModifier")
    public String pkgName;
    @SuppressWarnings("VisibilityModifier")
    public String testPkgName;
    @SuppressWarnings("VisibilityModifier")
    public String type = "API";
    @SuppressWarnings("VisibilityModifier")
    public String fileSetId;
    @SuppressWarnings("VisibilityModifier")
    public TestFileSet testFileSet;
    @SuppressWarnings("VisibilityModifier")
    public int testTimeOutSec = -1;
    @SuppressWarnings("VisibilityModifier")
    public boolean isPerfTest;
    @SuppressWarnings("VisibilityModifier")
    public boolean needUninstall = true;
    @SuppressWarnings("VisibilityModifier")
    public boolean needClearData = true;
    // todo: remove this field when update overall center-ADO/Gradle plugins compatibility
    @Deprecated
    @SuppressWarnings("VisibilityModifier")
    public Map<String, String> instrumentationArgs;
    @SuppressWarnings("VisibilityModifier")
    public Map<String, String> testRunArgs;
    @SuppressWarnings("VisibilityModifier")
    public Set<String> agentIds = new HashSet<>();
    @SuppressWarnings("VisibilityModifier")
    public String runningType;
    @SuppressWarnings("VisibilityModifier")
    public int maxStepCount = 100;
    @SuppressWarnings("VisibilityModifier")
    public int deviceTestCount = 1;
    @SuppressWarnings("VisibilityModifier")
    public String pipelineLink;
    @SuppressWarnings("VisibilityModifier")
    public int retryTime = 0;
    @SuppressWarnings("VisibilityModifier")
    public String frameworkType;
    @SuppressWarnings("VisibilityModifier")
    public List<String> neededPermissions;
    @SuppressWarnings("VisibilityModifier")
    public String teamId;
    @SuppressWarnings("VisibilityModifier")
    public String teamName;
    @SuppressWarnings("VisibilityModifier")
    public String testRunnerName;
    @SuppressWarnings("VisibilityModifier")
    public String testScope;
    @SuppressWarnings("VisibilityModifier")
    public String testSuiteClass;
    @SuppressWarnings("VisibilityModifier")
    public Map<String, List<DeviceAction>> deviceActions;
    @SuppressWarnings("VisibilityModifier")
    public List<InspectionStrategy> inspectionStrategies;
}
