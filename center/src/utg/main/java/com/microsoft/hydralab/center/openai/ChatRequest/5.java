import org.junit.Test;
import org.mockito.Mockito;

public class ChatRequestTest {
@Test
public void testSetFrequencyPenalty() {
 ChatRequest chatRequest = Mockito.mock(ChatRequest.class); double frequencyPenalty = 0.5; chatRequest.setFrequencyPenalty(frequencyPenalty); Mockito.verify(chatRequest).setFrequencyPenalty(frequencyPenalty); 
}

}
