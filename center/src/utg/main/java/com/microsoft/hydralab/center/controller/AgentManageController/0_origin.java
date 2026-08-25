import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AgentManageControllerTest {

    @Mock
    private DeviceAgentManagementService deviceAgentManagementService;

    @Mock
    private AgentManageService agentManageService;

    @InjectMocks
    private AgentManageController agentManageController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testQueryUpdateInfo() {
        // Arrange
        List<AgentUpdateTask> updateTasks = new ArrayList<>();
        when(deviceAgentManagementService.getUpdateTasks()).thenReturn(updateTasks);

        // Act
        Result result = agentManageController.queryUpdateInfo();

        // Assert
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals(updateTasks, result.getData());
    }
}