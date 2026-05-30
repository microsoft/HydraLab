import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.center.repository.SysPermissionRepository;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SysPermissionServiceTest {
    @Mock
    private SysPermissionRepository sysPermissionRepository;
    @Mock
    private RolePermissionRelationRepository permissionPermissionRelationRepository;

    private SysPermissionService sysPermissionService;
    private Map<String, SysPermission> permissionListMap;

    @Before
    public void setUp() {
        permissionListMap = new ConcurrentHashMap<>();
        sysPermissionService = new SysPermissionService();
        sysPermissionService.sysPermissionRepository = sysPermissionRepository;
        sysPermissionService.permissionPermissionRelationRepository = permissionPermissionRelationRepository;
        sysPermissionService.permissionListMap = permissionListMap;
    }

    @Test
    public void testCreatePermission() {
        String permissionType = "type";
        String permissionContent = "content";

        SysPermission sysPermission = new SysPermission();
        sysPermission.setPermissionType(permissionType);
        sysPermission.setPermissionContent(permissionContent);
        sysPermission.setCreateTime(new Date());
        sysPermission.setUpdateTime(new Date());

        when(sysPermissionRepository.save(Mockito.any(SysPermission.class))).thenReturn(sysPermission);

        SysPermission result = sysPermissionService.createPermission(permissionType, permissionContent);

        assertEquals(sysPermission, result);
        assertEquals(sysPermission, permissionListMap.get(sysPermission.getPermissionId()));
    }
}