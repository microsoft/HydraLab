import org.junit.Test;
import org.mockito.Mockito;

public class ChatRequestTest {

    @Test
    public void testSetTopP() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        double topP = 0.95;
        
        chatRequest.setTopP(topP);
        
        Mockito.verify(chatRequest).setTopP(topP);
    }
}