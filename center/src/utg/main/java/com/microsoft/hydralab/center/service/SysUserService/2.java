import com.microsoft.hydralab.center.repository.SysUserRepository;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SysUserServiceTest {
@Mock
private SysUserRepository sysUserRepository;
private SysUserService sysUserService;
@Before
public void setUp() {
 sysUserService = new SysUserService(); sysUserService.sysUserRepository = sysUserRepository; 
}

@Test
public void testQueryUserById() {
 String userId = "123"; SysUser expectedUser = new SysUser(); expectedUser.setUserId(userId); when(sysUserRepository.findById(userId)).thenReturn(java.util.Optional.of(expectedUser)); SysUser actualUser = sysUserService.queryUserById(userId); assertEquals(expectedUser, actualUser); 
}

}
