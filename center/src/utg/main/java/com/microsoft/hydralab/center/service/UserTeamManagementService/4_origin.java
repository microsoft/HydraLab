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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public void setUp() {
        requestor = new SysUser();
        teamId = "team1";
    }

    @Test
    public void testCheckUserExistenceWithTeam_Exist() {
        // Arrange
        List<SysUser> users = new ArrayList<>();
        SysUser user1 = new SysUser();
        user1.setMailAddress("user1@example.com");
        users.add(user1);
        Mockito.when(userTeamManagementService.queryUsersByTeam(teamId)).thenReturn(users);

        // Act
        boolean result = userTeamManagementService.checkUserExistenceWithTeam(requestor, teamId);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testCheckUserExistenceWithTeam_NotExist() {
        // Arrange
        List<SysUser> users = new ArrayList<>();
        Mockito.when(userTeamManagementService.queryUsersByTeam(teamId)).thenReturn(users);

        // Act
        boolean result = userTeamManagementService.checkUserExistenceWithTeam(requestor, teamId);

        // Assert
        assertFalse(result);
    }
}