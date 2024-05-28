import com.microsoft.hydralab.center.repository.SysUserRepository;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

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
    public void testSwitchUserRole() {
        // Arrange
        SysUser user = new SysUser();
        user.setRoleId("oldRoleId");
        user.setRoleName("oldRoleName");
        String newRoleId = "newRoleId";
        String newRoleName = "newRoleName";
        when(sysUserRepository.save(user)).thenReturn(user);

        // Act
        SysUser result = sysUserService.switchUserRole(user, newRoleId, newRoleName);

        // Assert
        assertEquals(newRoleId, result.getRoleId());
        assertEquals(newRoleName, result.getRoleName());
        verify(sysUserRepository, times(1)).save(user);
    }
}