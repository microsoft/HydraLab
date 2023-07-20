// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.microsoft.hydralab.common.util.DateUtil;
import com.microsoft.hydralab.performance.InspectionStrategy;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Entity
@Table(indexes = {
        @Index(name = "start_date_index", columnList = "start_date", unique = false),
        @Index(columnList = "team_id")})
public class TestTask implements Serializable {
    static final Pattern pIdMatch = Pattern.compile("\\d{3,7}");
    @Transient
    private static final String defaultRunner = "androidx.test.runner.AndroidJUnitRunner";
    @Transient
    private List<String> neededPermissions;
    @Transient
    public transient File appFile;
    // more like test bundle after we support appium jar
    @Transient
    public transient File testAppFile;
    @Transient
    public transient List<File> testJsonFileList = new ArrayList<>();
    @Transient
    public Set<String> agentIds = new HashSet<>();
    @Id
    private String id = UUID.randomUUID().toString();
    private int testDevicesCount;
    private int totalTestCount;
    private int totalFailCount;
    private String testTaskReportPath;
    private String testCommitId;
    private String testCommitMsg;
    private String testErrorMsg;
    private String pipelineLink;
    private Boolean skipInstall = false;
    @Column(name = "require_reinstall")
    private Boolean needUninstall = true;
    @Column(name = "require_clear_data")
    private Boolean needClearData = true;
    private String type = TestType.API;
    private String runningType = TestRunningType.INSTRUMENTATION;
    private String status = TestStatus.RUNNING;
    @Column(name = "start_date", nullable = false)
    private Date startDate = new Date();
    private Date endDate;
    private int timeOutSecond;
    private String pkgName;
    private String testPkgName;
    @Column(nullable = true)
    private String reportImagePath;
    @Transient
    private String deviceIdentifier;
    @Transient
    private String groupDevices;
    @Transient
    private String groupTestType;
    @Transient
    private String accessKey;
    @Transient
    private transient String title;
    @Transient
    private transient Map<String, String> instrumentationArgs;
    @Transient
    private List<TestRun> deviceTestResults = new ArrayList<>();
    @Transient
    private Map<String, List<DeviceAction>> deviceActions = new HashMap<>();
    private String fileSetId;
    @Transient
    private TestFileSet testFileSet;
    @Transient
    private File resourceDir;
    @Transient
    private int maxStepCount;
    @Transient
    private int deviceTestCount;
    @Transient
    private int retryTime = 0;
    private String frameworkType;
    @Column(name = "team_id")
    private String teamId;
    private String teamName;
    private transient String testRunnerName = defaultRunner;
    private String testScope;
    // todo: change this to a more general name for all scopes of ESPRESSO tests.
    private String testSuite;
    @Transient
    private List<InspectionStrategy> inspectionStrategies;
    @Transient
    private String notifyUrl;
    @Transient
    private boolean disableRecording = false;
    @Transient
    private boolean enableNetworkMonitor;
    @Transient
    private String networkMonitorRule;

    public TestTask() {
    }

    @SuppressWarnings("deprecation")
    public static TestTask convertToTestTask(TestTaskSpec testTaskSpec) {
        TestTask testTask = new TestTask();
        testTask.setId(testTaskSpec.testTaskId);
        testTask.setTestSuite(testTaskSpec.testSuiteClass);
        testTask.setDeviceIdentifier(testTaskSpec.deviceIdentifier);
        testTask.setGroupDevices(testTaskSpec.groupDevices);
        testTask.setGroupTestType(testTaskSpec.groupTestType);
        testTask.setAccessKey(testTaskSpec.accessKey);
        testTask.setTestCommitId(testTaskSpec.testFileSet.getCommitId());
        testTask.setTestCommitMsg(testTaskSpec.testFileSet.getCommitMessage());
        testTask.setPipelineLink(testTaskSpec.pipelineLink);
        testTask.setTimeOutSecond(testTaskSpec.testTimeOutSec);
        testTask.setNeededPermissions(testTaskSpec.neededPermissions);
        testTask.setDeviceActions(testTaskSpec.deviceActions);
        if (testTaskSpec.instrumentationArgs != null) {
            testTask.setInstrumentationArgs(testTaskSpec.instrumentationArgs);
        } else {
            testTask.setInstrumentationArgs(testTaskSpec.testRunArgs);
        }
        testTask.setFileSetId(testTaskSpec.fileSetId);
        testTask.setPkgName(testTaskSpec.pkgName);
        testTask.setTestPkgName(testTaskSpec.testPkgName);
        testTask.setMaxStepCount(testTaskSpec.maxStepCount);
        testTask.setDeviceTestCount(testTaskSpec.deviceTestCount);
        TestFileSet testFileSet = new TestFileSet();
        BeanUtil.copyProperties(testTaskSpec.testFileSet, testFileSet);
        testTask.setTestFileSet(testFileSet);
        testTask.setSkipInstall(testTaskSpec.skipInstall);
        testTask.setNeedUninstall(testTaskSpec.needUninstall);
        testTask.setNeedClearData(testTaskSpec.needClearData);
        if (StringUtils.isNotBlank(testTaskSpec.type)) {
            testTask.setType(testTaskSpec.type);
        }
        testTask.agentIds = testTaskSpec.agentIds;
        if (StringUtils.isNotBlank(testTaskSpec.runningType)) {
            testTask.setRunningType(testTaskSpec.runningType);
        }
        testTask.setRetryTime(testTaskSpec.retryTime);
        testTask.setFrameworkType(testTaskSpec.frameworkType);
        testTask.setTeamId(testTaskSpec.teamId);
        testTask.setTeamName(testTaskSpec.teamName);
        if (StringUtils.isNotBlank(testTaskSpec.testRunnerName)) {
            testTask.setTestRunnerName(testTaskSpec.testRunnerName);
        }
        testTask.setTestScope(testTaskSpec.testScope);
        testTask.setInspectionStrategies(testTaskSpec.inspectionStrategies);
        testTask.setNotifyUrl(testTaskSpec.notifyUrl);
        testTask.setDisableRecording(testTaskSpec.disableRecording);
        testTask.setEnableNetworkMonitor(testTaskSpec.enableNetworkMonitor);
        testTask.setNetworkMonitorRule(testTaskSpec.networkMonitorRule);

        return testTask;
    }

    @SuppressWarnings("deprecation")
    public static TestTaskSpec convertToTestTaskSpec(TestTask testTask) {
        TestTaskSpec testTaskSpec = new TestTaskSpec();
        testTaskSpec.testTaskId = testTask.getId();
        testTaskSpec.testSuiteClass = testTask.getTestSuite();
        testTaskSpec.deviceIdentifier = testTask.getDeviceIdentifier();
        testTaskSpec.groupTestType = testTask.getGroupTestType();
        testTaskSpec.accessKey = testTask.getAccessKey();
        testTaskSpec.fileSetId = testTask.getFileSetId();
        testTaskSpec.pkgName = testTask.getPkgName();
        testTaskSpec.testPkgName = testTask.getTestPkgName();
        testTaskSpec.type = testTask.getType();
        TestFileSet testFileSet = new TestFileSet();
        BeanUtil.copyProperties(testTask.getTestFileSet(), testFileSet);
        testTaskSpec.testFileSet = testFileSet;
        testTaskSpec.testTimeOutSec = testTask.getTimeOutSecond();
        testTaskSpec.skipInstall = testTask.getSkipInstall();
        testTaskSpec.needUninstall = testTask.getNeedUninstall();
        testTaskSpec.needClearData = testTask.getNeedClearData();
        testTaskSpec.neededPermissions = testTask.getNeededPermissions();
        testTaskSpec.deviceActions = testTask.getDeviceActions();
        testTaskSpec.instrumentationArgs = testTask.getInstrumentationArgs();
        testTaskSpec.runningType = testTask.getRunningType();
        testTaskSpec.maxStepCount = testTask.getMaxStepCount();
        testTaskSpec.deviceTestCount = testTask.getDeviceTestCount();
        testTaskSpec.pipelineLink = testTask.getPipelineLink();
        testTaskSpec.teamId = testTask.getTeamId();
        testTaskSpec.teamName = testTask.getTeamName();
        testTaskSpec.testRunnerName = testTask.getTestRunnerName();
        testTaskSpec.testScope = testTask.getTestScope();
        testTaskSpec.inspectionStrategies = testTask.getInspectionStrategies();
        testTaskSpec.notifyUrl = testTask.getNotifyUrl();
        testTaskSpec.disableRecording = testTask.isDisableRecording();
        testTaskSpec.enableNetworkMonitor = testTask.isEnableNetworkMonitor();
        testTaskSpec.networkMonitorRule = testTask.getNetworkMonitorRule();

        return testTaskSpec;
    }

    public static TestTask createEmptyTask() {
        TestTask testTask = new TestTask();
        testTask.setId(null);
        testTask.setType(null);
        testTask.setStartDate(null);
        testTask.setStatus(null);
        testTask.setNeedUninstall(null);
        testTask.setNeedClearData(null);

        return testTask;
    }

    public synchronized void addTestedDeviceResult(TestRun deviceTestResult) {
        deviceTestResults.add(deviceTestResult);
    }

    public synchronized void addTestJsonFile(File jsonFile) {
        testJsonFileList.add(jsonFile);
    }

    @Transient
    public boolean isCanceled() {
        return TestStatus.CANCELED.equals(status);
    }

    @JSONField(serialize = false)
    @Transient
    public String getDisplayStartTime() {
        return DateUtil.format.format(startDate);
    }

    @Transient
    public String getPullRequestId() {
        if (!TestType.PR.equals(type)) {
            return null;
        }
        if (StringUtils.isBlank(testCommitMsg)) {
            return null;
        }
        String msg = testCommitMsg.toLowerCase();
        if (!msg.startsWith("merge pull request ")) {
            return null;
        }
        Matcher matcher = pIdMatch.matcher(msg);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group();
    }

    @JSONField(serialize = false)
    @Transient
    public String getDisplayEndTime() {
        if (endDate == null) {
            return "";
        }
        return DateUtil.format.format(endDate);
    }

    @Transient
    public String getOverallSuccessRate() {
        if (totalTestCount == 0) {
            return "0%";
        }
        float rate = 100f * (totalTestCount - totalFailCount) / totalTestCount;
        return String.format("%.2f", rate) + '%';
    }

    public void onFinished() {
        endDate = new Date();
        if (deviceTestResults.isEmpty()) {
            return;
        }
        for (TestRun deviceTestResult : deviceTestResults) {
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

    public interface TestStatus {
        String RUNNING = "running";
        String FINISHED = "finished";
        String CANCELED = "canceled";
        String EXCEPTION = "error";
        String WAITING = "waiting";
    }

    public interface TestType {
        String PR = "PullRequest";
        String API = "API";
        String Schedule = "Schedule";
    }

    public interface TestRunningType {
        String INSTRUMENTATION = "INSTRUMENTATION";
        String APPIUM = "APPIUM";
        String APPIUM_CROSS = "APPIUM_CROSS";
        String SMART_TEST = "SMART";
        String MONKEY_TEST = "MONKEY";
        String APPIUM_MONKEY_TEST = "APPIUM_MONKEY";
        String T2C_JSON_TEST = "T2C_JSON";
        String XCTEST = "XCTEST";
        String MAESTRO = "MAESTRO";
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
