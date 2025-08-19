import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DeviceGroupServiceTest {
@Mock
private DeviceGroupRelationRepository deviceGroupRelationRepository;
@InjectMocks
private DeviceGroupService deviceGroupService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); 
}

@Test
public void testDeleteRelation() {
 String groupName = "TestGroup"; String deviceSerial = "TestSerial"; deviceGroupService.deleteRelation(groupName, deviceSerial); Mockito.verify(deviceGroupRelationRepository).delete(Mockito.any(DeviceGroupRelation.class)); 
}

}
