// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.service;

import com.alibaba.fastjson.JSONArray;
import com.microsoft.hydralab.center.repository.StabilityDataRepository;
import com.microsoft.hydralab.common.entity.center.StabilityData;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.*;
import com.microsoft.hydralab.common.repository.*;
import com.microsoft.hydralab.common.util.AttachmentService;
import com.microsoft.hydralab.common.util.CriteriaTypeUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TestDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataService.class);
    private final Sort sortByStartMillis = Sort.by(Sort.Direction.DESC, "startTimeMillis");
    private final Sort sortByStartDate = Sort.by(Sort.Direction.DESC, "startDate");
    @Resource
    TestTaskRepository testTaskRepository;
    @Resource
    AndroidTestUnitRepository androidTestUnitRepository;
    @Resource
    DeviceTestResultRepository deviceTestResultRepository;
    @Resource
    DeviceActionRepository deviceActionRepository;
    @Resource
    KeyValueRepository keyValueRepository;
    @Resource
    StabilityDataRepository stabilityDataRepository;
    @Resource
    AttachmentService attachmentService;
    @Resource
    SysUserService sysUserService;
    @Resource
    UserTeamManagementService userTeamManagementService;
    @Resource
    EntityManager entityManager;

    public List<AndroidTestUnit> getAllTestUnit(int page, int size) {
        List<AndroidTestUnit> testUnits = androidTestUnitRepository.findBySuccess(false, PageRequest.of(page, size, sortByStartMillis)).getContent();

        for (AndroidTestUnit testUnit : testUnits) {
            Optional<DeviceTestTask> deviceTestTask = deviceTestResultRepository.findById(testUnit.getDeviceTestResultId());
            deviceTestTask.ifPresent(testUnit::setDeviceTestTask);
        }

        return testUnits;
    }

    public List<StabilityData> getFailTest(int page, int size) {
        List<StabilityData> testUnits = stabilityDataRepository.findBySuccess(false, PageRequest.of(page, size, sortByStartMillis)).getContent();
        return testUnits;
    }

    public TestTask getTestTaskDetail(String testId) {
        TestTask testTask = keyValueRepository.getTestTaskMem(testId);
        if (testTask != null) {
            return testTask;
        }

        Optional<TestTask> taskOpt = testTaskRepository.findById(testId);
        if (taskOpt.isEmpty()) {
            return null;
        }
        testTask = taskOpt.get();
        List<DeviceTestTask> byTestTaskId = deviceTestResultRepository.findByTestTaskId(testId);

        if (byTestTaskId == null || byTestTaskId.isEmpty()) {
            return testTask;
        }

        testTask.getDeviceTestResults().addAll(byTestTaskId);
        for (DeviceTestTask deviceTestResult : byTestTaskId) {
            List<AndroidTestUnit> byDeviceTestResultId = androidTestUnitRepository.findByDeviceTestResultId(deviceTestResult.getId());
            deviceTestResult.getTestUnitList().addAll(byDeviceTestResultId);
            deviceTestResult.setAttachments(attachmentService.getAttachments(deviceTestResult.getId(), EntityFileRelation.EntityType.TEST_RESULT));
        }
        return testTask;
    }

    public List<TestTask> getTasksByTeamId(String teamId) {
        return testTaskRepository.findAllByTeamId(teamId);
    }

    public List<TestTask> getTasksMemByTeamId(String teamId) {
        return keyValueRepository.getTestTasksMemByTeamId(teamId);
    }

    public List<TestTask> getRunningTestTaskDetailsByTeam(Set<String> teamIds) {
        List<TestTask> testTasks = keyValueRepository.getRunningTestTasksMem();

        if (!CollectionUtils.isEmpty(teamIds)) {
            testTasks = testTasks.stream().filter(task -> teamIds.contains(task.getTeamId())).collect(Collectors.toList());
        }

        if (testTasks.isEmpty()) {
            return null;
        }
        return testTasks;
    }

    public void saveAllTestTasks(List<TestTask> testTasks) {
        testTaskRepository.saveAll(testTasks);
    }

    public Page<TestTask> getTestTasks(int page, int pageSize, List<CriteriaType> queryParams) {
        Specification<TestTask> spec = null;
        if (queryParams != null && queryParams.size() > 0) {
            spec = new CriteriaTypeUtil<TestTask>().transferToSpecification(queryParams, false);
        }

        Page<TestTask> pageObj = testTaskRepository.findAll(spec, PageRequest.of(page, pageSize, sortByStartDate));
        return pageObj;
    }

    public List<TestTask> getTestTaskSuites(List<CriteriaType> queryParams) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TestTask> query = criteriaBuilder.createQuery(TestTask.class);
        Root<TestTask> root = query.from(TestTask.class);

        if (queryParams != null && queryParams.size() > 0) {
            query.where(new CriteriaTypeUtil<TestTask>().transferToPredicate(root, criteriaBuilder, queryParams));
        }

        query.select(root.get("testSuite")).distinct(true);
        query.orderBy(criteriaBuilder.asc(root.get("testSuite")));

        List<TestTask> result = entityManager.createQuery(query).getResultList();
        return result;
    }

    public Set<String> cancelTaskById(String taskId, boolean isSaveDB) {
        TestTask testTaskMem = keyValueRepository.getTestTaskMem(taskId);
        if (isSaveDB) {
            testTaskMem.setStatus(TestTask.TestStatus.CANCELED);
            keyValueRepository.saveTestTask(testTaskMem);
            saveTestTaskData(testTaskMem, true);
        } else {
            keyValueRepository.deleteTestTaskById(taskId);
        }
        return testTaskMem.getAgentIds();
    }

    public void saveTestTaskDataFromAgent(TestTask testTask, boolean persistence, String agentId) {
        TestTask testTaskMem = keyValueRepository.getTestTaskMem(testTask.getId());
        //run by device
        if (testTaskMem.agentIds.size() == 0) {
            saveTestTaskData(testTask, persistence);
            return;
        }
        //run by group
        if (!persistence) {
            return;
        }

        testTaskMem.getDeviceTestResults().addAll(testTask.getDeviceTestResults());
        testTaskMem.setTotalTestCount(testTaskMem.getTotalTestCount() + testTask.getTotalTestCount());
        testTaskMem.setTotalFailCount(testTaskMem.getTotalFailCount() + testTask.getTotalFailCount());
        testTaskMem.setTestSuite(testTask.getTestSuite());
        testTaskMem.agentIds.remove(agentId);

        boolean isAllFinish = testTaskMem.agentIds.size() == 0;
        if (isAllFinish) {
            testTaskMem.setStatus(TestTask.TestStatus.FINISHED);
            testTaskMem.setEndDate(testTask.getEndDate());
        }
        saveTestTaskData(testTaskMem, isAllFinish);
    }

    public void saveTestTaskData(TestTask testTask, boolean persistence) {
        keyValueRepository.saveTestTask(testTask);

        if (!persistence) {
            return;
        }

        testTaskRepository.save(testTask);
        List<DeviceTestTask> deviceTestResults = testTask.getDeviceTestResults();
        if (deviceTestResults.isEmpty()) {
            return;
        }

        deviceTestResultRepository.saveAll(deviceTestResults);

        List<DeviceAction> deviceActions = testTask.getDeviceActions();
        if (deviceActions != null && !deviceActions.isEmpty()) {
            deviceActions.forEach(deviceAction -> deviceAction.setTestTaskId(testTask.getId()));
            deviceActionRepository.saveAll(deviceActions);
        }

        List<AndroidTestUnit> list = new ArrayList<>();
        for (DeviceTestTask deviceTestResult : deviceTestResults) {
            attachmentService.saveAttachments(deviceTestResult.getId(), EntityFileRelation.EntityType.TEST_RESULT, deviceTestResult.getAttachments());

            List<AndroidTestUnit> testUnitList = deviceTestResult.getTestUnitList();
            list.addAll(testUnitList);

            // only save failed cases
            for (AndroidTestUnit androidTestUnit : testUnitList) {
                if (androidTestUnit.isSuccess()) {
                    continue;
                }
                LOGGER.warn("one more failed cases saved: {}", androidTestUnit.getTitle());
                try {
                    keyValueRepository.saveAndroidTestUnit(androidTestUnit);
                } catch (Exception ignore) {

                }
            }

            keyValueRepository.saveDeviceTestResultResInfo(deviceTestResult);

            String crashStack = deviceTestResult.getCrashStack();
            if (crashStack == null) {
                continue;
            }
            keyValueRepository.saveCrashStack(deviceTestResult.getCrashStackId(), deviceTestResult.getCrashStack());
        }
        androidTestUnitRepository.saveAll(list);
        LOGGER.info("All saved {}", testTask.getId());
    }

    public DeviceTestTask getDeviceTestTaskWithVideoInfo(String dttId) {
        DeviceTestTask deviceTestTask = deviceTestResultRepository.getOne(dttId);
        JSONArray deviceTestResInfo = keyValueRepository.getDeviceTestResInfo(dttId);
        deviceTestTask.setVideoTimeTagArr(deviceTestResInfo);
        deviceTestTask.setVideoBlobUrl();
        deviceTestTask.setAttachments(attachmentService.getAttachments(dttId, EntityFileRelation.EntityType.TEST_RESULT));
        return deviceTestTask;
    }

    public DeviceTestTask getDeviceTestTaskByCrashId(String crashId) {
        return deviceTestResultRepository.findByCrashStackId(crashId).orElse(null);
    }

    public void checkTestDataAuthorization(SysUser requestor, String testId) {
        TestTask testTask = getTestTaskDetail(testId);
        if (testTask == null) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "The TestTask linked doesn't exist!");
        } else if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, testTask.getTeamId())) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestTask doesn't belong to user's Teams");
        }
    }
}
