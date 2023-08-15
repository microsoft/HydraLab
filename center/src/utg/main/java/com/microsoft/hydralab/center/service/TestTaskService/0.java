import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class TestTaskServiceTest {
@Mock
private DeviceAgentManagementService deviceAgentManagementService;
@Mock
private AgentManageService agentManageService;
@Mock
private DeviceGroupService deviceGroupService;
@Mock
private TestDataService testDataService;
private TestTaskService testTaskService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); testTaskService = new TestTaskService(); testTaskService.deviceAgentManagementService = deviceAgentManagementService; testTaskService.agentManageService = agentManageService; testTaskService.deviceGroupService = deviceGroupService; testTaskService.testDataService = testDataService; 
}

@Test
public void testAddTask() {
 TestTaskSpec task = new TestTaskSpec(); testTaskService.addTask(task); verify(testDataService, times(1)).saveTestTaskData(any(TestTask.class)); 
}

}
