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
import java.util.Collection;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class SysUserServiceTest {
@Mock
private SysUserRepository sysUserRepository;
@Mock
private Authentication authentication;
private SysUserService sysUserService;
@Before
public void setup() {
 sysUserService = new SysUserService(); sysUserService.sysUserRepository = sysUserRepository; 
}

@Test
public void testCheckUserRole() {
 Collection<GrantedAuthority> authorities = new ArrayList<>(); authorities.add(new GrantedAuthority() { @Override public String getAuthority() { return "ROLE_ADMIN"; } }); Mockito.when(authentication.getAuthorities()).thenReturn(authorities); assertTrue(sysUserService.checkUserRole(authentication, "ROLE_ADMIN")); assertFalse(sysUserService.checkUserRole(authentication, "ROLE_USER")); 
}

}
