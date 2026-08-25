import com.microsoft.hydralab.center.openai.AzureOpenAIServiceClient;
import com.alibaba.fastjson.JSON;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class AzureOpenAIServiceClientTest {
    @Mock
    private OkHttpClient client;
    
    private AzureOpenAIServiceClient serviceClient;
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        serviceClient = new AzureOpenAIServiceClient("apiKey", "deployment", "endpoint");
        serviceClient.client = client;
    }
    
    @Test
    public void testChatCompletion() throws Exception {
        // Arrange
        ChatRequest request = new ChatRequest();
        request.setMessage("Hello");
        
        String expectedResponse = "Response";
        String requestBodyString = JSON.toJSONString(request);
        String apiVersion = "2023-03-15-preview";
        
        MediaType mediaType = MediaType.parse("application/json");
        String url = "endpoint/openai/deployments/deployment/chat/completions?api-version=2023-03-15-preview";
        RequestBody body = RequestBody.create(requestBodyString, mediaType);
        Request httpRequest = new Request.Builder().url(url).post(body)
                .addHeader("api-key", "apiKey").build();
        Response response = mock(Response.class);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(expectedResponse);
        when(client.newCall(httpRequest).execute()).thenReturn(response);
        
        // Act
        String result = serviceClient.chatCompletion(request);
        
        // Assert
        verify(client).newCall(httpRequest).execute();
        verify(response).isSuccessful();
        verify(response).body();
        assertEquals(expectedResponse, result);
    }
}