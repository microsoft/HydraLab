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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
    public void testInitList() {
        // Arrange
        List<SysRole> roleList = new ArrayList<>();
        SysRole role1 = new SysRole();
        role1.setRoleId("1");
        role1.setRoleName("Role 1");
        role1.setAuthLevel(1);
        roleList.add(role1);
        SysRole role2 = new SysRole();
        role2.setRoleId("2");
        role2.setRoleName("Role 2");
        role2.setAuthLevel(2);
        roleList.add(role2);
        Mockito.when(sysRoleRepository.findAll()).thenReturn(roleList);
        // Act
        sysRoleService.initList();
        // Assert
        Map<String, SysRole> roleListMap = sysRoleService.roleListMap;
        assert roleListMap.containsKey("1");
        assert roleListMap.containsKey("2");
        assert roleListMap.get("1").getRoleName().equals("Role 1");
        assert roleListMap.get("2").getRoleName().equals("Role 2");
    }
}