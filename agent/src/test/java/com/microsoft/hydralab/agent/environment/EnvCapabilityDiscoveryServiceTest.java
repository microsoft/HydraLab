package com.microsoft.hydralab.agent.environment;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class EnvCapabilityDiscoveryServiceTest {

    @Test
    public void runDiscovery() throws IOException {
        EnvCapabilityDiscoveryService envCapabilityDiscoveryService = new EnvCapabilityDiscoveryService();
        envCapabilityDiscoveryService.discover();
        EnvInfo evnInfo = envCapabilityDiscoveryService.getEnvInfo();
        evnInfo.getCapabilities().forEach(capability -> {
            System.out.println(capability.getKeyword());
            System.out.println(capability.getFile());
            System.out.println(capability.getVersion());
            System.out.println(capability.getKeyword().versionOutput);
        });
    }
}
