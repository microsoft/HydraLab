// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.performance.InspectionStrategy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class TestTask extends Task implements Serializable {
    @Transient
    private static final String defaultRunner = "androidx.test.runner.AndroidJUnitRunner";
    @Transient
    private List<String> neededPermissions;
    // more like test bundle after we support appium jar
    @Transient
    public transient File testAppFile;
    @Transient
    public transient List<File> testJsonFileList = new ArrayList<>();
    private int totalTestCount;
    private int totalFailCount;
    private Boolean skipInstall = false;
    @Column(name = "require_reinstall")
    private Boolean needUninstall = true;
    @Column(name = "require_clear_data")
    private Boolean needClearData = true;
    private String testPlan;
    private String testPkgName;
    @Transient
    private String groupDevices;
    @Transient
    private String groupTestType;
    @Transient
    private transient String title;
    @Transient
    private int maxStepCount;
    @Transient
    private int deviceTestCount;
    private String frameworkType;
    private transient String testRunnerName = defaultRunner;
    private String testScope;
    // todo: change this to a more general name for all scopes of ESPRESSO tests.
    private String testSuite;
    @Transient
    private List<InspectionStrategy> inspectionStrategies;
    @Transient
    private boolean enablePerformanceSuggestion;
    @Transient
    private boolean enableNetworkMonitor;
    @Transient
    private String networkMonitorRule;
    @Column(nullable = true)
    private boolean enableTestOrchestrator = false;

    public TestTask() {
        super();
    }

    public TestTask(TestTaskSpec testTaskSpec) {
        super(testTaskSpec);
        setTestSuite(testTaskSpec.testSuiteClass);
        if(StringUtils.isBlank(testTaskSpec.testSuiteClass)){
            setTestSuite(testTaskSpec.pkgName);
        }else{
            setTaskAlias(testTaskSpec.testSuiteClass);
        }
        setGroupDevices(testTaskSpec.groupDevices);
        setGroupTestType(testTaskSpec.groupTestType);
        setNeededPermissions(testTaskSpec.neededPermissions);
        setTestPkgName(testTaskSpec.testPkgName);
        setMaxStepCount(testTaskSpec.maxStepCount);
        setDeviceTestCount(testTaskSpec.deviceTestCount);
        setSkipInstall(testTaskSpec.skipInstall);
        setNeedUninstall(testTaskSpec.needUninstall);
        setNeedClearData(testTaskSpec.needClearData);
        if (StringUtils.isNotBlank(testTaskSpec.testPlan)) {
            setTestPlan(testTaskSpec.testPlan);
        }
        setEnableTestOrchestrator(testTaskSpec.enableTestOrchestrator);
        setFrameworkType(testTaskSpec.frameworkType);

        if (StringUtils.isNotBlank(testTaskSpec.testRunnerName)) {
            setTestRunnerName(testTaskSpec.testRunnerName);
        }
        setTestScope(testTaskSpec.testScope);
        setInspectionStrategies(testTaskSpec.inspectionStrategies);
        setEnablePerformanceSuggestion(testTaskSpec.enablePerformanceSuggestion);
        setEnableNetworkMonitor(testTaskSpec.enableNetworkMonitor);
        setNetworkMonitorRule(testTaskSpec.networkMonitorRule);
    }

    public TestTaskSpec convertToTaskSpec() {
        TestTaskSpec testTaskSpec = super.convertToTaskSpec();
        testTaskSpec.testSuiteClass = getTestSuite();
        testTaskSpec.groupTestType = getGroupTestType();
        testTaskSpec.testPkgName = getTestPkgName();
        testTaskSpec.skipInstall = getSkipInstall();
        testTaskSpec.needUninstall = getNeedUninstall();
        testTaskSpec.needClearData = getNeedClearData();
        testTaskSpec.neededPermissions = getNeededPermissions();
        testTaskSpec.testPlan = getTestPlan();
        testTaskSpec.maxStepCount = getMaxStepCount();
        testTaskSpec.deviceTestCount = getDeviceTestCount();
        testTaskSpec.testRunnerName = getTestRunnerName();
        testTaskSpec.testScope = getTestScope();
        testTaskSpec.inspectionStrategies = getInspectionStrategies();
        testTaskSpec.enablePerformanceSuggestion = isEnablePerformanceSuggestion();
        testTaskSpec.enableNetworkMonitor = isEnableNetworkMonitor();
        testTaskSpec.networkMonitorRule = getNetworkMonitorRule();
        testTaskSpec.enableTestOrchestrator = isEnableTestOrchestrator();

        return testTaskSpec;
    }

    public synchronized void addTestJsonFile(File jsonFile) {
        testJsonFileList.add(jsonFile);
    }

    @Transient
    public String getOverallSuccessRate() {
        if (totalTestCount == 0) {
            return "0%";
        }
        float rate = 100f * (totalTestCount - totalFailCount) / totalTestCount;
        return String.format("%.2f", rate) + '%';
    }

    @Override
    public void onFinished() {
        super.onFinished();
        if (getTaskRunList().isEmpty()) {
            return;
        }
        for (TestRun deviceTestResult : getTaskRunList()) {
            totalTestCount += deviceTestResult.getTotalCount();
            totalFailCount += deviceTestResult.getFailCount();
        }
    }

    /**
     * @return We currently assume if a permission name contains "android.", it's a system (app) defined Android permission. Return true if we should grant permission besides that.
     */
    public boolean shouldGrantCustomizedPermissions() {
        return false;
    }

    public interface TestFrameworkType {
        String JUNIT4 = "JUnit4";
        String JUNIT5 = "JUnit5";
    }

    public interface TestScope {
        String TEST_APP = "TEST_APP";
        String PACKAGE = "PACKAGE";
        String CLASS = "CLASS";
    }
}
