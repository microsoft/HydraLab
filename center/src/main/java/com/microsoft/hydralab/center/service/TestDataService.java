// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.alibaba.fastjson.JSONArray;
import com.microsoft.hydralab.center.repository.StabilityDataRepository;
import com.microsoft.hydralab.common.entity.center.StabilityData;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.PerformanceTestResultEntity;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.repository.AndroidTestUnitRepository;
import com.microsoft.hydralab.common.repository.KeyValueRepository;
import com.microsoft.hydralab.common.repository.PerformanceTestResultRepository;
import com.microsoft.hydralab.common.repository.TestRunRepository;
import com.microsoft.hydralab.common.repository.TestTaskRepository;
import com.microsoft.hydralab.common.util.AttachmentService;
import com.microsoft.hydralab.common.util.CriteriaTypeUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@CacheConfig(cacheNames = "taskCache")
public class TestDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataService.class);
    private final Sort sortByStartMillis = Sort.by(Sort.Direction.DESC, "startTimeMillis");
    private final Sort sortByStartDate = Sort.by(Sort.Direction.DESC, "startDate");
    @Resource
    TestTaskRepository testTaskRepository;
    @Resource
    AndroidTestUnitRepository androidTestUnitRepository;
    @Resource
    TestRunRepository testRunRepository;
    @Resource
    KeyValueRepository keyValueRepository;
    @Resource
    StabilityDataRepository stabilityDataRepository;
    @Resource
    PerformanceTestResultRepository performanceTestResultRepository;
    @Resource
    AttachmentService attachmentService;
    @Resource
    SysUserService sysUserService;
    @Resource
    UserTeamManagementService userTeamManagementService;
    @Resource
    EntityManager entityManager;
    @Lazy
    @Resource
    private TestDataService testDataServiceCache;

    public List<AndroidTestUnit> getAllTestUnit(int page, int size) {
        List<AndroidTestUnit> testUnits = androidTestUnitRepository.findBySuccess(false, PageRequest.of(page, size, sortByStartMillis)).getContent();

        for (AndroidTestUnit testUnit : testUnits) {
            Optional<TestRun> deviceTestTask = testRunRepository.findById(testUnit.getDeviceTestResultId());
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
        List<TestRun> byTestTaskId = testRunRepository.findByTestTaskId(testId);

        if (byTestTaskId == null || byTestTaskId.isEmpty()) {
            return testTask;
        }

        testTask.getDeviceTestResults().addAll(byTestTaskId);
        for (TestRun deviceTestResult : byTestTaskId) {
            List<AndroidTestUnit> byDeviceTestResultId = androidTestUnitRepository.findByDeviceTestResultId(deviceTestResult.getId());
            deviceTestResult.getTestUnitList().addAll(byDeviceTestResultId);
            deviceTestResult.setAttachments(attachmentService.getAttachments(deviceTestResult.getId(), EntityType.TEST_RESULT));
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
        TestTask oldTestTask = testDataServiceCache.getTestTaskDetail(taskId);
        oldTestTask.setStatus(TestTask.TestStatus.CANCELED);
        oldTestTask.setTestErrorMsg(reason);

        testDataServiceCache.saveTestTaskData(oldTestTask);
        return oldTestTask.getAgentIds();
    }

    public void saveTestTaskDataFromAgent(TestTask testTask, boolean persistence, String agentId) {
        TestTask oldTestTask = testDataServiceCache.getTestTaskDetail(testTask.getId());
        //run by device
        if (oldTestTask.agentIds.size() == 0) {
            testDataServiceCache.saveTestTaskData(testTask);
            return;
        }
        //run by group
        if (!persistence) {
            return;
        }

        oldTestTask.getDeviceTestResults().addAll(testTask.getDeviceTestResults());
        oldTestTask.setTotalTestCount(oldTestTask.getTotalTestCount() + testTask.getTotalTestCount());
        oldTestTask.setTotalFailCount(oldTestTask.getTotalFailCount() + testTask.getTotalFailCount());
        oldTestTask.setTestSuite(testTask.getTestSuite());
        oldTestTask.agentIds.remove(agentId);

        boolean isAllFinish = oldTestTask.agentIds.size() == 0;
        if (isAllFinish) {
            oldTestTask.setStatus(TestTask.TestStatus.FINISHED);
            oldTestTask.setEndDate(testTask.getEndDate());
        }
        testDataServiceCache.saveTestTaskData(oldTestTask);
    }

    @CachePut(key = "#testTask.id")
    public TestTask saveTestTaskData(TestTask testTask) {
        testTaskRepository.save(testTask);
        List<TestRun> deviceTestResults = testTask.getDeviceTestResults();
        if (deviceTestResults.isEmpty()) {
            return testTask;
        }

        testRunRepository.saveAll(deviceTestResults);

        List<AndroidTestUnit> list = new ArrayList<>();
        for (TestRun deviceTestResult : deviceTestResults) {
            attachmentService.saveAttachments(deviceTestResult.getId(), EntityType.TEST_RESULT, deviceTestResult.getAttachments());
            performanceTestResultRepository.saveAll(deviceTestResult.getPerformanceTestResultEntities());

            List<AndroidTestUnit> testUnitList = deviceTestResult.getTestUnitList();
            list.addAll(testUnitList);

            // only save failed cases
            for (AndroidTestUnit androidTestUnit : testUnitList) {
                if (androidTestUnit.isSuccess()) {
                    continue;
                }
                LOGGER.warn("one more failed cases saved: {}", androidTestUnit.getTitle());
                keyValueRepository.saveAndroidTestUnit(androidTestUnit);
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

    public TestRun getTestRunWithVideoInfo(String dttId) {
        TestRun testRun = testRunRepository.getOne(dttId);
        JSONArray deviceTestResInfo = keyValueRepository.getDeviceTestResInfo(dttId);
        testRun.setVideoTimeTagArr(deviceTestResInfo);
        testRun.setVideoBlobUrl();
        testRun.setAttachments(attachmentService.getAttachments(dttId, EntityType.TEST_RESULT));
        return testRun;
    }

    public TestRun getTestRunByCrashId(String crashId) {
        return testRunRepository.findByCrashStackId(crashId).orElse(null);
    }

    public void checkTestDataAuthorization(SysUser requestor, String testId) {
        TestTask testTask = testDataServiceCache.getTestTaskDetail(testId);
        if (testTask == null) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "The TestTask linked doesn't exist!");
        }
        // temporarily disable team auth check before a better solution on access to test result comes out.
//        else if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, testTask.getTeamId())) {
//            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestTask doesn't belong to user's Teams");
//        }
    }

    public List<PerformanceTestResultEntity> getPerformanceTestHistory(List<CriteriaType> queryParams) {
        Specification<PerformanceTestResultEntity> spec = new CriteriaTypeUtil<PerformanceTestResultEntity>().transferToSpecification(queryParams, false);
        //TODO set page
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "Date"));
        List<PerformanceTestResultEntity> perfHistoryList = new ArrayList<>(performanceTestResultRepository.findAll(spec, pageRequest).getContent());
        Collections.reverse(perfHistoryList);
        return perfHistoryList;
    }

    @CacheEvict(key = "#testRun.testTaskId")
    public void saveGPTSuggestion(TestRun testRun, String suggestion) {
        testRun.setSuggestion(suggestion);
        testRunRepository.save(testRun);
    }

    public TestRun findTestRunById(String testRunId) {
        return testRunRepository.findById(testRunId).orElse(null);
    }
}
