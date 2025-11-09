import com.microsoft.hydralab.center.repository.SysUserRepository;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.util.Const;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
public void testCheckUserAdmin() {
 Authentication auth = Mockito.mock(Authentication.class); List<GrantedAuthority> authorities = new ArrayList<>(); authorities.add(new GrantedAuthority() { @Override public String getAuthority() { return Const.DefaultRole.SUPER_ADMIN; } }); Mockito.when(auth.getAuthorities()).thenReturn(authorities); assertTrue(sysUserService.checkUserAdmin(auth)); authorities.clear(); authorities.add(new GrantedAuthority() { @Override public String getAuthority() { return Const.DefaultRole.ADMIN; } }); Mockito.when(auth.getAuthorities()).thenReturn(authorities); assertTrue(sysUserService.checkUserAdmin(auth)); authorities.clear(); authorities.add(new GrantedAuthority() { @Override public String getAuthority() { return "ROLE_USER"; } }); Mockito.when(auth.getAuthorities()).thenReturn(authorities); assertFalse(sysUserService.checkUserAdmin(auth)); 
}

}
