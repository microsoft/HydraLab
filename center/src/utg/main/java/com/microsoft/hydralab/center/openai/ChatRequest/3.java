import org.junit.Test;
import org.mockito.Mockito;

public class ChatRequestTest {
@Test
public void testSetTemperature() {
 ChatRequest chatRequest = Mockito.mock(ChatRequest.class); double temperature = 0.5; chatRequest.setTemperature(temperature); Mockito.verify(chatRequest).setTemperature(temperature); 
}

}
