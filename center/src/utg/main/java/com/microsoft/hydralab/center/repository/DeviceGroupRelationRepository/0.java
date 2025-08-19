import com.microsoft.hydralab.center.repository.DeviceGroupRelationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeviceGroupRelationRepositoryTest {
private DeviceGroupRelationRepository deviceGroupRelationRepository;
private DeviceGroupRelationService deviceGroupRelationService;
public void setup() {
", " MockitoAnnotations.initMocks(this);", " 
}

public void testDeleteAllByGroupName() {
", " String groupName = \"TestGroup\";", " List<DeviceGroupRelation> deviceGroupRelations = new ArrayList<>();", " when(deviceGroupRelationRepository.findAllByGroupName(groupName)).thenReturn(deviceGroupRelations);", " deviceGroupRelationService.deleteAllByGroupName(groupName);", " verify(deviceGroupRelationRepository).deleteAllByGroupName(groupName);", " 
}

}
