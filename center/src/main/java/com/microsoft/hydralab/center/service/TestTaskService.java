// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.center.DeviceGroup;
import com.microsoft.hydralab.common.entity.center.TestTaskQueuedInfo;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
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
        if (deviceIdentifier.contains(",")) {
            for (String tempIdentifier : deviceIdentifier.split(",")) {
                relatedIdentifiers.add(tempIdentifier);
                relatedIdentifiers.addAll(deviceAgentManagementService.queryGroupByDevice(deviceIdentifier));
            }
        } else if (deviceIdentifier.startsWith(Const.DeviceGroup.GROUP_NAME_PREFIX)) {
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
                    testDataService.saveTestTaskData(testTask);
                    taskQueue.poll();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                //the task will be saved in memory if taskSpec is error
                testTask.setStatus(TestTask.TestStatus.EXCEPTION);
                testTask.setTestErrorMsg(e.getMessage());
                testDataService.saveTestTaskData(testTask);
                taskQueue.poll();
            }
        }
        isRunning.set(false);
    }

    public void cancelTask(String testTaskId) {
        synchronized (taskQueue) {
            Queue<TestTaskSpec> taskQueueCopy = getTestQueueCopy();
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

    public LinkedList<TestTaskSpec> getTestQueueCopy() {
        return new LinkedList<>(taskQueue);
    }

    public TestTaskQueuedInfo getTestQueuedInfo(String testTaskId) {
        TestTaskQueuedInfo taskQueuedInfo = new TestTaskQueuedInfo();
        // [index, retry time]
        int[] queuedInfo = new int[2];
        queuedInfo[0] = -1;
        queuedInfo[1] = 0;
        taskQueuedInfo.setQueuedInfo(queuedInfo);

        Queue<TestTaskSpec> taskQueueCopy = getTestQueueCopy();
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

    public void checkTestTaskTeamConsistency(TestTaskSpec testTaskSpec) throws HydraLabRuntimeException {
        if (TestTask.TestRunningType.APPIUM_CROSS.equals(testTaskSpec.runningType)
                || TestTask.TestRunningType.T2C_JSON_TEST.equals(testTaskSpec.runningType)) {
            String[] identifiers = testTaskSpec.deviceIdentifier.split(",");
            for (String identifier : identifiers) {
                checkDeviceTeamConsistency(identifier, testTaskSpec.teamId, testTaskSpec.accessKey);
            }
        } else {
            String deviceIdentifier = testTaskSpec.deviceIdentifier;
            if (deviceIdentifier.startsWith(Const.DeviceGroup.GROUP_NAME_PREFIX)) {
                DeviceGroup deviceGroup = deviceGroupService.getGroupByName(deviceIdentifier);
                if (deviceGroup == null) {
                    throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Didn't find DeviceGroup with given deviceIdentifier!");
                }
                if (testTaskSpec.teamId.equals(deviceGroup.getTeamId())) {
                    return;
                }
                if (!deviceGroup.getIsPrivate()) {
                    return;
                }
                deviceAgentManagementService.checkAccessInfo(deviceIdentifier, testTaskSpec.accessKey);
            } else {
                checkDeviceTeamConsistency(deviceIdentifier, testTaskSpec.teamId, testTaskSpec.accessKey);
            }
        }
    }

    private void checkDeviceTeamConsistency(String deviceIdentifier, String teamId, String accessKey) {
        DeviceInfo device = deviceAgentManagementService.getDevice(deviceIdentifier);
        if (device == null) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Didn't find device with given deviceIdentifier!");
        }
        AgentUser agent = agentManageService.getAgent(device.getAgentId());
        if (agent == null) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Didn't find AgentUser with given agent id!");
        }
        if (agent.getTeamId().equals(teamId)) {
            return;
        }
        if (!device.getIsPrivate()) {
            return;
        }
        deviceAgentManagementService.checkAccessInfo(deviceIdentifier, accessKey);
    }

    public void updateTaskTeam(String teamId, String teamName) {
        List<TestTask> testTasks = testDataService.getTasksByTeamId(teamId);

        testTasks.forEach(testTask -> testTask.setTeamName(teamName));
        testDataService.saveAllTestTasks(testTasks);
    }
}
