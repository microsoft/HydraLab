import com.microsoft.hydralab.center.repository.SysUserRepository;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class SysUserServiceTest {
@Mock
private SysUserRepository sysUserRepository;
private SysUserService sysUserService;
@Before
public void setUp() {
 MockitoAnnotations.initMocks(this); sysUserService = new SysUserService(); sysUserService.sysUserRepository = sysUserRepository; 
}

@Test
public void testUpdateUser() {
 SysUser sysUser = new SysUser(); sysUser.setUserName("testUser"); sysUser.setMailAddress("test@mail.com"); sysUser.setRoleId("1"); sysUser.setRoleName("Test Role"); when(sysUserRepository.save(sysUser)).thenReturn(sysUser); SysUser updatedUser = sysUserService.updateUser(sysUser); assertEquals(sysUser, updatedUser); 
}

}
