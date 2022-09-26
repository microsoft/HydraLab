// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.repository;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.KeyValue;
import com.microsoft.hydralab.common.entity.common.TestTask;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class KeyValueRepository {
    public final static String RECENT_APK_SET = "RECENT_APK_SET";
    public final static String UNIT_TEST_DETAIL = "UNIT_TEST_DETAIL:";
    public static final String DEVICE_RES_INFO = "Device_Res_Info:";
    public final static String CRASH_STACK_IN_DEVICE = "CRASH_STACK:";
    private static final int RUNNING_TASK_MAX_SIZE = 32;
    private final Map<String, TestTask> runningTaskCache = new HashMap<>();
    private final ArrayList<String> runningTaskIdCache = new ArrayList<>();
    @Resource
    KeyValueDBRepository keyValueDBRepository;

    public void putKeyValuePairDB(String key, String value) {
        keyValueDBRepository.save(new KeyValue(key, value));
    }

    public String getValueByKeyDB(String key) {
        return keyValueDBRepository.getOne(key).getValue();
    }

    public void saveCrashStack(String crashStackId, String crashStack) {
        if (StringUtils.isBlank(crashStack)) {
            return;
        }
        putKeyValuePairDB(CRASH_STACK_IN_DEVICE + crashStackId, crashStack.replace("\n", "<br>"));
    }

    public void saveDeviceTestResultResInfo(DeviceTestTask result) {
        JSONArray videoTimeTagArr = result.getVideoTimeTagArr();
        if (videoTimeTagArr == null) {
            return;
        }
        putKeyValuePairDB(DEVICE_RES_INFO + result.getId(), videoTimeTagArr.toJSONString());
    }

    public synchronized TestTask getTestTaskMem(String testId) {
        return runningTaskCache.get(testId);
    }

    public synchronized List<TestTask> getTestTasksMemByTeamId(String teamId) {
        List<TestTask> list = new ArrayList<>();
        runningTaskCache.forEach((id, task) -> {
            if (task.getTeamId().equals(teamId)) {
                list.add(task);
            }
        });
        return list;
    }

    public synchronized List<TestTask> getRunningTestTasksMem() {
        List<TestTask> list = new ArrayList<>();
        runningTaskCache.forEach((id, task) -> {
            if (task.getStatus().equals(TestTask.TestStatus.RUNNING)) {
                list.add(task);
            }
        });
        return list;
    }

    public synchronized void saveTestTask(TestTask testTask) {
        runningTaskCache.put(testTask.getId(), testTask);
        runningTaskIdCache.add(testTask.getId());
        if (runningTaskIdCache.size() > RUNNING_TASK_MAX_SIZE) {
            runningTaskCache.remove(runningTaskIdCache.remove(0));
        }
    }

    public synchronized void deleteTestTaskById(String testTaskId) {
        runningTaskIdCache.remove(testTaskId);
        runningTaskCache.remove(testTaskId);
    }

    public void saveAndroidTestUnit(AndroidTestUnit androidTestUnit) {
        putKeyValuePairDB(UNIT_TEST_DETAIL + androidTestUnit.getId(), JSON.toJSONString(androidTestUnit));
    }

    public void updateApkSet(String apkId) {

    }

    public AndroidTestUnit getAndroidTestUnit(String testCaseId) {
        return JSON.parseObject(getValueByKeyDB(UNIT_TEST_DETAIL + testCaseId), AndroidTestUnit.class);
    }

    public String getCrashStack(String crashStackId) {
        return getValueByKeyDB(CRASH_STACK_IN_DEVICE + crashStackId);
    }

    public JSONArray getDeviceTestResInfo(String id) {
        return JSON.parseArray(getValueByKeyDB(DEVICE_RES_INFO + id));
    }

}
