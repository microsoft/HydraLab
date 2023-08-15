import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestTaskServiceTest {
@Mock
private DeviceAgentManagementService deviceAgentManagementService;
@Mock
private AgentManageService agentManageService;
@Mock
private DeviceGroupService deviceGroupService;
@Mock
private TestDataService testDataService;
@InjectMocks
private TestTaskService testTaskService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); 
}

@Test
public void testCancelTask() {
 String testTaskId = "12345"; testTaskService.cancelTask(testTaskId); verify(testTaskService, times(1)).cancelTask(testTaskId); 
}

}
