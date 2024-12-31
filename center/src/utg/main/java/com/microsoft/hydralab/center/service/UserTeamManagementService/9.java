import com.microsoft.hydralab.center.repository.UserTeamRelationRepository;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserTeamManagementServiceTest {
@Mock
private UserTeamRelationRepository userTeamRelationRepository;
@InjectMocks
private UserTeamManagementService userTeamManagementService;
private SysTeam team;
@Before
public void setUp() {
 team = new SysTeam(); team.setTeamId("teamId"); 
}

@Test
public void testDeleteTeam() {
 userTeamManagementService.deleteTeam(team); Mockito.verify(userTeamRelationRepository).findAllByTeamId("teamId"); Mockito.verify(userTeamManagementService).deleteUserTeamRelation(Mockito.any()); Mockito.verify(userTeamManagementService).sysTeamService.deleteTeam(team); 
}

}
