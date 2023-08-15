import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class SysRoleServiceTest {

    @Mock
    private SysRoleRepository sysRoleRepository;

    @Mock
    private RolePermissionRelationRepository rolePermissionRelationRepository;

    @Test
    public void testIsAuthLevelValid() {
        SysRoleService sysRoleService = new SysRoleService();
        sysRoleService.sysRoleRepository = sysRoleRepository;
        sysRoleService.rolePermissionRelationRepository = rolePermissionRelationRepository;

        int validAuthLevel = 5;
        int invalidAuthLevel = -1;

        boolean isValid = sysRoleService.isAuthLevelValid(validAuthLevel);
        assertTrue(isValid);

        boolean isInvalid = sysRoleService.isAuthLevelValid(invalidAuthLevel);
        assertFalse(isInvalid);
    }
}