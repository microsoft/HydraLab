import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.common.entity.center.RolePermissionRelation;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RolePermissionManagementServiceTest {
@Mock
private RolePermissionRelationRepository rolePermissionRelationRepository;
@Mock
private SysPermissionService sysPermissionService;
@Mock
private SysRoleService sysRoleService;
private RolePermissionManagementService rolePermissionManagementService;
@Before
public void setUp() {
 MockitoAnnotations.initMocks(this); rolePermissionManagementService = new RolePermissionManagementService(); rolePermissionManagementService.rolePermissionRelationRepository = rolePermissionRelationRepository; rolePermissionManagementService.sysPermissionService = sysPermissionService; rolePermissionManagementService.sysRoleService = sysRoleService; 
}

@Test
public void testInitList() {
 RolePermissionRelation relation1 = new RolePermissionRelation("role1", "permission1"); RolePermissionRelation relation2 = new RolePermissionRelation("role2", "permission2"); List<RolePermissionRelation> relationList = List.of(relation1, relation2); SysPermission permission1 = new SysPermission("permission1"); SysPermission permission2 = new SysPermission("permission2"); when(rolePermissionRelationRepository.findAll()).thenReturn(relationList); when(sysPermissionService.queryPermissionById("permission1")).thenReturn(permission1); when(sysPermissionService.queryPermissionById("permission2")).thenReturn(permission2); rolePermissionManagementService.initList(); Map<String, Set<SysPermission>> rolePermissionListMap = rolePermissionManagementService.rolePermissionListMap; assertEquals(2, rolePermissionListMap.size()); Set<SysPermission> role1Permissions = rolePermissionListMap.get("role1"); assertEquals(1, role1Permissions.size()); assertEquals(permission1, role1Permissions.iterator().next()); Set<SysPermission> role2Permissions = rolePermissionListMap.get("role2"); assertEquals(1, role2Permissions.size()); assertEquals(permission2, role2Permissions.iterator().next()); verify(rolePermissionRelationRepository, times(1)).findAll(); verify(sysPermissionService, times(2)).queryPermissionById(anyString()); 
}

}
