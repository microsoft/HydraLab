import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AppConfigTest {

    @Mock
    private FileReporter fileReporter;

    @Test
    public void testFileReporter() {
        AppConfig appConfig = new AppConfig();
        appConfig.fileReporter();

        verify(fileReporter, times(1)).init(anyString());
        verify(fileReporter, times(1)).registerExceptionReporter();
    }
}