import com.microsoft.hydralab.center.repository.SysTeamRepository;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SysTeamServiceTest {
@Mock
private SysTeamRepository sysTeamRepository;
@InjectMocks
private SysTeamService sysTeamService;
private Map<String, SysTeam> teamListMap;
@Before
public void setUp() {
 teamListMap = new ConcurrentHashMap<>(); MockitoAnnotations.initMocks(this); 
}

@Test
public void testQueryTeamById() {
 String teamId = "1"; SysTeam team = new SysTeam(); team.setTeamId(teamId); teamListMap.put(teamId, team); when(sysTeamRepository.findById(teamId)).thenReturn(Optional.of(team)); SysTeam result = sysTeamService.queryTeamById(teamId); assertEquals(team, result); 
}

}
