import org.junit.Test;
import org.mockito.Mockito;

public class ChatRequestTest {

    @Test
    public void testSetMaxTokens() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        int maxTokens = 1000;
        
        chatRequest.setMaxTokens(maxTokens);
        
        Mockito.verify(chatRequest).setMaxTokens(maxTokens);
    }
}