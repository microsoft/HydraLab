import com.microsoft.hydralab.center.service.TestTaskService;
import com.microsoft.hydralab.common.entity.center.TestTaskQueuedInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestTaskServiceTest {

    @Mock
    private TestTaskQueuedInfo testTaskQueuedInfo;

    @InjectMocks
    private TestTaskService testTaskService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetTestQueuedInfo() {
        String testTaskId = "12345";

        // Mock the behavior of the method being tested
        when(testTaskService.getTestQueuedInfo(testTaskId)).thenReturn(testTaskQueuedInfo);

        // Call the method being tested
        TestTaskQueuedInfo result = testTaskService.getTestQueuedInfo(testTaskId);

        // Verify the result
        assertEquals(testTaskQueuedInfo, result);
    }
}