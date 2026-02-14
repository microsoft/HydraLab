import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.common.entity.center.RolePermissionRelation;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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
 rolePermissionManagementService = new RolePermissionManagementService(); rolePermissionManagementService.rolePermissionRelationRepository = rolePermissionRelationRepository; rolePermissionManagementService.sysPermissionService = sysPermissionService; rolePermissionManagementService.sysRoleService = sysRoleService; 
}

@Test
public void testAddRolePermissionRelation() {
 String roleId = "role1"; String permissionId = "permission1"; RolePermissionRelation rolePermissionRelation = new RolePermissionRelation(roleId, permissionId); Set<SysPermission> permissions = new HashSet<>(); permissions.add(new SysPermission(permissionId)); when(sysPermissionService.queryPermissionById(permissionId)).thenReturn(new SysPermission(permissionId)); when(rolePermissionRelationRepository.save(rolePermissionRelation)).thenReturn(rolePermissionRelation); RolePermissionRelation result = rolePermissionManagementService.addRolePermissionRelation(roleId, permissionId); assertEquals(rolePermissionRelation, result); assertEquals(permissions, rolePermissionManagementService.rolePermissionListMap.get(roleId)); 
}

}
