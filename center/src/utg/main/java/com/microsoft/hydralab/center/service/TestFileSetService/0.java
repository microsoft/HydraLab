import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class TestFileSetServiceTest {
@Mock
private TestFileSetRepository testFileSetRepository;
@Mock
private AttachmentService attachmentService;
@Mock
private StorageServiceClientProxy storageServiceClientProxy;
private TestFileSetService testFileSetService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); testFileSetService = new TestFileSetService(); testFileSetService.testFileSetRepository = testFileSetRepository; testFileSetService.attachmentService = attachmentService; testFileSetService.storageServiceClientProxy = storageServiceClientProxy; 
}

@Test
public void testAddTestFileSet() {
 TestFileSet testFileSet = new TestFileSet(); when(testFileSetRepository.save(testFileSet)).thenReturn(testFileSet); TestFileSet result = testFileSetService.addTestFileSet(testFileSet); verify(testFileSetRepository, times(1)).save(testFileSet); assertSame(testFileSet, result); 
}

}
