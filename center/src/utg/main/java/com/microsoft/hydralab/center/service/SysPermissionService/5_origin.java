import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.center.repository.SysPermissionRepository;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Map;
import static org.mockito.Mockito.*;

public class SysPermissionServiceTest {
    @Mock
    private SysPermissionRepository sysPermissionRepository;
    @Mock
    private RolePermissionRelationRepository permissionPermissionRelationRepository;
    private SysPermissionService sysPermissionService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        sysPermissionService = new SysPermissionService();
        sysPermissionService.sysPermissionRepository = sysPermissionRepository;
        sysPermissionService.permissionPermissionRelationRepository = permissionPermissionRelationRepository;
    }

    @Test
    public void testDeletePermission() {
        SysPermission permission = new SysPermission();
        permission.setPermissionId("1");
        permission.setPermissionType("type");
        permission.setPermissionContent("content");

        sysPermissionService.deletePermission(permission);

        verify(sysPermissionRepository, times(1)).deleteById("1");
        verify(permissionPermissionRelationRepository, times(1)).deleteAllByPermissionId("1");
    }
}