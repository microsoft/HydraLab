package com.microsoft.hydralab.agent.environment;

import com.microsoft.hydralab.agent.runner.appium.AppiumCrossRunner;
import com.microsoft.hydralab.common.entity.agent.AgentFunctionAvailability;
import com.microsoft.hydralab.common.entity.agent.EnvCapability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.agent.EnvInfo;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.impl.WindowsDeviceDriver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.given;

public class EnvCapabilityDiscoveryServiceTest {

    @Test
    public void testDiscovery() throws IOException {
        EnvCapabilityDiscoveryService envCapabilityDiscoveryService = new EnvCapabilityDiscoveryService();
        envCapabilityDiscoveryService.setEnableScan(true);
        envCapabilityDiscoveryService.discover();
        EnvInfo evnInfo = envCapabilityDiscoveryService.getEnvInfo();
        evnInfo.getCapabilities().forEach(capability -> {
            System.out.println(capability.getKeyword());
            System.out.println(capability.isReady());
            System.out.println(capability.getFile());
            System.out.println(capability.getVersion());
            System.out.println(capability.getKeyword().getVersionOutput());
        });
    }

    @Test
    public void testCapabilityMeet() {
        EnvCapability envCapaBility = new EnvCapability(EnvCapability.CapabilityKeyword.appium, 1, 25);
        Assertions.assertTrue(envCapaBility.meet(new EnvCapability(EnvCapability.CapabilityKeyword.appium, -1, 25)));
        Assertions.assertTrue(envCapaBility.meet(new EnvCapability(EnvCapability.CapabilityKeyword.appium, 1, 25)));
        Assertions.assertTrue(envCapaBility.meet(new EnvCapability(EnvCapability.CapabilityKeyword.appium, 1, -1)));
        Assertions.assertTrue(envCapaBility.meet(new EnvCapability(EnvCapability.CapabilityKeyword.appium, 1, 24)));
        Assertions.assertFalse(envCapaBility.meet(new EnvCapability(EnvCapability.CapabilityKeyword.appium, 1, 26)));
        Assertions.assertFalse(envCapaBility.meet(new EnvCapability(EnvCapability.CapabilityKeyword.appium, 2, 22)));
    }

    @Test
    public void testFunctionAvailability() {
        EnvInfo evnInfo = Mockito.mock(EnvInfo.class);
        List<EnvCapability> envCapabilities = new ArrayList<>();
        envCapabilities.add(new EnvCapability(EnvCapability.CapabilityKeyword.appium, 1, 25));
        EnvCapability.CapabilityKeyword.appium.setVersionOutput("1.25.0");
        given(evnInfo.getCapabilities()).willReturn(envCapabilities);

        List<EnvCapabilityRequirement> envCapabilityRequirements = new ArrayList<>();
        envCapabilityRequirements.add(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.appium, 1, -1));
        AgentManagementService agentManagementService = new AgentManagementService();
        agentManagementService.setEnvInfo(evnInfo);
        agentManagementService.registerFunctionAvailability(WindowsDeviceDriver.class.getName(), AgentFunctionAvailability.AgentFunctionType.DEVICE_DRIVER, true,
                envCapabilityRequirements);
        Assertions.assertTrue(agentManagementService.getFunctionAvailabilities().get(0).isAvailable());

        envCapabilityRequirements.add(new EnvCapabilityRequirement(EnvCapability.CapabilityKeyword.ffmpeg, 4, -1));
        agentManagementService.registerFunctionAvailability(AppiumCrossRunner.class.getName(), AgentFunctionAvailability.AgentFunctionType.TEST_RUNNER, true,
                envCapabilityRequirements);
        Assertions.assertFalse(agentManagementService.getFunctionAvailabilities().get(1).isAvailable());
    }
}
