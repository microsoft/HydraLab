// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.microsoft.hydralab.common.util.DateUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
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

/**
 * @author zhoule
 * @date 11/23/2023
 */

@Data
@Entity
@Table(indexes = {
        @Index(name = "task_start_date_index", columnList = "start_date", unique = false),
        @Index(columnList = "team_id")})
@Inheritance(strategy = InheritanceType.JOINED)
public class Task implements Serializable {
    @Transient
    public transient File appFile;
    @Transient
    public Set<String> agentIds = new HashSet<>();
    @Id
    private String id = UUID.randomUUID().toString();
    private int deviceCount;
    private String taskReportPath;
    private String commitId;
    private String commitMsg;
    private String errorMsg;
    private String pipelineLink;
    private String triggerType = Task.TriggerType.API;
    private String runnerType = TestTask.RunnerType.INSTRUMENTATION.name();
    private String status = Task.TaskStatus.RUNNING;
    @Column(name = "start_date", nullable = false)
    private Date startDate = new Date();
    private Date endDate;
    private int timeOutSecond;
    private String pkgName;
    private String taskAlias;
    @Transient
    private String deviceIdentifier;
    @Transient
    private String accessKey;
    @Transient
    private transient Map<String, String> taskRunArgs;
    @Transient
    private List<TestRun> taskRunList = new ArrayList<>();
    @Transient
    private Map<String, List<DeviceAction>> deviceActions = new HashMap<>();
    private String fileSetId;
    @Transient
    private TestFileSet testFileSet;
    @Transient
    private File resourceDir;
    private int retryTime = 0;
    @Column(name = "team_id")
    private String teamId;
    private String teamName;
    @Transient
    private String notifyUrl;
    @Transient
    private boolean disableRecording = false;
    private boolean isSucceed = false;

    @Transient
    private List<TestRun> deviceTestResults;

    public synchronized void addTestedDeviceResult(TestRun deviceTestResult) {
        taskRunList.add(deviceTestResult);
    }

    @Transient
    public boolean isCanceled() {
        return Task.TaskStatus.CANCELED.equals(this);
    }

    @JSONField(serialize = false)
    @Transient
    public String getDisplayStartTime() {
        return DateUtil.format.format(startDate);
    }


    @JSONField(serialize = false)
    @Transient
    public String getDisplayEndTime() {
        if (endDate == null) {
            return "";
        }
        return DateUtil.format.format(endDate);
    }

    public void onFinished() {
        setEndDate(new Date());
    }

    public enum RunnerType {
        INSTRUMENTATION,
        APPIUM,
        APPIUM_CROSS,
        SMART,
        MONKEY,
        APPIUM_MONKEY,
        T2C_JSON,
        XCTEST,
        MAESTRO,
        PYTHON,
        APK_SCANNER {
            @Override
            public Task transferToTask(TestTaskSpec testTaskSpec) {
                return new AnalysisTask(testTaskSpec);
            }
        };

        public Task transferToTask(TestTaskSpec testTaskSpec) {
            return new TestTask(testTaskSpec);
        }
    }

    public TestTaskSpec convertToTaskSpec() {
        TestTaskSpec testTaskSpec = new TestTaskSpec();
        testTaskSpec.testTaskId = getId();
        testTaskSpec.accessKey = getAccessKey();
        testTaskSpec.fileSetId = getFileSetId();
        testTaskSpec.pkgName = getPkgName();
        testTaskSpec.type = getTriggerType();
        TestFileSet testFileSet = new TestFileSet();
        BeanUtil.copyProperties(getTestFileSet(), testFileSet);
        testTaskSpec.testFileSet = testFileSet;
        testTaskSpec.testTimeOutSec = getTimeOutSecond();
        testTaskSpec.deviceActions = getDeviceActions();
        testTaskSpec.testRunArgs = getTaskRunArgs();
        testTaskSpec.runningType = getRunnerType();
        testTaskSpec.deviceIdentifier = getDeviceIdentifier();
        testTaskSpec.pipelineLink = getPipelineLink();
        testTaskSpec.teamId = getTeamId();
        testTaskSpec.teamName = getTeamName();
        testTaskSpec.notifyUrl = getNotifyUrl();
        testTaskSpec.disableRecording = isDisableRecording();
        testTaskSpec.retryTime = getRetryTime();

        return testTaskSpec;
    }

    @SuppressWarnings("deprecation")
    public Task(TestTaskSpec testTaskSpec) {
        setId(testTaskSpec.testTaskId);
        setAccessKey(testTaskSpec.accessKey);
        setCommitId(testTaskSpec.testFileSet.getCommitId());
        setCommitMsg(testTaskSpec.testFileSet.getCommitMessage());
        setPipelineLink(testTaskSpec.pipelineLink);
        setTimeOutSecond(testTaskSpec.testTimeOutSec);
        setDeviceActions(testTaskSpec.deviceActions);
        if (testTaskSpec.instrumentationArgs != null) {
            testTaskSpec.testRunArgs.putAll(testTaskSpec.instrumentationArgs);
        }
        setTaskRunArgs(testTaskSpec.testRunArgs);
        setFileSetId(testTaskSpec.fileSetId);
        setPkgName(testTaskSpec.pkgName);
        setTaskAlias(testTaskSpec.pkgName);
        TestFileSet testFileSet = new TestFileSet();
        BeanUtil.copyProperties(testTaskSpec.testFileSet, testFileSet);
        setTestFileSet(testFileSet);
        setDeviceIdentifier(testTaskSpec.deviceIdentifier);
        if (StringUtils.isNotBlank(testTaskSpec.type)) {
            setTriggerType(testTaskSpec.type);
        }
        agentIds = testTaskSpec.agentIds;
        if (StringUtils.isNotBlank(testTaskSpec.runningType)) {
            setRunnerType(testTaskSpec.runningType);
        } else {
            setRunnerType(RunnerType.INSTRUMENTATION.name());
        }

        setRetryTime(testTaskSpec.retryTime);
        setTeamId(testTaskSpec.teamId);
        setTeamName(testTaskSpec.teamName);
        setNotifyUrl(testTaskSpec.notifyUrl);
        setDisableRecording(testTaskSpec.disableRecording);
    }

    public Task() {

    }

    public interface TaskStatus {
        String RUNNING = "running";
        String FINISHED = "finished";
        String CANCELED = "canceled";
        String EXCEPTION = "error";
        String WAITING = "waiting";
    }

    public interface TriggerType {
        String PR = "PullRequest";
        String API = "API";
        String Schedule = "Schedule";
    }

    @Deprecated
    public int getTestDevicesCount() {
        return deviceCount;
    }

    @Deprecated
    public String getTestErrorMsg() {
        return errorMsg;
    }
}
