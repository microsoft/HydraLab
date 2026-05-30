import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.hydralab.center.repository.DeviceGroupRelationRepository;
import com.microsoft.hydralab.center.repository.DeviceGroupRepository;
import com.microsoft.hydralab.common.entity.center.DeviceGroup;
import com.microsoft.hydralab.common.entity.center.DeviceGroupRelation;
import com.microsoft.hydralab.common.util.Const;

@RunWith(MockitoJUnitRunner.class)
public class DeviceGroupServiceTest {

    @Mock
    private DeviceGroupRepository deviceGroupRepository;

    @Mock
    private DeviceGroupRelationRepository deviceGroupRelationRepository;

    private DeviceGroupService deviceGroupService;

    @Before
    public void setup() {
        deviceGroupService = new DeviceGroupService();
        deviceGroupService.deviceGroupRepository = deviceGroupRepository;
        deviceGroupService.deviceGroupRelationRepository = deviceGroupRelationRepository;
    }

    @Test
    public void testSaveRelation() {
        // Arrange
        String groupName = "TestGroup";
        String deviceSerial = "TestSerial";
        DeviceGroupRelation expectedRelation = new DeviceGroupRelation(groupName, deviceSerial);
        Mockito.when(deviceGroupRelationRepository.save(Mockito.any(DeviceGroupRelation.class))).thenReturn(expectedRelation);

        // Act
        DeviceGroupRelation actualRelation = deviceGroupService.saveRelation(groupName, deviceSerial);

        // Assert
        Mockito.verify(deviceGroupRelationRepository).save(Mockito.any(DeviceGroupRelation.class));
        Mockito.verify(deviceGroupRelationRepository, Mockito.times(1)).save(Mockito.any(DeviceGroupRelation.class));
        Mockito.verifyNoMoreInteractions(deviceGroupRelationRepository);
        Assert.assertEquals(expectedRelation, actualRelation);
    }
}