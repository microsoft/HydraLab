import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class ChatRequestTest {

    @Test
    public void testGetTemperature() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        Mockito.when(chatRequest.getTemperature()).thenReturn(0.75);

        double expectedTemperature = 0.75;
        double actualTemperature = chatRequest.getTemperature();

        assertEquals(expectedTemperature, actualTemperature, 0.0);
    }
}