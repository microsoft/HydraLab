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
import static org.junit.Assert.assertNull;

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
    public void testGetRequestorRole() {
        // Arrange
        SysUser requestor = new SysUser();
        requestor.setRoleId("role1");

        SysRole expectedRole = new SysRole();
        expectedRole.setRoleId("role1");
        expectedRole.setRoleName("Role 1");
        expectedRole.setAuthLevel(1);
        expectedRole.setCreateTime(new Date());
        expectedRole.setUpdateTime(new Date());

        Mockito.when(sysRoleRepository.findById("role1")).thenReturn(Optional.of(expectedRole));

        // Act
        SysRole actualRole = sysRoleService.getRequestorRole(requestor);

        // Assert
        assertEquals(expectedRole, actualRole);
    }
}