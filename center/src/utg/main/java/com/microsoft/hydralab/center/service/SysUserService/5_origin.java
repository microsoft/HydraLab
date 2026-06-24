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
    public void testSwitchUserDefaultTeam() {
        // Arrange
        SysUser user = new SysUser();
        String defaultTeamId = "teamId";
        String defaultTeamName = "teamName";
        when(sysUserRepository.save(user)).thenReturn(user);

        // Act
        SysUser result = sysUserService.switchUserDefaultTeam(user, defaultTeamId, defaultTeamName);

        // Assert
        assertEquals(user, result);
        verify(sysUserRepository, times(1)).save(user);
    }
}