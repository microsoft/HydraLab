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
        MockitoAnnotations.initMocks(this);
        sysUserService = new SysUserService();
        sysUserService.sysUserRepository = sysUserRepository;
    }

    @Test
    public void testCreateUserWithDefaultRole() {
        // Arrange
        String userName = "John";
        String mailAddress = "john@example.com";
        String defaultRoleId = "123";
        String defaultRoleName = "User";

        SysUser sysUser = new SysUser();
        sysUser.setUserName(userName);
        sysUser.setMailAddress(mailAddress);
        sysUser.setRoleId(defaultRoleId);
        sysUser.setRoleName(defaultRoleName);

        when(sysUserRepository.save(sysUser)).thenReturn(sysUser);

        // Act
        SysUser result = sysUserService.createUserWithDefaultRole(userName, mailAddress, defaultRoleId, defaultRoleName);

        // Assert
        assertEquals(sysUser, result);
    }
}