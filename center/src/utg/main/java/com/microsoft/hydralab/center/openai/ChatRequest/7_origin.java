import org.junit.Test;
import org.mockito.Mockito;

public class ChatRequestTest {

    @Test
    public void testSetPresencePenalty() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        double presencePenalty = 0.5;

        chatRequest.setPresencePenalty(presencePenalty);

        Mockito.verify(chatRequest).setPresencePenalty(presencePenalty);
    }
}