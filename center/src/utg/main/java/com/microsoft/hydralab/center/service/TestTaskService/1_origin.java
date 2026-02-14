import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.util.Queue;

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
        MockitoAnnotations.initMocks(this);
        testTaskService = new TestTaskService();
        testTaskService.deviceAgentManagementService = deviceAgentManagementService;
        testTaskService.agentManageService = agentManageService;
        testTaskService.deviceGroupService = deviceGroupService;
        testTaskService.testDataService = testDataService;
    }

    @Test
    public void testIsQueueEmpty() {
        Queue<TestTaskSpec> taskQueue = Mockito.mock(Queue.class);
        Mockito.when(taskQueue.isEmpty()).thenReturn(true);
        testTaskService.taskQueue = taskQueue;

        Boolean result = testTaskService.isQueueEmpty();

        Assert.assertTrue(result);
    }
}