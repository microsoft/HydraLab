import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import static org.mockito.Mockito.*;

public class TestTaskServiceTest {
    @Mock
    private TestDataService testDataService;

    private TestTaskService testTaskService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        testTaskService = new TestTaskService();
        testTaskService.testDataService = testDataService;
    }

    @Test
    public void testUpdateTaskTeam() {
        String teamId = "123";
        String teamName = "Test Team";

        List<TestTask> testTasks = mock(List.class);
        when(testDataService.getTasksByTeamId(teamId)).thenReturn(testTasks);

        testTaskService.updateTaskTeam(teamId, teamName);

        verify(testTasks, times(1)).forEach(any());
        verify(testDataService, times(1)).saveAllTestTasks(testTasks);
    }
}