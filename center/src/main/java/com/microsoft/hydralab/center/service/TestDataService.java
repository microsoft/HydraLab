// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.service;

import com.alibaba.fastjson.JSONArray;
import com.microsoft.hydralab.center.repository.StabilityDataRepository;
import com.microsoft.hydralab.common.entity.center.StabilityData;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.*;
import com.microsoft.hydralab.common.repository.AndroidTestUnitRepository;
import com.microsoft.hydralab.common.repository.DeviceTestResultRepository;
import com.microsoft.hydralab.common.repository.KeyValueRepository;
import com.microsoft.hydralab.common.repository.TestTaskRepository;
import com.microsoft.hydralab.common.util.AttachmentService;
import com.microsoft.hydralab.common.util.CriteriaTypeUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@CacheConfig(cacheNames = "taskCache")
public class TestDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataService.class);
    private final Sort sortByStartMillis = Sort.by(Sort.Direction.DESC, "startTimeMillis");
    private final Sort sortByStartDate = Sort.by(Sort.Direction.DESC, "startDate");
    @Lazy
    @Resource
    private TestDataService testDataServiceCache;
    @Resource
    TestTaskRepository testTaskRepository;
    @Resource
    AndroidTestUnitRepository androidTestUnitRepository;
    @Resource
    DeviceTestResultRepository deviceTestResultRepository;
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

    @Cacheable(key = "#testId")
    public TestTask getTestTaskDetail(String testId) {
        Optional<TestTask> taskOpt = testTaskRepository.findById(testId);
        if (taskOpt.isEmpty()) {
            return null;
        }
        TestTask testTask = taskOpt.get();
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

    public Set<String> cancelTaskById(String taskId, String reason) {
        TestTask testTaskHis = testDataServiceCache.getTestTaskDetail(taskId);
        testTaskHis.setStatus(TestTask.TestStatus.CANCELED);
        testTaskHis.setTestErrorMsg(reason);

        testDataServiceCache.saveTestTaskData(testTaskHis);
        return testTaskHis.getAgentIds();
    }

    public void saveTestTaskDataFromAgent(TestTask testTask, boolean persistence, String agentId) {
        TestTask testTaskHis = testDataServiceCache.getTestTaskDetail(testTask.getId());
        //run by device
        if (testTaskHis.agentIds.size() == 0) {
            testDataServiceCache.saveTestTaskData(testTask);
            return;
        }
        //run by group
        if (!persistence) {
            return;
        }

        testTaskHis.getDeviceTestResults().addAll(testTask.getDeviceTestResults());
        testTaskHis.setTotalTestCount(testTaskHis.getTotalTestCount() + testTask.getTotalTestCount());
        testTaskHis.setTotalFailCount(testTaskHis.getTotalFailCount() + testTask.getTotalFailCount());
        testTaskHis.setTestSuite(testTask.getTestSuite());
        testTaskHis.agentIds.remove(agentId);

        boolean isAllFinish = testTaskHis.agentIds.size() == 0;
        if (isAllFinish) {
            testTaskHis.setStatus(TestTask.TestStatus.FINISHED);
            testTaskHis.setEndDate(testTask.getEndDate());
        }
        testDataServiceCache.saveTestTaskData(testTaskHis);
    }

    @CachePut(key = "#testTask.id")
    public TestTask saveTestTaskData(TestTask testTask) {
        testTaskRepository.save(testTask);
        List<DeviceTestTask> deviceTestResults = testTask.getDeviceTestResults();
        if (deviceTestResults.isEmpty()) {
            return testTask;
        }

        deviceTestResultRepository.saveAll(deviceTestResults);

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
        return testTask;
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
        TestTask testTask = testDataServiceCache.getTestTaskDetail(testId);
        if (testTask == null) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "The TestTask linked doesn't exist!");
        } else if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, testTask.getTeamId())) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestTask doesn't belong to user's Teams");
        }
    }
}
