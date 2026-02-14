import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.center.repository.SysPermissionRepository;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
        sysPermissionService = new SysPermissionService(sysPermissionRepository, permissionPermissionRelationRepository);
    }

    @Test
    public void testInitList() {
        // Arrange
        List<SysPermission> permissionList = new ArrayList<>();
        SysPermission permission1 = new SysPermission();
        permission1.setPermissionId("1");
        permission1.setPermissionType("Type1");
        permission1.setPermissionContent("Content1");
        permission1.setCreateTime(new Date());
        permission1.setUpdateTime(new Date());
        permissionList.add(permission1);
        when(sysPermissionRepository.findAll()).thenReturn(permissionList);

        // Act
        sysPermissionService.initList();

        // Assert
        verify(sysPermissionRepository, times(1)).findAll();
        verify(sysPermissionRepository, times(1)).save(permission1);
    }
}