import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class TestFileSetServiceTest {
@Mock
private TestFileSetRepository testFileSetRepository;
private TestFileSetService testFileSetService;
@Before
public void setup() {
 testFileSetService = new TestFileSetService(); testFileSetService.testFileSetRepository = testFileSetRepository; 
}

@Test
public void testUpdateFileSetTeam() {
 String teamId = "teamId"; String teamName = "teamName"; List<TestFileSet> testFileSets = new ArrayList<>(); TestFileSet testFileSet1 = new TestFileSet(); testFileSet1.setTeamId(teamId); testFileSets.add(testFileSet1); TestFileSet testFileSet2 = new TestFileSet(); testFileSet2.setTeamId(teamId); testFileSets.add(testFileSet2); Mockito.when(testFileSetRepository.findAllByTeamId(teamId)).thenReturn(testFileSets); testFileSetService.updateFileSetTeam(teamId, teamName); Mockito.verify(testFileSetRepository, Mockito.times(1)).saveAll(testFileSets); 
}

}
