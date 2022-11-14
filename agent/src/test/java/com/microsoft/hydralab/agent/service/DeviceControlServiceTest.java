package com.microsoft.hydralab.agent.service;

import com.microsoft.hydralab.agent.runner.espresso.EspressoRunner;
import com.microsoft.hydralab.agent.test.BaseTest;
import com.microsoft.hydralab.common.entity.center.TestTaskSpec;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.management.impl.AndroidDeviceManager;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.annotation.Resource;

public class DeviceControlServiceTest extends BaseTest {

    @Resource
    DeviceControlService deviceControlService;
    @Resource
    DeviceManager deviceManager;
    @MockBean
    EspressoRunner espressoRunner;


    @Test
    public void getAllConnectedDevice() {

    }

    @Test
    public void cancelTestTaskById() {
    }

    @Test
    public void runTestTask() {
        TestTaskSpec taskSpec = new TestTaskSpec();
        taskSpec.runningType = TestTask.TestRunningType.INSTRUMENTATION;
        TestTask testTask = deviceControlService.runTestTask(taskSpec);
    }

    @Test
    public void getDeviceManager() {
        baseLogger.info(String.valueOf(deviceManager instanceof AndroidDeviceManager));
        Assert.assertTrue("Init DeviceManager Bean Error!", deviceManager instanceof AndroidDeviceManager);
    }
}