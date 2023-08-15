import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;

public class ChatRequestTest {
@Test
public void testGetMaxTokens() {
 ChatRequest chatRequest = Mockito.mock(ChatRequest.class); Mockito.when(chatRequest.getMaxTokens()).thenReturn(800); int result = chatRequest.getMaxTokens(); assertEquals(800, result); 
}

}
