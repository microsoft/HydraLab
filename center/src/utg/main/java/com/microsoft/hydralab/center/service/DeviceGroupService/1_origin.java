import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class DeviceGroupServiceTest {
    @Mock
    private DeviceGroupRepository deviceGroupRepository;

    private DeviceGroupService deviceGroupService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        deviceGroupService = new DeviceGroupService();
        deviceGroupService.setDeviceGroupRepository(deviceGroupRepository);
    }

    @Test
    public void testUpdateGroup() {
        // Create a sample device group
        DeviceGroup deviceGroup = new DeviceGroup();
        deviceGroup.setTeamId("teamId");
        deviceGroup.setTeamName("teamName");
        deviceGroup.setGroupName("groupName");
        deviceGroup.setOwner("owner");

        // Mock the save method of deviceGroupRepository
        when(deviceGroupRepository.save(deviceGroup)).thenReturn(deviceGroup);

        // Call the target function
        DeviceGroup result = deviceGroupService.updateGroup(deviceGroup);

        // Verify that the save method of deviceGroupRepository is called with the correct argument
        verify(deviceGroupRepository, times(1)).save(deviceGroup);

        // Verify that the returned result is the same as the input device group
        assertEquals(deviceGroup, result);
    }
}