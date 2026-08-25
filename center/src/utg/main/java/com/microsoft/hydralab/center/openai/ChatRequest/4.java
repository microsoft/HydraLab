import org.junit.Test;
import org.mockito.Mockito;

public class ChatRequestTest {
@Test
public void testGetFrequencyPenalty() {
 ChatRequest chatRequest = Mockito.mock(ChatRequest.class); Mockito.when(chatRequest.getFrequencyPenalty()).thenReturn(0.0); double frequencyPenalty = chatRequest.getFrequencyPenalty(); assertEquals(0.0, frequencyPenalty, 0.001); 
}

}
