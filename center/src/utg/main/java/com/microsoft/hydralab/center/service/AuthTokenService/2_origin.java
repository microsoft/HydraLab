import com.microsoft.hydralab.center.repository.AuthTokenRepository;
import com.microsoft.hydralab.common.entity.center.AuthToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class AuthTokenServiceTest {
    @Mock
    private AuthTokenRepository authTokenRepository;
    
    private AuthTokenService authTokenService;
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        authTokenService = new AuthTokenService();
        authTokenService.authTokenRepository = authTokenRepository;
    }
    
    @Test
    public void testDeleteAuthToken() {
        AuthToken authToken = new AuthToken();
        
        authTokenService.deleteAuthToken(authToken);
        
        verify(authTokenRepository, times(1)).delete(authToken);
    }
}