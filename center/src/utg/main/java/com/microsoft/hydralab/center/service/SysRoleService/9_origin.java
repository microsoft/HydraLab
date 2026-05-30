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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SysRoleServiceTest {

    @Mock
    private SysRoleRepository sysRoleRepository;

    @Mock
    private RolePermissionRelationRepository rolePermissionRelationRepository;

    private SysRoleService sysRoleService;

    @Before
    public void setup() {
        sysRoleService = new SysRoleService();
        sysRoleService.sysRoleRepository = sysRoleRepository;
        sysRoleService.rolePermissionRelationRepository = rolePermissionRelationRepository;
    }

    @Test
    public void testGetOrCreateDefaultRole() {
        // Arrange
        String roleName = "Test Role";
        int authLevel = 1;
        SysRole expectedRole = new SysRole();
        expectedRole.setRoleName(roleName);
        expectedRole.setAuthLevel(authLevel);
        expectedRole.setCreateTime(new Date());
        expectedRole.setUpdateTime(new Date());

        when(sysRoleRepository.findByRoleName(roleName)).thenReturn(Optional.empty());
        when(sysRoleRepository.save(Mockito.any(SysRole.class))).thenReturn(expectedRole);

        // Act
        SysRole actualRole = sysRoleService.getOrCreateDefaultRole(roleName, authLevel);

        // Assert
        assertEquals(expectedRole, actualRole);
    }
}