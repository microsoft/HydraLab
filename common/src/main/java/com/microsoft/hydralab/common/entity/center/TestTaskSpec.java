// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.TestFileSet;
import lombok.ToString;

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
    public String type;
    public String fileSetId;
    public TestFileSet testFileSet;
    public int testTimeOutSec = -1;
    public boolean isPerfTest;
    public boolean needUninstall = true;
    public boolean needClearData = true;
    public Map<String, String> instrumentationArgs;
    public Set<String> agentIds = new HashSet<>();
    public String runningType;
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
}
