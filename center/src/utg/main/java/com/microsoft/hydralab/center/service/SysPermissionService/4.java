import com.microsoft.hydralab.center.repository.SysPermissionRepository;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SysPermissionServiceTest {
@Mock
private SysPermissionRepository sysPermissionRepository;
@InjectMocks
private SysPermissionService sysPermissionService;
private Map<String, SysPermission> permissionListMap;
@Before
public void setUp() {
 permissionListMap = new ConcurrentHashMap<>(); MockitoAnnotations.initMocks(this); 
}

@Test
public void testQueryPermissionByContent() {
 String permissionContent = "testPermission"; SysPermission expectedPermission = new SysPermission(); expectedPermission.setPermissionContent(permissionContent); List<SysPermission> permissionList = new ArrayList<>(); permissionList.add(expectedPermission); when(sysPermissionRepository.findByPermissionContent(permissionContent)).thenReturn(Optional.of(expectedPermission)); SysPermission actualPermission = sysPermissionService.queryPermissionByContent(permissionContent); assertEquals(expectedPermission, actualPermission); verify(sysPermissionRepository, times(1)).findByPermissionContent(permissionContent); 
}

}
