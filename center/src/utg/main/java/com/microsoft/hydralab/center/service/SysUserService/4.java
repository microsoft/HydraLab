import com.microsoft.hydralab.center.repository.SysUserRepository;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.util.Optional;

public class SysUserServiceTest {
@Mock
private SysUserRepository sysUserRepository;
private SysUserService sysUserService;
@Before
public void setUp() {
 MockitoAnnotations.initMocks(this); sysUserService = new SysUserService(); sysUserService.sysUserRepository = sysUserRepository; 
}

@Test
public void testCheckUserExistenceWithRole() {
 String roleId = "role1"; int count = 1; Mockito.when(sysUserRepository.countByRoleId(roleId)).thenReturn(count); boolean result = sysUserService.checkUserExistenceWithRole(roleId); Assert.assertTrue(result); Mockito.verify(sysUserRepository, Mockito.times(1)).countByRoleId(roleId); 
}

}
