// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.service;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.center.*;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestTask;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class TestTaskService {
    private static volatile AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Logger logger = LoggerFactory.getLogger(TestTaskService.class);
    private final Queue<TestTaskSpec> taskQueue = new LinkedList<>();
    @Resource
    DeviceAgentManagementService deviceAgentManagementService;
    @Resource
    AgentManageService agentManageService;
    @Resource
    DeviceGroupService deviceGroupService;
    @Resource
    TestDataService testDataService;

    public void addTask(TestTaskSpec task) {
        taskQueue.offer(task);
    }

    public Boolean isQueueEmpty() {
        return taskQueue.isEmpty();
    }

    public Boolean isDeviceFree(String deviceIdentifier) {
        Set<String> relatedIdentifiers = new HashSet<>();
        relatedIdentifiers.add(deviceIdentifier);
        if (deviceIdentifier.startsWith(Const.DeviceGroup.groupPre)) {
            relatedIdentifiers.addAll(deviceAgentManagementService.queryDeviceByGroup(deviceIdentifier));
        } else {
            relatedIdentifiers.addAll(deviceAgentManagementService.queryGroupByDevice(deviceIdentifier));
        }
        synchronized (taskQueue) {
            Queue<TestTaskSpec> taskQueueCopy = new LinkedList<>(taskQueue);
            while (!taskQueueCopy.isEmpty()) {
                TestTaskSpec temp = taskQueueCopy.poll();
                if (relatedIdentifiers.contains(temp.deviceIdentifier)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Scheduled(cron = "0 */3 * * * *")
    public void runTask() {
        //only run one task at the same time
        if (isRunning.get()) {
            return;
        }
        isRunning.set(true);
        while (!taskQueue.isEmpty()) {
            TestTaskSpec testTaskSpec = taskQueue.peek();
            TestTask testTask = TestTask.convertToTestTask(testTaskSpec);
            try {
                JSONObject result = deviceAgentManagementService.runTestTaskBySpec(testTaskSpec);
                if (result.get(Const.Param.TEST_DEVICE_SN) == null) {
                    break;
                } else {
                    testTask.setTestDevicesCount(result.getString(Const.Param.TEST_DEVICE_SN).split(",").length);
                    testDataService.saveTestTaskData(testTask, false);
                    taskQueue.poll();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                //the task will be saved in memory if taskSpec is error
                testTask.setStatus(TestTask.TestStatus.EXCEPTION);
                testTask.setTestErrorMsg(e.getMessage());
                testDataService.saveTestTaskData(testTask, false);
                taskQueue.poll();
            }
        }
        isRunning.set(false);
    }

    public void cancelTask(String testTaskId) {
        synchronized (taskQueue) {
            Queue<TestTaskSpec> taskQueueCopy = new LinkedList<>(taskQueue);
            TestTaskSpec tempTask = null;
            while (!taskQueueCopy.isEmpty()) {
                TestTaskSpec temp = taskQueueCopy.poll();
                if (testTaskId.equals(temp.testTaskId)) {
                    tempTask = temp;
                    break;
                }
            }
            if (tempTask != null) {
                taskQueue.remove(tempTask);
            }
        }
    }

    public TestTaskQueuedInfo getTestQueuedInfo(String testTaskId) {
        TestTaskQueuedInfo taskQueuedInfo = new TestTaskQueuedInfo();
        int[] queuedInfo = new int[2]; // [index, retry time]
        queuedInfo[0] = -1;
        queuedInfo[1] = 0;
        taskQueuedInfo.setQueuedInfo(queuedInfo);

        Queue<TestTaskSpec> taskQueueCopy = new LinkedList<>(taskQueue);
        int index = 1;
        while (!taskQueueCopy.isEmpty()) {
            TestTaskSpec temp = taskQueueCopy.poll();
            if (testTaskId.equals(temp.testTaskId)) {
                queuedInfo[0] = index;
                queuedInfo[1] = temp.retryTime;
                taskQueuedInfo.setTestTaskSpec(temp);
                break;
            }
            index++;
        }
        return taskQueuedInfo;
    }

    public boolean checkTestTaskTeamConsistency(TestTaskSpec testTaskSpec) {
        if (TestTask.TestRunningType.APPIUM_CROSS.equals(testTaskSpec.runningType)
                || TestTask.TestRunningType.T2C_JSON_TEST.equals(testTaskSpec.runningType)) {
            AgentUser agent = agentManageService.getAgent(testTaskSpec.deviceIdentifier);
            if (agent == null) {
                return false;
            }
            return testTaskSpec.teamId.equals(agent.getTeamId());
        } else {
            String deviceIdentifier = testTaskSpec.deviceIdentifier;
            if (deviceIdentifier.startsWith(Const.DeviceGroup.groupPre)) {
                DeviceGroup deviceGroup = deviceGroupService.getGroupByName(deviceIdentifier);
                if (deviceGroup == null) {
                    return false;
                }
                if (testTaskSpec.teamId.equals(deviceGroup.getTeamId())) {
                    return true;
                }
                if (!deviceGroup.getIsPrivate()) {
                    return true;
                }
                deviceAgentManagementService.checkAccessInfo(deviceIdentifier, testTaskSpec.accessKey);
                return true;
            } else {
                DeviceInfo device = deviceAgentManagementService.getDevice(deviceIdentifier);
                if (device == null) {
                    return false;
                }
                AgentUser agent = agentManageService.getAgent(device.getAgentId());
                if (agent == null) {
                    return false;
                }
                if (testTaskSpec.teamId.equals(agent.getTeamId())) {
                    return true;
                }
                if (!device.getIsPrivate()) {
                    return true;
                }
                deviceAgentManagementService.checkAccessInfo(deviceIdentifier, testTaskSpec.accessKey);
                return true;
            }
        }
    }

    public void updateTaskTeam(String teamId, String teamName) {
        List<TestTask> testTasksMem = testDataService.getTasksMemByTeamId(teamId);
        List<TestTask> testTasks = testDataService.getTasksByTeamId(teamId);

        testTasksMem.forEach(testTaskMem -> testTaskMem.setTeamName(teamName));

        testTasks.forEach(testTask -> testTask.setTeamName(teamName));
        testDataService.saveAllTestTasks(testTasks);
    }
}
