import com.microsoft.hydralab.center.repository.SysUserRepository;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SysUserServiceTest {

    @Mock
    private SysUserRepository sysUserRepository;

    private SysUserService sysUserService;

    @Before
    public void setUp() {
        sysUserService = new SysUserService();
        sysUserService.sysUserRepository = sysUserRepository;
    }

    @Test
    public void testQueryUserByMailAddress() {
        // Arrange
        String mailAddress = "test@example.com";
        SysUser expectedUser = new SysUser();
        expectedUser.setMailAddress(mailAddress);
        when(sysUserRepository.findByMailAddress(mailAddress)).thenReturn(Optional.of(expectedUser));

        // Act
        SysUser actualUser = sysUserService.queryUserByMailAddress(mailAddress);

        // Assert
        assertEquals(expectedUser, actualUser);
    }
}