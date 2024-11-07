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
 MockitoAnnotations.initMocks(this); deviceGroupService = new DeviceGroupService(); deviceGroupService.setDeviceGroupRepository(deviceGroupRepository); 
}

@Test
public void testUpdateGroup() {
 DeviceGroup deviceGroup = new DeviceGroup(); deviceGroup.setTeamId("teamId"); deviceGroup.setTeamName("teamName"); deviceGroup.setGroupName("groupName"); deviceGroup.setOwner("owner"); when(deviceGroupRepository.save(deviceGroup)).thenReturn(deviceGroup); DeviceGroup result = deviceGroupService.updateGroup(deviceGroup); verify(deviceGroupRepository, times(1)).save(deviceGroup); assertEquals(deviceGroup, result); 
}

}
