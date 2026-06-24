import com.microsoft.hydralab.center.controller.StorageController;
import com.microsoft.hydralab.center.service.StorageTokenManageService;
import com.microsoft.hydralab.center.util.LocalStorageIOUtil;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.LogUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StorageControllerTest {

    @Mock
    private StorageTokenManageService storageTokenManageService;

    @Mock
    private Logger logger;

    @InjectMocks
    private StorageController storageController;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MultipartFile uploadedFile;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        uploadedFile = Mockito.mock(MultipartFile.class);
    }

    @Test
    public void testUploadFile() throws IOException {
        // Arrange
        String fileUri = "testFile.txt";
        String storageToken = "Bearer token";
        InputStream inputStream = Mockito.mock(InputStream.class);
        Result expectedResult = Result.ok(fileUri);

        when(request.getHeader("Authorization")).thenReturn(storageToken);
        when(storageTokenManageService.validateAccessToken(storageToken)).thenReturn(true);
        when(uploadedFile.getInputStream()).thenReturn(inputStream);

        // Act
        Result result = storageController.uploadFile(request, uploadedFile, fileUri);

        // Assert
        assertEquals(expectedResult, result);
    }
}