import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.common.entity.center.RolePermissionRelation;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RolePermissionManagementServiceTest {

    @Mock
    private RolePermissionRelationRepository rolePermissionRelationRepository;

    @Mock
    private SysPermissionService sysPermissionService;

    @InjectMocks
    private RolePermissionManagementService rolePermissionManagementService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDeleteRolePermissionRelation() {
        RolePermissionRelation relation = new RolePermissionRelation("roleId", "permissionId");
        Set<SysPermission> permissions = new HashSet<>();
        permissions.add(new SysPermission("permissionId", "permissionName"));
        when(rolePermissionManagementService.queryPermissionsByRole("roleId")).thenReturn(new ArrayList<>(permissions));

        rolePermissionManagementService.deleteRolePermissionRelation(relation);

        verify(rolePermissionRelationRepository, times(1)).delete(relation);
        verify(sysPermissionService, times(1)).queryPermissionById("permissionId");
    }
}