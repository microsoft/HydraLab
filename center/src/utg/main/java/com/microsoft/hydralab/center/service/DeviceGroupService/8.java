import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceGroupServiceTest {
@Mock
private DeviceGroupRepository deviceGroupRepository;
@Test
public void testCheckGroupName() {
 String groupName = "testGroup"; int count = 1; when(deviceGroupRepository.countByGroupName(groupName)).thenReturn(count); DeviceGroupService deviceGroupService = new DeviceGroupService(); deviceGroupService.deviceGroupRepository = deviceGroupRepository; boolean result = deviceGroupService.checkGroupName(groupName); assertTrue(result); verify(deviceGroupRepository, times(1)).countByGroupName(groupName); 
}

}
