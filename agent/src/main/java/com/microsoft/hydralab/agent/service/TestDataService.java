// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.EntityFileRelation;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.repository.AndroidTestUnitRepository;
import com.microsoft.hydralab.common.repository.DeviceTestResultRepository;
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
    DeviceTestResultRepository deviceTestResultRepository;
    @Resource
    KeyValueRepository keyValueRepository;
    @Resource
    AttachmentService attachmentService;

    public void saveTestTaskData(TestTask testTask, boolean complete) {
        keyValueRepository.saveTestTask(testTask);

        if (!complete) {
            return;
        }

        List<DeviceTestTask> deviceTestResults = testTask.getDeviceTestResults();
        if (deviceTestResults.isEmpty()) {
            return;
        }

        deviceTestResultRepository.saveAll(deviceTestResults);
        testTaskRepository.save(testTask);

        List<AndroidTestUnit> list = new ArrayList<>();
        for (DeviceTestTask deviceTestResult : deviceTestResults) {
            attachmentService.saveRelations(deviceTestResult.getId(), EntityFileRelation.EntityType.TEST_RESULT, deviceTestResult.getAttachments());

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

            String crashStack = deviceTestResult.getCrashStack();
            if (crashStack == null) {
                continue;
            }
            keyValueRepository.saveCrashStack(deviceTestResult.getCrashStackId(), deviceTestResult.getCrashStack());
        }
        androidTestUnitRepository.saveAll(list);
        LOGGER.info("All saved {}", testTask.getId());
    }
}
