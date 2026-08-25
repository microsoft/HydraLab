import org.junit.Test;
import org.mockito.Mockito;

public class ChatRequestTest {
@Test
public void testSetStop() {
 ChatRequest chatRequest = Mockito.mock(ChatRequest.class); String stop = "stop"; chatRequest.setStop(stop); Mockito.verify(chatRequest).setStop(stop); 
}

}
