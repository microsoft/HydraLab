package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.center.repository.SysRoleRepository;
import com.microsoft.hydralab.common.entity.center.SysRole;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class SysRoleServiceTest {

    @Mock
    private SysRoleRepository sysRoleRepository;
    @Mock
    private RolePermissionRelationRepository rolePermissionRelationRepository;
    private SysRoleService sysRoleService;
    private Map<String, SysRole> roleListMap;

    @Before
    public void setup() {
        sysRoleService = new SysRoleService();
        sysRoleService.sysRoleRepository = sysRoleRepository;
        sysRoleService.rolePermissionRelationRepository = rolePermissionRelationRepository;
    }

    @Test
    public void testInitList() {
        List<SysRole> roleList = new ArrayList<>();
        SysRole role1 = new SysRole();
        role1.setRoleId("1");
        role1.setRoleName("Role 1");
        role1.setAuthLevel(1);
        roleList.add(role1);
        SysRole role2 = new SysRole();
        role2.setRoleId("2");
        role2.setRoleName("Role 2");
        role2.setAuthLevel(2);
        roleList.add(role2);
        Mockito.when(sysRoleRepository.findAll()).thenReturn(roleList);
        sysRoleService.initList();
        Map<String, SysRole> roleListMap = sysRoleService.roleListMap;
        assert roleListMap.containsKey("1");
        assert roleListMap.containsKey("2");
        assert roleListMap.get("1").getRoleName().equals("Role 1");
        assert roleListMap.get("2").getRoleName().equals("Role 2");
    }

    @Before
    public void setUp() {
        sysRoleService = new SysRoleService();
        sysRoleService.sysRoleRepository = sysRoleRepository;
        sysRoleService.rolePermissionRelationRepository = rolePermissionRelationRepository;
    }

    @Test
    public void testCreateRole() {
        String roleName = "Test Role";
        int authLevel = 1;
        SysRole sysRole = new SysRole();
        sysRole.setRoleName(roleName);
        sysRole.setCreateTime(new Date());
        sysRole.setUpdateTime(new Date());
        sysRole.setAuthLevel(authLevel);
        Mockito.when(sysRoleRepository.save(Mockito.any(SysRole.class))).thenReturn(sysRole);
        SysRole result = sysRoleService.createRole(roleName, authLevel);
        assertEquals(sysRole, result);
    }

    @Test
    public void testUpdateRole() {
        SysRole sysRole = new SysRole();
        sysRole.setRoleId("1");
        sysRole.setRoleName("Admin");
        sysRole.setAuthLevel(1);
        sysRole.setCreateTime(new Date());
        sysRole.setUpdateTime(new Date());
        Mockito.when(sysRoleRepository.save(Mockito.any(SysRole.class))).thenReturn(sysRole);
        SysRole result = sysRoleService.updateRole(sysRole);
        assertEquals(sysRole, result);
    }

    @Test
    public void testQueryRoleById() {
        String roleId = "123";
        SysRole expectedRole = new SysRole();
        expectedRole.setRoleId(roleId);
        roleListMap.put(roleId, expectedRole);
        Mockito.when(sysRoleRepository.findById(roleId)).thenReturn(Optional.of(expectedRole));
        SysRole actualRole = sysRoleService.queryRoleById(roleId);
        assertEquals(expectedRole, actualRole);
    }

    @Test
    public void testQueryRoleByName() {
        String roleName = "admin";
        SysRole expectedRole = new SysRole();
        expectedRole.setRoleId("1");
        expectedRole.setRoleName(roleName);
        expectedRole.setCreateTime(new Date());
        expectedRole.setUpdateTime(new Date());
        expectedRole.setAuthLevel(1);
        List<SysRole> roleList = new ArrayList<>();
        roleList.add(expectedRole);
        Mockito.when(sysRoleRepository.findByRoleName(roleName)).thenReturn(Optional.of(expectedRole));
        SysRole actualRole = sysRoleService.queryRoleByName(roleName);
        assertEquals(expectedRole, actualRole);
    }

    @Test
    public void testDeleteRole() {
        SysRole role = new SysRole();
        role.setRoleId("1");
        role.setRoleName("Test Role");
        role.setCreateTime(new Date());
        role.setUpdateTime(new Date());
        role.setAuthLevel(1);
        Map<String, SysRole> roleListMap = new ConcurrentHashMap<>();
        roleListMap.put(role.getRoleId(), role);
        Mockito.when(sysRoleRepository.findById(role.getRoleId())).thenReturn(java.util.Optional.of(role));
        sysRoleService.deleteRole(role);
        assertNull(roleListMap.get(role.getRoleId()));
        Mockito.verify(sysRoleRepository, Mockito.times(1)).deleteById(role.getRoleId());
        Mockito.verify(rolePermissionRelationRepository, Mockito.times(1)).deleteAllByRoleId(role.getRoleId());
    }

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

    @Test
    public void testIsAuthLevelSuperior() {
        SysRole aRole = new SysRole();
        aRole.setAuthLevel(2);
        aRole.setRoleId("1");
        aRole.setRoleName("Role1");
        aRole.setCreateTime(new Date());
        aRole.setUpdateTime(new Date());
        SysRole bRole = new SysRole();
        bRole.setAuthLevel(3);
        bRole.setRoleId("2");
        bRole.setRoleName("Role2");
        bRole.setCreateTime(new Date());
        bRole.setUpdateTime(new Date());
        boolean result = sysRoleService.isAuthLevelSuperior(aRole, bRole);
        assertFalse(result);
    }

    @Test
    public void testGetRequestorRole() {
        SysUser requestor = new SysUser();
        requestor.setRoleId("role1");
        SysRole expectedRole = new SysRole();
        expectedRole.setRoleId("role1");
        expectedRole.setRoleName("Role 1");
        expectedRole.setAuthLevel(1);
        expectedRole.setCreateTime(new Date());
        expectedRole.setUpdateTime(new Date());
        Mockito.when(sysRoleRepository.findById("role1")).thenReturn(Optional.of(expectedRole));
        SysRole actualRole = sysRoleService.getRequestorRole(requestor);
        assertEquals(expectedRole, actualRole);
    }

    @Test
    public void testGetOrCreateDefaultRole() {
        String roleName = "Test Role";
        int authLevel = 1;
        SysRole expectedRole = new SysRole();
        expectedRole.setRoleName(roleName);
        expectedRole.setAuthLevel(authLevel);
        expectedRole.setCreateTime(new Date());
        expectedRole.setUpdateTime(new Date());
        Mockito.when(sysRoleRepository.findByRoleName(roleName)).thenReturn(Optional.empty());
        Mockito.when(sysRoleRepository.save(Mockito.any(SysRole.class))).thenReturn(expectedRole);
        SysRole actualRole = sysRoleService.getOrCreateDefaultRole(roleName, authLevel);
        assertEquals(expectedRole, actualRole);
    }
}