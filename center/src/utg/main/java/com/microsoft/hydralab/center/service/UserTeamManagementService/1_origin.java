import com.microsoft.hydralab.center.repository.UserTeamRelationRepository;
import com.microsoft.hydralab.center.service.SysRoleService;
import com.microsoft.hydralab.center.service.SysTeamService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.common.entity.center.SysRole;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.center.UserTeamRelation;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
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
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userTeamManagementService = new UserTeamManagementService();
        userTeamManagementService.setUserTeamRelationRepository(userTeamRelationRepository);
        userTeamManagementService.setSysTeamService(sysTeamService);
        userTeamManagementService.setSysUserService(sysUserService);
        userTeamManagementService.setSysRoleService(sysRoleService);
    }

    @Test
    public void testAddUserTeamRelation() {
        // Arrange
        String teamId = "team1";
        SysUser user = new SysUser();
        user.setMailAddress("user1@example.com");
        boolean isTeamAdmin = true;

        SysTeam team = new SysTeam();
        team.setTeamId(teamId);

        when(sysTeamService.queryTeamById(teamId)).thenReturn(team);

        // Act
        UserTeamRelation result = userTeamManagementService.addUserTeamRelation(teamId, user, isTeamAdmin);

        // Assert
        assertEquals(teamId, result.getTeamId());
        assertEquals(user.getMailAddress(), result.getMailAddress());
        assertEquals(isTeamAdmin, result.isTeamAdmin());
        verify(userTeamRelationRepository, times(1)).save(result);
    }
}