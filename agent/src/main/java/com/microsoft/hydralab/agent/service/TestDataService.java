// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.common.entity.common.*;
import com.microsoft.hydralab.common.repository.AndroidTestUnitRepository;
import com.microsoft.hydralab.common.repository.TestRunRepository;
import com.microsoft.hydralab.common.repository.KeyValueRepository;
import com.microsoft.hydralab.common.repository.TestTaskRepository;
import com.microsoft.hydralab.common.util.AttachmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class TestDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataService.class);
    @Resource
    TestTaskRepository testTaskRepository;
    @Resource
    AndroidTestUnitRepository androidTestUnitRepository;
    @Resource
    TestRunRepository testRunRepository;
    @Resource
    KeyValueRepository keyValueRepository;
    @Resource
    AttachmentService attachmentService;

    public void saveTestTaskData(TestTask testTask) {
        List<TestRun> testRuns = testTask.getDeviceTestResults();
        if (testRuns.isEmpty()) {
            return;
        }

        testRunRepository.saveAll(testRuns);
        testTaskRepository.save(testTask);

        List<AndroidTestUnit> list = new ArrayList<>();
        for (TestRun testRun : testRuns) {
            attachmentService.saveRelations(testRun.getId(), EntityType.TEST_RESULT, testRun.getAttachments());

            List<AndroidTestUnit> testUnitList = testRun.getTestUnitList();
            list.addAll(testUnitList);

            // only save failed cases
            for (AndroidTestUnit androidTestUnit : testUnitList) {
                if (androidTestUnit.isSuccess()) {
                    continue;
                }
                LOGGER.warn("one more failed cases saved: {}", androidTestUnit.getTitle());
                keyValueRepository.saveAndroidTestUnit(androidTestUnit);
            }

            String crashStack = testRun.getCrashStack();
            if (crashStack == null) {
                continue;
            }
            keyValueRepository.saveCrashStack(testRun.getCrashStackId(), testRun.getCrashStack());
        }
        androidTestUnitRepository.saveAll(list);
        LOGGER.info("All saved {}", testTask.getId());
    }
}
