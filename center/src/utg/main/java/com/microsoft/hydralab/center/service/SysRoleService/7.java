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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class SysRoleServiceTest {
@Mock
private SysRoleRepository sysRoleRepository;
@Mock
private RolePermissionRelationRepository rolePermissionRelationRepository;
private SysRoleService sysRoleService;
@Before
public void setUp() {
 sysRoleService = new SysRoleService(); sysRoleService.sysRoleRepository = sysRoleRepository; sysRoleService.rolePermissionRelationRepository = rolePermissionRelationRepository; sysRoleService.roleListMap = new ConcurrentHashMap<>(); 
}

@Test
public void testIsAuthLevelSuperior() {
 SysRole aRole = new SysRole(); aRole.setAuthLevel(2); aRole.setRoleId("1"); aRole.setRoleName("Role1"); aRole.setCreateTime(new Date()); aRole.setUpdateTime(new Date()); SysRole bRole = new SysRole(); bRole.setAuthLevel(3); bRole.setRoleId("2"); bRole.setRoleName("Role2"); bRole.setCreateTime(new Date()); bRole.setUpdateTime(new Date()); boolean result = sysRoleService.isAuthLevelSuperior(aRole, bRole); assertFalse(result); 
}

}
