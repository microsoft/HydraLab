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
public void testCheckUserTeamRelation() {
 String mailAddress = "test@example.com"; String teamId = "12345"; UserTeamRelation userTeamRelation = new UserTeamRelation(teamId, mailAddress, false); Mockito.when(userTeamRelationRepository.findByMailAddressAndTeamId(mailAddress, teamId)).thenReturn(Optional.of(userTeamRelation)); boolean result = userTeamManagementService.checkUserTeamRelation(mailAddress, teamId); assertTrue(result); 
}

}
