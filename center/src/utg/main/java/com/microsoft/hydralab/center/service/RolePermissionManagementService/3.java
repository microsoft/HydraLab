import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.common.entity.center.RolePermissionRelation;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RolePermissionManagementServiceTest {
@Mock
private RolePermissionRelationRepository rolePermissionRelationRepository;
@InjectMocks
private RolePermissionManagementService rolePermissionManagementService;
@Before
public void setup() {
 SysPermission sysPermission = new SysPermission(); sysPermission.setId("permissionId"); sysPermission.setName("permissionName"); RolePermissionRelation rolePermissionRelation = new RolePermissionRelation(); rolePermissionRelation.setRoleId("roleId"); rolePermissionRelation.setPermissionId("permissionId"); when(rolePermissionRelationRepository.findByRoleIdAndPermissionId("roleId", "permissionId")) .thenReturn(Optional.of(rolePermissionRelation)); when(rolePermissionManagementService.queryRelation("roleId", "permissionId")) .thenReturn(rolePermissionRelation); 
}

@Test
public void testQueryRelation() {
 RolePermissionRelation result = rolePermissionManagementService.queryRelation("roleId", "permissionId"); assertEquals("roleId", result.getRoleId()); assertEquals("permissionId", result.getPermissionId()); 
}

}
