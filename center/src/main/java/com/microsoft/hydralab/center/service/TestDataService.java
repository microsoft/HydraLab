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
import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.TaskResult;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.repository.AndroidTestUnitRepository;
import com.microsoft.hydralab.common.repository.KeyValueRepository;
import com.microsoft.hydralab.common.repository.PerformanceTestResultRepository;
import com.microsoft.hydralab.common.repository.TaskRepository;
import com.microsoft.hydralab.common.repository.TaskResultRepository;
import com.microsoft.hydralab.common.repository.TestRunRepository;
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
    TaskRepository taskRepository;
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

    @Resource
    private TaskResultRepository taskResultRepository;

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

    @Cacheable(key = "#id")
    public Task getTaskDetail(String id) {
        Optional<Task> taskOpt = taskRepository.findById(id);
        if (taskOpt.isEmpty()) {
            return null;
        }
        Task task = taskOpt.get();
        List<TestRun> byTestTaskId = testRunRepository.findByTestTaskId(id);

        if (byTestTaskId == null || byTestTaskId.isEmpty()) {
            return task;
        }

        task.getTaskRunList().addAll(byTestTaskId);
        for (TestRun deviceTestResult : byTestTaskId) {
            List<AndroidTestUnit> byDeviceTestResultId = androidTestUnitRepository.findByDeviceTestResultId(deviceTestResult.getId());
            deviceTestResult.getTestUnitList().addAll(byDeviceTestResultId);
            deviceTestResult.setAttachments(attachmentService.getAttachments(deviceTestResult.getId(), EntityType.TEST_RESULT));
            deviceTestResult.setTaskResult(taskResultRepository.findByTaskRunId(deviceTestResult.getId()).orElse(null));
        }
        return task;
    }

    public List<Task> getTasksByTeamId(String teamId) {
        return taskRepository.findAllByTeamId(teamId);
    }

    public void saveAllTestTasks(List<Task> testTasks) {
        taskRepository.saveAll(testTasks);
    }

    public Page<Task> getTasks(int page, int pageSize, List<CriteriaType> queryParams) {
        Specification<Task> spec = null;
        if (queryParams != null && queryParams.size() > 0) {
            spec = new CriteriaTypeUtil<Task>().transferToSpecification(queryParams, false);
        }

        Page<Task> pageObj = taskRepository.findAll(spec, PageRequest.of(page, pageSize, sortByStartDate));
        return pageObj;
    }

    public List<Task> getTestTaskSuites(List<CriteriaType> queryParams) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Task> query = criteriaBuilder.createQuery(Task.class);
        Root<Task> root = query.from(Task.class);

        if (queryParams != null && queryParams.size() > 0) {
            query.where(new CriteriaTypeUtil<Task>().transferToPredicate(root, criteriaBuilder, queryParams));
        }

        query.select(root.get("taskAlias")).distinct(true);
        query.orderBy(criteriaBuilder.asc(root.get("taskAlias")));

        List<Task> result = entityManager.createQuery(query).getResultList();
        return result;
    }

    public Set<String> cancelTaskById(String taskId, String reason) {
        Task oldTestTask = testDataServiceCache.getTaskDetail(taskId);
        oldTestTask.setStatus(TestTask.TaskStatus.CANCELED);
        oldTestTask.setErrorMsg(reason);

        testDataServiceCache.saveTaskData(oldTestTask);
        return oldTestTask.getAgentIds();
    }

    public void saveTaskDataFromAgent(Task task, boolean persistence, String agentId) {
        Task oldTestTask = testDataServiceCache.getTaskDetail(task.getId());
        //run by device
        if (oldTestTask.agentIds.size() == 0) {
            testDataServiceCache.saveTaskData(task);
            return;
        }
        //run by group
        if (!persistence) {
            return;
        }

        oldTestTask.getTaskRunList().addAll(task.getTaskRunList());
        if (oldTestTask instanceof TestTask) {
            ((TestTask) oldTestTask).setTotalTestCount(((TestTask) oldTestTask).getTotalTestCount() + ((TestTask) task).getTotalTestCount());
            ((TestTask) oldTestTask).setTotalFailCount(((TestTask) oldTestTask).getTotalFailCount() + ((TestTask) task).getTotalFailCount());
            ((TestTask) oldTestTask).setTestSuite(((TestTask) task).getTestSuite());
        }

        oldTestTask.agentIds.remove(agentId);

        boolean isAllFinish = oldTestTask.agentIds.size() == 0;
        if (isAllFinish) {
            oldTestTask.setStatus(Task.TaskStatus.FINISHED);
            oldTestTask.setEndDate(task.getEndDate());
        }
        testDataServiceCache.saveTaskData(oldTestTask);
    }

    @CachePut(key = "#task.id")
    public Task saveTaskData(Task task) {
        taskRepository.save(task);
        List<TestRun> deviceTestResults = task.getTaskRunList();
        if (deviceTestResults.isEmpty()) {
            return task;
        }

        testRunRepository.saveAll(deviceTestResults);

        List<AndroidTestUnit> list = new ArrayList<>();
        for (TestRun deviceTestResult : deviceTestResults) {
            attachmentService.saveAttachments(deviceTestResult.getId(), EntityType.TEST_RESULT, deviceTestResult.getAttachments());
            performanceTestResultRepository.saveAll(deviceTestResult.getPerformanceTestResultEntities());

            List<AndroidTestUnit> testUnitList = deviceTestResult.getTestUnitList();
            list.addAll(testUnitList);

            TaskResult taskResult = deviceTestResult.getTaskResult();
            if (taskResult != null) {
                taskResult.setTaskId(task.getId());
                taskResult.setTaskRunId(deviceTestResult.getId());
                taskResultRepository.save(taskResult);
            }

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
        LOGGER.info("All saved {}", task.getId());
        return task;
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
        Task task = testDataServiceCache.getTaskDetail(testId);
        if (task == null) {
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

    public enum SuggestionType {
        TestRun,
        TestCase
    }

    @CacheEvict(key = "#testRun.testTaskId")
    public void saveTestRunGPTSuggestion(TestRun testRun, String suggestion) {
        testRun.setSuggestion(suggestion);
        testRunRepository.save(testRun);
    }

    @CacheEvict(key = "#testCase.testTaskId")
    public void saveTestCaseGPTSuggestion(AndroidTestUnit testCase, String suggestion) {
        testCase.setSuggestion(suggestion);
        androidTestUnitRepository.save(testCase);
        keyValueRepository.saveAndroidTestUnit(testCase);
    }

    public TestRun findTestRunById(String testRunId) {
        return testRunRepository.findById(testRunId).orElse(null);
    }

    public AndroidTestUnit findTestCaseById(String testCaseId) {
        return androidTestUnitRepository.findById(testCaseId).orElse(null);
    }
}
