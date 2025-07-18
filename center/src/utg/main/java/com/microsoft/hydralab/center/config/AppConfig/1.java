import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

@RunWith(MockitoJUnitRunner.class)
public class AppConfigTest {
@Mock
private ApplicationContext applicationContext;
@Test
public void testStorageServiceClientProxy() {
 AppConfig appConfig = new AppConfig(); appConfig.storageServiceClientProxy(applicationContext); 
}

}
