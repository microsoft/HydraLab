import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.util.Const;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

public class StorageTokenManageServiceTest {
    private StorageTokenManageService storageTokenManageService;

    @Mock
    private StorageServiceClientProxy storageServiceClientProxy;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        storageTokenManageService = new StorageTokenManageService();
        storageTokenManageService.storageServiceClientProxy = storageServiceClientProxy;
    }

    @Test
    public void testValidateAccessToken() {
        String accessToken = "validAccessToken";
        boolean expected = true;

        boolean actual = storageTokenManageService.validateAccessToken(accessToken);

        Assert.assertEquals(expected, actual);
    }
}