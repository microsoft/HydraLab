package com.microsoft.hydralab.agent.environment;

import org.junit.jupiter.api.Test;

public class EnvCapabilityDiscoveryServiceTest {

    @Test
    public void runDiscovery() {
        EnvCapabilityDiscoveryService envCapabilityDiscoveryService = new EnvCapabilityDiscoveryService();
        envCapabilityDiscoveryService.discover();
        EnvInfo evnInfo = envCapabilityDiscoveryService.getEnvInfo();
        evnInfo.getCapabilities().forEach(capability -> {
            System.out.println(capability.getName());
            System.out.println(capability.getFile());
            System.out.println(capability.getVersion());
        });
    }
}
