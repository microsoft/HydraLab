package com.microsoft.hydralab.agent.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public interface ITestRun {
    File getResultFolder();

    String getId();

    String getDeviceSerialNumberByType(String type);

    String getOngoingTestUnitName();

    default Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }
}
