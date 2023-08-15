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
    public void testIsGroupNameIllegal() {
        DeviceGroupService deviceGroupService = new DeviceGroupService();
        String groupName = "TestGroup";
        boolean expected = false;

        boolean actual = deviceGroupService.isGroupNameIllegal(groupName);

        assertEquals(expected, actual);
    }
}