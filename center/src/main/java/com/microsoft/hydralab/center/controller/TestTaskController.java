// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.TestDataService;
import com.microsoft.hydralab.center.service.TestFileSetService;
import com.microsoft.hydralab.center.service.TestTaskService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.center.TestTaskQueuedInfo;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.TestFileSet;
import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

@RestController
@RequestMapping
public class TestTaskController {
    private final Logger logger = LoggerFactory.getLogger(TestTaskController.class);
    @Resource
    DeviceAgentManagementService deviceAgentManagementService;
    @Resource
    TestDataService testDataService;
    @Resource
    TestTaskService testTaskService;
    @Resource
    TestFileSetService testFileSetService;
    @Resource
    SysUserService sysUserService;
    @Resource
    private UserTeamManagementService userTeamManagementService;

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) TestTaskSpec and related resources (Agent/Device/Group) should have a uniform same teamId as user
     */
    @PostMapping(value = {Const.Path.TEST_TASK_RUN}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Object> runTestTask(@CurrentSecurityContext SysUser requestor,
                                      @RequestBody TestTaskSpec testTaskSpec) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            JSONObject result = new JSONObject();
            TestFileSet testFileSet = testFileSetService.getFileSetInfo(testTaskSpec.fileSetId);
            if (testFileSet == null) {
                return Result.error(HttpStatus.NOT_FOUND.value(), "no such test file set");
            }
            testTaskSpec.testFileSet = testFileSet;
            testTaskSpec.teamId = testFileSet.getTeamId();
            testTaskSpec.teamName = testFileSet.getTeamName();
            testTaskSpec.testTaskId = UUID.randomUUID().toString();
            if (!sysUserService.checkUserAdmin(requestor)) {
                if (!userTeamManagementService.checkRequestorTeamRelation(requestor, testTaskSpec.teamId)) {
                    return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestFileSet doesn't belong to user's Teams");
                }
                testTaskService.checkTestTaskTeamConsistency(testTaskSpec);
            }
            //if the queue is not empty, the task will be added to the queue directly
            if (testTaskService.isQueueEmpty()
                    || Task.RunnerType.APK_SCANNER.name().equals(testTaskSpec.runningType)
                    || deviceAgentManagementService.isRunOnBlockedDevice(testTaskSpec) || testTaskService.isDeviceFree(testTaskSpec.deviceIdentifier)) {
                result = deviceAgentManagementService.runTestTaskBySpec(testTaskSpec);
                if (result.get(Const.Param.TEST_DEVICE_SN) == null) {
                    //if there is no alive device, the task will be added to the queue directly
                    testTaskService.addTask(testTaskSpec);
                } else {
                    Task task = Task.RunnerType.valueOf(testTaskSpec.runningType).transferToTask(testTaskSpec);
                    task.setDeviceCount(result.getString(Const.Param.TEST_DEVICE_SN).split(",").length);
                    testDataService.saveTaskData(task);
                }
            } else {
                testTaskService.addTask(testTaskSpec);
            }
            if (result.get(Const.Param.TEST_DEVICE_SN) == null) {
                result.put("message", "Device is under testing, test task has been added to a queue, please wait for a minute!");
            }
            result.put(Const.Param.TEST_TASK_ID, testTaskSpec.testTaskId);
            return Result.ok(result);
        } catch (HydraLabRuntimeException e) {
            return Result.error(e.getCode(), e);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.BAD_REQUEST.value(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that TestTask is in
     */
    @GetMapping(value = {"/api/test/task/{testId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Object> getTaskStatus(@CurrentSecurityContext SysUser requestor,
                                        @PathVariable("testId") String testId) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }
            Task task = testDataService.getTaskDetail(testId);
            if (task != null) {
//                if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, testTask.getTeamId())) {
//                    return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestTask doesn't belong to user's Teams");
//                }
                task.setDeviceTestResults(task.getTaskRunList());
                return Result.ok(task);
            }
            TestTaskQueuedInfo queuedInfo = testTaskService.getTestQueuedInfo(testId);
            TestTaskSpec queuedTaskSpec = queuedInfo.getTestTaskSpec();
            if (queuedTaskSpec == null) {
                return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "The testId is error!");
            }
            if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, queuedTaskSpec.teamId)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestTask doesn't belong to user's Teams");
            }

            JSONObject result = new JSONObject();
            result.put("message", "Current position in queue: " + queuedInfo.getQueuedInfo()[0]);
            result.put("status", Task.TaskStatus.WAITING);
            result.put("retryTime", queuedInfo.getQueuedInfo()[1]);
            return Result.ok(result);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER: all
     * Data access:
     * 1) For users with ROLE SUPER_ADMIN/ADMIN, return all data.
     * 2) For the rest users, return data that is in the user's TEAMs
     */
    @GetMapping(value = {"/api/test/task/queue"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<TestTaskQueuedInfo>> getTaskQueue(@CurrentSecurityContext SysUser requestor) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }
            boolean isAdmin = sysUserService.checkUserAdmin(requestor);
            List<TestTaskQueuedInfo> result = new ArrayList<>();
            Queue<TestTaskSpec> taskQueueCopy = testTaskService.getTestQueueCopy();
            int index = 0;
            while (!taskQueueCopy.isEmpty()) {
                index++;
                TestTaskSpec temp = taskQueueCopy.poll();
                if (!isAdmin && !requestor.getTeamAdminMap().keySet().contains(temp.teamId)) {
                    continue;
                }
                TestTaskQueuedInfo taskQueuedInfo = new TestTaskQueuedInfo();
                int[] queuedInfo = new int[]{
                        index, temp.retryTime
                };
                taskQueuedInfo.setQueuedInfo(queuedInfo);
                taskQueuedInfo.setTestTaskSpec(temp);
                result.add(taskQueuedInfo);
            }
            return Result.ok(result);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER: all
     * Data access:
     * 1) For users with ROLE SUPER_ADMIN/ADMIN, return all data.
     * 2) For the rest users, return data that is in the user's TEAMs
     */
    @PostMapping(value = {"/api/test/task/list"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Page<Task>> getTaskList(@CurrentSecurityContext SysUser requestor,
                                              @RequestBody JSONObject data) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            List<CriteriaType> criteriaTypes = new ArrayList<>();
            // filter all TestTask in TEAMs that user is in
            if (!sysUserService.checkUserAdmin(requestor)) {
                criteriaTypes = userTeamManagementService.formTeamIdCriteria(requestor.getTeamAdminMap());
                if (criteriaTypes.size() == 0) {
                    return Result.error(HttpStatus.UNAUTHORIZED.value(), "User belongs to no TEAM, please contact administrator for binding TEAM");
                }
            }

            int page = data.getIntValue("page");
            int pageSize = data.getIntValue("pageSize");
            if (pageSize <= 0) {
                pageSize = 30;
            }
            JSONArray queryParams = data.getJSONArray("queryParams");
            if (queryParams != null) {
                for (int i = 0; i < queryParams.size(); i++) {
                    CriteriaType temp = queryParams.getJSONObject(i).toJavaObject(CriteriaType.class);
                    criteriaTypes.add(temp);
                }
            }

            return Result.ok(testDataService.getTasks(page, pageSize, criteriaTypes));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    @PostMapping(value = {"/api/test/task/listFirst"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Task> getTaskListFirst(@CurrentSecurityContext SysUser requestor,
                                         @RequestBody JSONObject data) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            List<CriteriaType> criteriaTypes = new ArrayList<>();
            // filter all TestTask in TEAMs that user is in
            if (!sysUserService.checkUserAdmin(requestor)) {
                criteriaTypes = userTeamManagementService.formTeamIdCriteria(requestor.getTeamAdminMap());
                if (criteriaTypes.size() == 0) {
                    return Result.error(HttpStatus.UNAUTHORIZED.value(), "User belongs to no TEAM, please contact administrator for binding TEAM");
                }
            }

            int page = data.getIntValue("page");
            int pageSize = data.getIntValue("pageSize");
            if (pageSize <= 0) {
                pageSize = 30;
            }
            JSONArray queryParams = data.getJSONArray("queryParams");
            if (queryParams != null) {
                for (int i = 0; i < queryParams.size(); i++) {
                    CriteriaType temp = queryParams.getJSONObject(i).toJavaObject(CriteriaType.class);
                    criteriaTypes.add(temp);
                }
            }
            Page<Task> tasks = testDataService.getTasks(page, pageSize, criteriaTypes);
            if (tasks.getTotalElements() > 0) {
                Task task = testDataService.getTaskDetail(tasks.getContent().get(0).getId());
                return Result.ok(task);
            } else {
                return Result.ok(null);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER: all
     * Data access:
     * 1) For users with ROLE SUPER_ADMIN/ADMIN, return all data.
     * 2) For the rest users, return data that is in the user's TEAMs
     */
    @PostMapping(value = {"/api/test/task/listSuite"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List> getTaskSuiteList(@CurrentSecurityContext SysUser requestor,
                                         @RequestBody List<CriteriaType> criteriaTypes) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            // filter all Task Suite in TEAMs that user is in
            if (!sysUserService.checkUserAdmin(requestor)) {
                List<CriteriaType> criteriaTypeList = userTeamManagementService.formTeamIdCriteria(requestor.getTeamAdminMap());
                if (criteriaTypeList.size() == 0) {
                    return Result.error(HttpStatus.UNAUTHORIZED.value(), "User belongs to no TEAM, please contact administrator for binding TEAM");
                }
            }

            return Result.ok(testDataService.getTestTaskSuites(criteriaTypes));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that the running/queued TestTask is in
     */
    @GetMapping(value = {"/api/test/task/cancel/{testId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Object> cancelTask(@CurrentSecurityContext SysUser requestor,
                                     @PathVariable("testId") String testId,
                                     @RequestParam("reason") String reason) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            if (!LogUtils.isLegalStr(testId, Const.RegexString.UUID, false)) {
                return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error param! Should be UUID");
            }
            TestTaskQueuedInfo queuedInfo = testTaskService.getTestQueuedInfo(testId);
            TestTaskSpec queuedTaskSpec = queuedInfo.getTestTaskSpec();
            if (queuedTaskSpec != null) {
                if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, queuedTaskSpec.teamId)) {
                    return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestTask doesn't belong to user's Teams");
                }

                testTaskService.cancelTask(testId);
            } else {
                Task task = testDataService.getTaskDetail(testId);
                if (task == null) {
                    return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "The testId is wrong!");
                } else if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, task.getTeamId())) {
                    return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestTask doesn't belong to user's Teams");
                }

                deviceAgentManagementService.cancelTestTaskById(testId, reason);
                //run the task saved in queue
                testTaskService.runTask();
            }
            if (!LogUtils.isLegalStr(testId, Const.RegexString.UUID, false)) {
                logger.warn("test {} is canceled", testId);// CodeQL [java/log-injection] False Positive: Has verified the string by regular expression
            }
        } catch (Exception e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
        return Result.ok();
    }
}
