import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class ChatRequestTest {

    @Test
    public void testGetPresencePenalty() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        Mockito.when(chatRequest.getPresencePenalty()).thenReturn(0.0);

        double result = chatRequest.getPresencePenalty();

        assertEquals(0.0, result, 0.0);
    }
}