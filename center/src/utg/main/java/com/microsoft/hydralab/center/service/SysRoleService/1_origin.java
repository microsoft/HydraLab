import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.center.repository.SysRoleRepository;
import com.microsoft.hydralab.common.entity.center.SysRole;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SysRoleServiceTest {

    private SysRoleService sysRoleService;

    @Mock
    private SysRoleRepository sysRoleRepository;

    @Mock
    private RolePermissionRelationRepository rolePermissionRelationRepository;

    @Before
    public void setUp() {
        sysRoleService = new SysRoleService();
        sysRoleService.sysRoleRepository = sysRoleRepository;
        sysRoleService.rolePermissionRelationRepository = rolePermissionRelationRepository;
    }

    @Test
    public void testCreateRole() {
        // Arrange
        String roleName = "Test Role";
        int authLevel = 1;
        SysRole sysRole = new SysRole();
        sysRole.setRoleName(roleName);
        sysRole.setCreateTime(new Date());
        sysRole.setUpdateTime(new Date());
        sysRole.setAuthLevel(authLevel);

        when(sysRoleRepository.save(Mockito.any(SysRole.class))).thenReturn(sysRole);

        // Act
        SysRole result = sysRoleService.createRole(roleName, authLevel);

        // Assert
        assertEquals(sysRole, result);
    }
}