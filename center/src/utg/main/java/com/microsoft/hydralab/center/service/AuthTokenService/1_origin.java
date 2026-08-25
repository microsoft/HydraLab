import com.microsoft.hydralab.center.repository.AuthTokenRepository;
import com.microsoft.hydralab.common.entity.center.AuthToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthTokenServiceTest {

    @Mock
    private AuthTokenRepository authTokenRepository;

    @InjectMocks
    private AuthTokenService authTokenService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAuthToken() {
        // Arrange
        Long tokenId = 1L;
        AuthToken authToken = new AuthToken();
        authToken.setId(tokenId);
        Optional<AuthToken> optionalAuthToken = Optional.of(authToken);
        when(authTokenRepository.findById(tokenId)).thenReturn(optionalAuthToken);

        // Act
        AuthToken result = authTokenService.getAuthToken(tokenId);

        // Assert
        assertEquals(authToken, result);
    }
}