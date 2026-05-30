import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private Queue<TestTaskSpec> taskQueue;
    private AtomicBoolean isRunning;
    @Before
    public void setUp() {
        taskQueue = new LinkedList<>();
        isRunning = new AtomicBoolean(false);
    }
    @Test
    public void testRunTask() {
        // Arrange
        TestTaskSpec testTaskSpec = new TestTaskSpec();
        testTaskSpec.setTestTaskId("testTaskId");
        testTaskSpec.setDeviceIdentifier("deviceIdentifier");
        taskQueue.offer(testTaskSpec);
        Mockito.when(testTaskService.getTestQueueCopy()).thenReturn(taskQueue);
        Mockito.when(deviceAgentManagementService.runTestTaskBySpec(testTaskSpec)).thenReturn(new JSONObject());
        // Act
        testTaskService.runTask();
        // Assert
        Mockito.verify(deviceAgentManagementService, Mockito.times(1)).runTestTaskBySpec(testTaskSpec);
        Mockito.verify(testDataService, Mockito.times(1)).saveTestTaskData(Mockito.any(TestTask.class));
        Mockito.verify(testTaskService, Mockito.times(1)).getTestQueueCopy();
        Mockito.verify(testTaskService, Mockito.times(1)).isRunning.set(false);
    }
}