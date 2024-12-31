import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.util.Const;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StorageTokenManageServiceTest {

    @Mock
    private StorageServiceClientProxy storageServiceClientProxy;

    private StorageTokenManageService storageTokenManageService;

    private ConcurrentMap<String, AccessToken> accessTokenMap;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        storageTokenManageService = new StorageTokenManageService();
        storageTokenManageService.storageServiceClientProxy = storageServiceClientProxy;
        accessTokenMap = new ConcurrentHashMap<>();
        storageTokenManageService.accessTokenMap = accessTokenMap;
    }

    @Test
    public void testGenerateWriteToken() {
        // Arrange
        String uniqueId = "testUniqueId";
        AccessToken accessToken = new AccessToken();
        accessTokenMap.put(uniqueId, accessToken);

        // Mock the behavior of storageServiceClientProxy
        Mockito.when(storageServiceClientProxy.isAccessTokenExpired(accessToken)).thenReturn(false);
        Mockito.when(storageServiceClientProxy.generateAccessToken(Const.FilePermission.WRITE)).thenReturn(accessToken);

        // Act
        AccessToken result = storageTokenManageService.generateWriteToken(uniqueId);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(accessToken, result);
    }
}