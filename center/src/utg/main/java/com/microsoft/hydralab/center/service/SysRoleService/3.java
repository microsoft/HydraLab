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
 roleListMap = new ConcurrentHashMap<>(); sysRoleService = new SysRoleService(); sysRoleService.sysRoleRepository = sysRoleRepository; sysRoleService.rolePermissionRelationRepository = rolePermissionRelationRepository; sysRoleService.roleListMap = roleListMap; 
}

@Test
public void testQueryRoleById() {
 String roleId = "123"; SysRole expectedRole = new SysRole(); expectedRole.setRoleId(roleId); roleListMap.put(roleId, expectedRole); when(sysRoleRepository.findById(roleId)).thenReturn(Optional.of(expectedRole)); SysRole actualRole = sysRoleService.queryRoleById(roleId); assertEquals(expectedRole, actualRole); 
}

}
