// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.repository;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.DeviceTestTask;
import com.microsoft.hydralab.common.entity.common.KeyValue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Repository
@CacheConfig
public class KeyValueRepository {
    public final static String UNIT_TEST_DETAIL = "UNIT_TEST_DETAIL:";
    public static final String DEVICE_RES_INFO = "Device_Res_Info:";
    public final static String CRASH_STACK_IN_DEVICE = "CRASH_STACK:";
    @Resource
    KeyValueDBRepository keyValueDBRepository;
    @Lazy
    @Resource
    KeyValueRepository keyValueRepository;


    @CachePut(key = "#result.keyid")
    public KeyValue putKeyValuePairDB(String key, String value) {
        KeyValue result = new KeyValue(key, value);
        keyValueDBRepository.save(result);
        return result;
    }

    @Cacheable(key = "#key")
    public KeyValue getKeyValuePairDB(String key) {
        return keyValueDBRepository.getOne(key);
    }

    public String getValueByKeyDB(String key) {
        return keyValueRepository.getKeyValuePairDB(key).getValue();
    }

    public void saveCrashStack(String crashStackId, String crashStack) {
        if (StringUtils.isBlank(crashStack)) {
            return;
        }
        keyValueRepository.putKeyValuePairDB(CRASH_STACK_IN_DEVICE + crashStackId, crashStack.replace("\n", "<br>"));
    }

    public void saveDeviceTestResultResInfo(DeviceTestTask result) {
        JSONArray videoTimeTagArr = result.getVideoTimeTagArr();
        if (videoTimeTagArr == null) {
            return;
        }
        keyValueRepository.putKeyValuePairDB(DEVICE_RES_INFO + result.getId(), videoTimeTagArr.toJSONString());
    }

    public void saveAndroidTestUnit(AndroidTestUnit androidTestUnit) {
        keyValueRepository.putKeyValuePairDB(UNIT_TEST_DETAIL + androidTestUnit.getId(), JSON.toJSONString(androidTestUnit));
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
