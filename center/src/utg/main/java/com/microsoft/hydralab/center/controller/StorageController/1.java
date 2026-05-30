import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import static org.mockito.Mockito.*;

public class StorageControllerTest {
private StorageController storageController;
@Mock
private HttpServletRequest request;
@Mock
private HttpServletResponse response;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); storageController = new StorageController(); 
}

@Test
public void testPostDownloadFile() {
 String fileUri = "example/file.txt"; File file = mock(File.class); when(request.getHeader("Authorization")).thenReturn("Bearer token"); when(storageController.storageTokenManageService.validateAccessToken("token")).thenReturn(true); when(LogUtils.isLegalStr(fileUri, Const.RegexString.STORAGE_FILE_REL_PATH, false)).thenReturn(true); when(new File(Const.LocalStorageURL.CENTER_LOCAL_STORAGE_ROOT + fileUri)).thenReturn(file); when(file.exists()).thenReturn(true); when(file.getName()).thenReturn("file.txt"); storageController.postDownloadFile(request, response, fileUri); verify(response).reset(); verify(response).setContentType("application/octet-stream"); verify(response).setCharacterEncoding("utf-8"); verify(response).setContentLength((int) file.length()); verify(response).setHeader("Content-Disposition", "attachment;filename=file.txt"); verify(response.getOutputStream(), times(1)).write(any(byte[].class), anyInt(), anyInt()); verify(storageController.logger).info(String.format("Output file: %s , size: %d!", fileUri, resLen)); 
}

}
