import com.microsoft.hydralab.center.repository.UserTeamRelationRepository;
import com.microsoft.hydralab.center.service.SysRoleService;
import com.microsoft.hydralab.center.service.SysTeamService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.center.UserTeamRelation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class UserTeamManagementServiceTest {
@Mock
private UserTeamRelationRepository userTeamRelationRepository;
@Mock
private SysTeamService sysTeamService;
@Mock
private SysUserService sysUserService;
@Mock
private SysRoleService sysRoleService;
@InjectMocks
private UserTeamManagementService userTeamManagementService;
private SysUser requestor;
private String teamId;
@Before
public void setup() {
 requestor = new SysUser(); teamId = "team1"; 
}

@Test
public void testCheckRequestorTeamAdmin_WhenRequestorIsTeamAdmin_ReturnsTrue() {
 requestor.setTeamAdminMap(Map.of(teamId, true)); boolean result = userTeamManagementService.checkRequestorTeamAdmin(requestor, teamId); assertTrue(result); 
}

@Test
public void testCheckRequestorTeamAdmin_WhenRequestorIsNotTeamAdmin_ReturnsFalse() {
 requestor.setTeamAdminMap(Map.of(teamId, false)); boolean result = userTeamManagementService.checkRequestorTeamAdmin(requestor, teamId); assertFalse(result); 
}

}
