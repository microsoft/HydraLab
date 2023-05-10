package com.microsoft.hydralab.agent.runner;

import java.io.File;

public interface ITestRun {
    File getResultFolder();

    String getId();

    String getDeviceSerialNumberByType(String type);

    String getOngoingTestUnitName();
}
