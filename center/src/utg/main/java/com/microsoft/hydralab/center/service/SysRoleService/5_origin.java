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
    public void testDeleteRole() {
        // Arrange
        SysRole role = new SysRole();
        role.setRoleId("1");
        role.setRoleName("Test Role");
        role.setCreateTime(new Date());
        role.setUpdateTime(new Date());
        role.setAuthLevel(1);

        Map<String, SysRole> roleListMap = new ConcurrentHashMap<>();
        roleListMap.put(role.getRoleId(), role);

        Mockito.when(sysRoleRepository.findById(role.getRoleId())).thenReturn(java.util.Optional.of(role));

        // Act
        sysRoleService.deleteRole(role);

        // Assert
        assertNull(roleListMap.get(role.getRoleId()));
        Mockito.verify(sysRoleRepository, Mockito.times(1)).deleteById(role.getRoleId());
        Mockito.verify(rolePermissionRelationRepository, Mockito.times(1)).deleteAllByRoleId(role.getRoleId());
    }
}