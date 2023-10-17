package com.microsoft.hydralab.agent.environment;

import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvInfo;
import lombok.Getter;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class EnvCapabilityDiscoveryService {
    Logger logger = org.slf4j.LoggerFactory.getLogger(EnvCapabilityDiscoveryService.class);
    @Getter
    private final EnvInfo envInfo = new EnvInfo();
    @Getter
    private EnvCapabilityScanner scanner;

    private boolean enableScan = false;

    public void setEnableScan(boolean enableScan) {
        this.enableScan = enableScan;
    }

    public void discover() throws IOException {
        Properties props = System.getProperties();
        envInfo.setOsName(props.getProperty("os.name"));
        envInfo.setOsVersion(props.getProperty("os.version"));
        envInfo.setUserName(System.getProperty("user.name"));

        logger.info("General ENV_INFO: {}", envInfo);

        determineEnvironmentComponents(envInfo.getOsName());

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
        String osLowerCase = osName.toLowerCase(Locale.US);
        if (osLowerCase.contains("windows")) {
            envInfo.setOs(EnvInfo.OS.WINDOWS);
            scanner = new EnvCapabilityScanner.WindowsScanner();
        } else if (osLowerCase.contains("linux")) {
            envInfo.setOs(EnvInfo.OS.LINUX);
            scanner = new EnvCapabilityScanner.LinuxScanner();
        } else if (osLowerCase.contains("mac os")
                || osLowerCase.contains("macos")) {
            envInfo.setOs(EnvInfo.OS.MACOS);
            scanner = new EnvCapabilityScanner.MacOSScanner();
        } else {
            envInfo.setOs(EnvInfo.OS.UNKNOWN);
        }
    }

}
