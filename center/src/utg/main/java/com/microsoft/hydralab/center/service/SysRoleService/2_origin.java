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
    private SysRoleService sysRoleService;
    @Mock
    private SysRoleRepository sysRoleRepository;
    @Mock
    private RolePermissionRelationRepository rolePermissionRelationRepository;
    private Map<String, SysRole> roleListMap;
    
    @Before
    public void setUp() {
        roleListMap = new ConcurrentHashMap<>();
        sysRoleService = new SysRoleService();
        sysRoleService.sysRoleRepository = sysRoleRepository;
        sysRoleService.rolePermissionRelationRepository = rolePermissionRelationRepository;
        sysRoleService.roleListMap = roleListMap;
    }
    
    @Test
    public void testUpdateRole() {
        // Arrange
        SysRole sysRole = new SysRole();
        sysRole.setRoleId("1");
        sysRole.setRoleName("Admin");
        sysRole.setAuthLevel(1);
        sysRole.setCreateTime(new Date());
        sysRole.setUpdateTime(new Date());
        
        when(sysRoleRepository.save(Mockito.any(SysRole.class))).thenReturn(sysRole);
        
        // Act
        SysRole result = sysRoleService.updateRole(sysRole);
        
        // Assert
        assertEquals(sysRole, result);
    }
}