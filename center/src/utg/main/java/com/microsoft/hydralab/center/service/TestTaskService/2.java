import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestTaskServiceTest {
@Mock
private DeviceAgentManagementService deviceAgentManagementService;
@Mock
private TestTaskSpec testTaskSpec;
private TestTaskService testTaskService;
@Before
public void setUp() {
 MockitoAnnotations.initMocks(this); testTaskService = new TestTaskService(); testTaskService.deviceAgentManagementService = deviceAgentManagementService; 
}

@Test
public void testIsDeviceFree() {
 String deviceIdentifier = "device1"; when(deviceAgentManagementService.queryGroupByDevice(deviceIdentifier)).thenReturn(new HashSet<>()); when(deviceAgentManagementService.queryDeviceByGroup(deviceIdentifier)).thenReturn(new HashSet<>()); Boolean result = testTaskService.isDeviceFree(deviceIdentifier); assertTrue(result); verify(deviceAgentManagementService, times(1)).queryGroupByDevice(deviceIdentifier); verify(deviceAgentManagementService, times(1)).queryDeviceByGroup(deviceIdentifier); 
}

}
