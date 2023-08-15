import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class DeviceGroupServiceTest {
@Mock
private DeviceGroupRepository deviceGroupRepository;
private DeviceGroupService deviceGroupService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); deviceGroupService = new DeviceGroupService(); deviceGroupService.setDeviceGroupRepository(deviceGroupRepository); 
}

@Test
public void testUpdateGroupTeam() {
 String teamId = "teamId"; String teamName = "teamName"; deviceGroupService.updateGroupTeam(teamId, teamName); verify(deviceGroupRepository, times(1)).saveAll(anyList()); 
}

}
