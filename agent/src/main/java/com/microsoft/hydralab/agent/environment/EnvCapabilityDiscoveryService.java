package com.microsoft.hydralab.agent.environment;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class EnvCapabilityDiscoveryService {
    Logger logger = org.slf4j.LoggerFactory.getLogger(EnvCapabilityDiscoveryService.class);
    private final EnvInfo envInfo;
    private EnvCapabilityScanner scanner;

    private boolean enableScan = false;

    public void setEnableScan(boolean enableScan) {
        this.enableScan = enableScan;
    }

    public EnvCapabilityDiscoveryService() {
        envInfo = new EnvInfo();
    }

    public void discover() throws IOException {
        Properties props = System.getProperties();
        String osName = props.getProperty("os.name");
        String osVersion = props.getProperty("os.version");
        String userName = System.getProperty("user.name");
        logger.info("Current OS name: {}", osName);
        logger.info("Current OS version: {}", osVersion);
        logger.info("Current process start user is {}", userName);

        determineEnvironmentComponents(osName);

        scanCapabilities();
    }

    private void scanCapabilities() throws IOException {
        if (!enableScan) {
            return;
        }
        if (scanner == null) {
            return;
        }
        List<EnvCapability> capabilities = scanner.scan();
        envInfo.setCapabilities(capabilities);
    }

    private void determineEnvironmentComponents(String osName) {
        if (osName.toLowerCase(Locale.US).contains("windows")) {
            envInfo.setOs(EnvInfo.OS.WINDOWS);
            scanner = new EnvCapabilityScanner.WindowsScanner();
        } else if (osName.toLowerCase(Locale.US).contains("linux")) {
            envInfo.setOs(EnvInfo.OS.LINUX);
            scanner = new EnvCapabilityScanner.LinuxScanner();
        } else if (osName.toLowerCase(Locale.US).contains("mac")) {
            envInfo.setOs(EnvInfo.OS.MACOS);
            scanner = new EnvCapabilityScanner.MacOSScanner();
        } else {
            envInfo.setOs(EnvInfo.OS.UNKNOWN);
        }
    }

    public EnvInfo getEnvInfo() {
        return envInfo;
    }
}
