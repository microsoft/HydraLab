import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class DeviceGroupServiceTest {
@Mock
private DeviceGroupRepository deviceGroupRepository;
private DeviceGroupService deviceGroupService;
@Before
public void setUp() {
 deviceGroupService = new DeviceGroupService(); deviceGroupService.deviceGroupRepository = deviceGroupRepository; 
}

@Test
public void testGetGroupByName() {
 String groupName = "TestGroup"; DeviceGroup expectedGroup = new DeviceGroup(); expectedGroup.setGroupName(groupName); Mockito.when(deviceGroupRepository.findById(anyString())).thenReturn(java.util.Optional.of(expectedGroup)); DeviceGroup actualGroup = deviceGroupService.getGroupByName(groupName); assertEquals(expectedGroup, actualGroup); 
}

}
