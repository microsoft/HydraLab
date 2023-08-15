package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.SysUserRepository;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Optional;
import org.junit.Assert;
import static org.mockito.Mockito.*;
import com.microsoft.hydralab.common.util.Const;
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
    private SysUserService sysUserService;
    @Mock
    private Authentication authentication;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        sysUserService = new SysUserService();
        sysUserService.sysUserRepository = sysUserRepository;
    }

    @Test
    public void testCreateUserWithDefaultRole() {
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
        SysUser result = sysUserService.createUserWithDefaultRole(userName, mailAddress, defaultRoleId, defaultRoleName);
        assertEquals(sysUser, result);
    }

    @Test
    public void testUpdateUser() {
        SysUser sysUser = new SysUser();
        sysUser.setUserName("testUser");
        sysUser.setMailAddress("test@mail.com");
        sysUser.setRoleId("1");
        sysUser.setRoleName("Test Role");
        when(sysUserRepository.save(sysUser)).thenReturn(sysUser);
        SysUser updatedUser = sysUserService.updateUser(sysUser);
        assertEquals(sysUser, updatedUser);
    }

    @Test
    public void testQueryUserById() {
        String userId = "123";
        SysUser expectedUser = new SysUser();
        expectedUser.setUserId(userId);
        when(sysUserRepository.findById(userId)).thenReturn(Optional.of(expectedUser));
        SysUser actualUser = sysUserService.queryUserById(userId);
        assertEquals(expectedUser, actualUser);
    }

    @Test
    public void testQueryUserByMailAddress() {
        String mailAddress = "test@example.com";
        SysUser expectedUser = new SysUser();
        expectedUser.setMailAddress(mailAddress);
        when(sysUserRepository.findByMailAddress(mailAddress)).thenReturn(Optional.of(expectedUser));
        SysUser actualUser = sysUserService.queryUserByMailAddress(mailAddress);
        assertEquals(expectedUser, actualUser);
    }

    @Test
    public void testCheckUserExistenceWithRole() {
        String roleId = "role1";
        int count = 1;
        Mockito.when(sysUserRepository.countByRoleId(roleId)).thenReturn(count);
        boolean result = sysUserService.checkUserExistenceWithRole(roleId);
        Assert.assertTrue(result);
        Mockito.verify(sysUserRepository, Mockito.times(1)).countByRoleId(roleId);
    }

    @Test
    public void testSwitchUserDefaultTeam() {
        SysUser user = new SysUser();
        String defaultTeamId = "teamId";
        String defaultTeamName = "teamName";
        when(sysUserRepository.save(user)).thenReturn(user);
        SysUser result = sysUserService.switchUserDefaultTeam(user, defaultTeamId, defaultTeamName);
        assertEquals(user, result);
        verify(sysUserRepository, times(1)).save(user);
    }

    @Test
    public void testSwitchUserRole() {
        SysUser user = new SysUser();
        user.setRoleId("oldRoleId");
        user.setRoleName("oldRoleName");
        String newRoleId = "newRoleId";
        String newRoleName = "newRoleName";
        when(sysUserRepository.save(user)).thenReturn(user);
        SysUser result = sysUserService.switchUserRole(user, newRoleId, newRoleName);
        assertEquals(newRoleId, result.getRoleId());
        assertEquals(newRoleName, result.getRoleName());
        verify(sysUserRepository, times(1)).save(user);
    }

    @Test
    public void testCheckUserRole() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return "ROLE_ADMIN";
            }
        });
        Mockito.when(authentication.getAuthorities()).thenReturn(authorities);
        assertTrue(sysUserService.checkUserRole(authentication, "ROLE_ADMIN"));
        assertFalse(sysUserService.checkUserRole(authentication, "ROLE_USER"));
    }

    @Test
    public void testCheckUserAdmin() {
        Authentication auth = Mockito.mock(Authentication.class);
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return Const.DefaultRole.SUPER_ADMIN;
            }
        });
        Mockito.when(auth.getAuthorities()).thenReturn(authorities);
        assertTrue(sysUserService.checkUserAdmin(auth));
        authorities.clear();
        authorities.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return Const.DefaultRole.ADMIN;
            }
        });
        Mockito.when(auth.getAuthorities()).thenReturn(authorities);
        assertTrue(sysUserService.checkUserAdmin(auth));
        authorities.clear();
        authorities.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return "ROLE_USER";
            }
        });
        Mockito.when(auth.getAuthorities()).thenReturn(authorities);
        assertFalse(sysUserService.checkUserAdmin(auth));
    }

}