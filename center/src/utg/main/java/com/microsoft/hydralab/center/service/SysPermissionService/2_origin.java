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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SysPermissionServiceTest {
    private SysPermissionService sysPermissionService;
    @Mock
    private SysPermissionRepository sysPermissionRepository;
    @Mock
    private RolePermissionRelationRepository permissionPermissionRelationRepository;
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
    public void testUpdatePermission() {
        // Arrange
        SysPermission sysPermission = new SysPermission();
        sysPermission.setPermissionId("1");
        sysPermission.setPermissionType("type");
        sysPermission.setPermissionContent("content");
        sysPermission.setCreateTime(new Date());
        sysPermission.setUpdateTime(new Date());
        
        when(sysPermissionRepository.save(Mockito.any(SysPermission.class))).thenReturn(sysPermission);
        
        // Act
        SysPermission result = sysPermissionService.updatePermission(sysPermission);
        
        // Assert
        assertEquals(sysPermission, result);
    }
}