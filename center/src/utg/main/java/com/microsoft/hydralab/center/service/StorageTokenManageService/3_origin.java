import com.microsoft.hydralab.common.util.Const;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StorageTokenManageServiceTest {

    @Mock
    private StorageServiceClientProxy storageServiceClientProxy;

    private StorageTokenManageService storageTokenManageService;

    @Before
    public void setUp() {
        storageTokenManageService = new StorageTokenManageService();
        storageTokenManageService.storageServiceClientProxy = storageServiceClientProxy;
    }

    @Test
    public void testValidateTokenVal() {
        String token = "exampleToken";
        boolean expectedResult = true;

        boolean actualResult = storageTokenManageService.validateTokenVal(token);

        Assert.assertEquals(expectedResult, actualResult);
    }
}