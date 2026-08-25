import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.*;

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
public void setUp() {
 MockitoAnnotations.initMocks(this); userTeamManagementService = new UserTeamManagementService(); userTeamManagementService.userTeamRelationRepository = userTeamRelationRepository; userTeamManagementService.sysTeamService = sysTeamService; userTeamManagementService.sysUserService = sysUserService; userTeamManagementService.sysRoleService = sysRoleService; 
}

@Test
public void testCheckTeamAdmin() {
 String teamId = "123"; String mailAddress = "test@example.com"; Mockito.when(userTeamManagementService.checkTeamAdmin(teamId, mailAddress)).thenReturn(true); boolean result = userTeamManagementService.checkTeamAdmin(teamId, mailAddress); assertTrue(result); 
}

}
