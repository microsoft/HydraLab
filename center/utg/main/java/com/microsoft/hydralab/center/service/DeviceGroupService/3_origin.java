import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import com.microsoft.hydralab.center.repository.DeviceGroupRelationRepository;
import com.microsoft.hydralab.center.repository.DeviceGroupRepository;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class DeviceGroupServiceTest {
    @Mock
    private DeviceGroupRepository deviceGroupRepository;
    @Mock
    private DeviceGroupRelationRepository deviceGroupRelationRepository;
    @InjectMocks
    private DeviceGroupService deviceGroupService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDeleteGroup() {
        String groupName = "TestGroup";
        
        deviceGroupService.deleteGroup(groupName);
        
        Mockito.verify(deviceGroupRepository).deleteById(groupName);
        Mockito.verify(deviceGroupRelationRepository).deleteAllByGroupName(groupName);
    }
}