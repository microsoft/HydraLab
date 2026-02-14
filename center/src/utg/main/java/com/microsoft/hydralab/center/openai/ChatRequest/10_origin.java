import com.microsoft.hydralab.center.openai.ChatRequest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ChatRequestTest {

    @Test
    public void testGetStop() {
        ChatRequest chatRequest = Mockito.mock(ChatRequest.class);
        String expectedStop = "stop";

        Mockito.when(chatRequest.getStop()).thenReturn(expectedStop);

        String actualStop = chatRequest.getStop();

        Assert.assertEquals(expectedStop, actualStop);
    }
}