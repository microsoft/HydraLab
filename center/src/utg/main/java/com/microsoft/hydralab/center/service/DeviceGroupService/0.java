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
public void testCreateGroup() {
 String teamId = "teamId"; String teamName = "teamName"; String groupName = "groupName"; String owner = "owner"; DeviceGroup deviceGroup = new DeviceGroup(); deviceGroup.setTeamId(teamId); deviceGroup.setTeamName(teamName); deviceGroup.setGroupName(Const.DeviceGroup.GROUP_NAME_PREFIX + groupName); deviceGroup.setGroupDisplayName(groupName); deviceGroup.setGroupType(Const.DeviceGroup.USER_GROUP); when(deviceGroupRepository.save(any(DeviceGroup.class))).thenReturn(deviceGroup); DeviceGroup result = deviceGroupService.createGroup(teamId, teamName, groupName, owner); verify(deviceGroupRepository, times(1)).save(any(DeviceGroup.class)); assertEquals(deviceGroup, result); 
}

}
