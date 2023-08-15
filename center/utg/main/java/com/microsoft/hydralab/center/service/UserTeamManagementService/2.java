import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

public class UserTeamManagementServiceTest {
@Mock
private UserTeamRelationRepository userTeamRelationRepository;
@Mock
private SysTeamService sysTeamService;
@Mock
private SysUserService sysUserService;
@Mock
private SysRoleService sysRoleService;
private UserTeamManagementService userTeamManagementService;
@Before
public void setup() {
 MockitoAnnotations.initMocks(this); userTeamManagementService = new UserTeamManagementService(); userTeamManagementService.userTeamRelationRepository = userTeamRelationRepository; userTeamManagementService.sysTeamService = sysTeamService; userTeamManagementService.sysUserService = sysUserService; userTeamManagementService.sysRoleService = sysRoleService; 
}

@Test
public void testDeleteUserTeamRelation() {
 UserTeamRelation relation = new UserTeamRelation("teamId", "mailAddress", true); userTeamManagementService.deleteUserTeamRelation(relation); verify(userTeamRelationRepository, times(1)).delete(relation); 
}

}
