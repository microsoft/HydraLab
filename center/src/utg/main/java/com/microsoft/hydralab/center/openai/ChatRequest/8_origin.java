import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class ChatRequestTest {

    @Test
    public void testGetTopP() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        Mockito.when(chatRequest.getTopP()).thenReturn(0.95);

        double result = chatRequest.getTopP();

        assertEquals(0.95, result, 0.001);
    }
}