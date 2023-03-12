package com.microsoft.hydralab.agent.environment;

import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class EnvCapabilityDiscoveryService {
    private final EnvInfo envInfo;
    private EnvCapabilityScanner scanner;

    public EnvCapabilityDiscoveryService() {
        envInfo = new EnvInfo();
    }

    public void discover() {
        Properties props = System.getProperties();
        String osName = props.getProperty("os.name");
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
        if (scanner != null) {
            List<EnvCapability> capabilities = scanner.scan();
            envInfo.setCapabilities(capabilities);
        }
    }

    public EnvInfo getEnvInfo() {
        return envInfo;
    }
}
