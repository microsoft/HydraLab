import com.microsoft.hydralab.center.config.WebSocketConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

public class WebSocketConfigTest {
private ServletServerContainerFactoryBean container;
public void testCreateWebSocketContainer() {
", " WebSocketConfig webSocketConfig = new WebSocketConfig();", " when(container.getMaxTextMessageBufferSize()).thenReturn(100000);", " when(container.getMaxBinaryMessageBufferSize()).thenReturn(100000);", " ServletServerContainerFactoryBean result = webSocketConfig.createWebSocketContainer();", " assertSame(container, result);", " 
}

}
