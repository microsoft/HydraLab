package com.microsoft.hydralab.common.network;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
import com.microsoft.hydralab.common.util.ADBOperateUtil;
import org.slf4j.Logger;

import java.io.File;

public class DummyNetworkMonitor implements NetworkMonitor {

    protected Logger logger;

    public DummyNetworkMonitor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void start() {
        logger.info("Start VPN service");
    }
    @Override
    public void stop() {
        logger.info("Stop VPN service");
    }
}
