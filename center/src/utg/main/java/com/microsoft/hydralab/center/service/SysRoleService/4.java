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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class SysRoleServiceTest {
@Mock
private SysRoleRepository sysRoleRepository;
@Mock
private RolePermissionRelationRepository rolePermissionRelationRepository;
private SysRoleService sysRoleService;
@Before
public void setup() {
 sysRoleService = new SysRoleService(); sysRoleService.sysRoleRepository = sysRoleRepository; sysRoleService.rolePermissionRelationRepository = rolePermissionRelationRepository; 
}

@Test
public void testQueryRoleByName() {
 String roleName = "admin"; SysRole expectedRole = new SysRole(); expectedRole.setRoleId("1"); expectedRole.setRoleName(roleName); expectedRole.setCreateTime(new Date()); expectedRole.setUpdateTime(new Date()); expectedRole.setAuthLevel(1); List<SysRole> roleList = new ArrayList<>(); roleList.add(expectedRole); Mockito.when(sysRoleRepository.findByRoleName(roleName)).thenReturn(Optional.of(expectedRole)); SysRole actualRole = sysRoleService.queryRoleByName(roleName); assertEquals(expectedRole, actualRole); 
}

}
