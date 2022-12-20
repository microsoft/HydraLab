// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.common.entity.common.*;
import com.microsoft.hydralab.common.repository.*;
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
    DeviceActionRepository deviceActionRepository;
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

        List<DeviceAction> deviceActions = testTask.getDeviceActions();
        if (deviceActions != null && !deviceActions.isEmpty()) {
            deviceActions.forEach(deviceAction -> deviceAction.setTestTaskId(testTask.getId()));
            deviceActionRepository.saveAll(deviceActions);
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
