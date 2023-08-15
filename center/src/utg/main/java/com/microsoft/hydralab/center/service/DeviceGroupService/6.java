import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class DeviceGroupServiceTest {
@Mock
private DeviceGroupRepository deviceGroupRepository;
@Mock
private DeviceGroupRelationRepository deviceGroupRelationRepository;
@Mock
private SysUserService sysUserService;
@Mock
private UserTeamManagementService userTeamManagementService;
private DeviceGroupService deviceGroupService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); deviceGroupService = new DeviceGroupService(); deviceGroupService.deviceGroupRepository = deviceGroupRepository; deviceGroupService.deviceGroupRelationRepository = deviceGroupRelationRepository; deviceGroupService.sysUserService = sysUserService; deviceGroupService.userTeamManagementService = userTeamManagementService; 
}

@Test
public void testGetRelation() {
 String groupName = "group1"; String deviceSerial = "serial1"; DeviceGroupRelationId id = new DeviceGroupRelationId(); id.setDeviceSerial(deviceSerial); id.setGroupName(groupName); DeviceGroupRelation expectedRelation = new DeviceGroupRelation(groupName, deviceSerial); when(deviceGroupRelationRepository.findById(id)).thenReturn(java.util.Optional.of(expectedRelation)); DeviceGroupRelation actualRelation = deviceGroupService.getRelation(groupName, deviceSerial); assertEquals(expectedRelation, actualRelation); verify(deviceGroupRelationRepository, times(1)).findById(id); 
}

}
