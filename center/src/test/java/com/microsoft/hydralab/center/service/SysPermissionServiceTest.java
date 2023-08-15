package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.repository.RolePermissionRelationRepository;
import com.microsoft.hydralab.center.repository.SysPermissionRepository;
import com.microsoft.hydralab.common.entity.center.SysPermission;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import static org.mockito.Mockito.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;

@RunWith(MockitoJUnitRunner.class)
public class SysPermissionServiceTest {

    @Mock
    private SysPermissionRepository sysPermissionRepository;
    @Mock
    private RolePermissionRelationRepository permissionPermissionRelationRepository;
    private SysPermissionService sysPermissionService;
    private Map<String, SysPermission> permissionListMap;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        sysPermissionService = new SysPermissionService();
        sysPermissionService.sysPermissionRepository = sysPermissionRepository;
        sysPermissionService.permissionPermissionRelationRepository = permissionPermissionRelationRepository;
    }

    @Test
    public void testInitList() {
        List<SysPermission> permissionList = new ArrayList<>();
        SysPermission permission1 = new SysPermission();
        permission1.setPermissionId("1");
        permission1.setPermissionType("Type1");
        permission1.setPermissionContent("Content1");
        permission1.setCreateTime(new Date());
        permission1.setUpdateTime(new Date());
        permissionList.add(permission1);
        when(sysPermissionRepository.findAll()).thenReturn(permissionList);
        sysPermissionService.initList();
        verify(sysPermissionRepository, times(1)).findAll();
        verify(sysPermissionRepository, times(1)).save(permission1);
    }

    @Test
    public void testCreatePermission() {
        String permissionType = "type";
        String permissionContent = "content";
        SysPermission sysPermission = new SysPermission();
        sysPermission.setPermissionType(permissionType);
        sysPermission.setPermissionContent(permissionContent);
        sysPermission.setCreateTime(new Date());
        sysPermission.setUpdateTime(new Date());
        when(sysPermissionRepository.save(Mockito.any(SysPermission.class))).thenReturn(sysPermission);
        SysPermission result = sysPermissionService.createPermission(permissionType, permissionContent);
        assertEquals(sysPermission, result);
        assertEquals(sysPermission, permissionListMap.get(sysPermission.getPermissionId()));
    }

    @Test
    public void testUpdatePermission() {
        SysPermission sysPermission = new SysPermission();
        sysPermission.setPermissionId("1");
        sysPermission.setPermissionType("type");
        sysPermission.setPermissionContent("content");
        sysPermission.setCreateTime(new Date());
        sysPermission.setUpdateTime(new Date());
        when(sysPermissionRepository.save(Mockito.any(SysPermission.class))).thenReturn(sysPermission);
        SysPermission result = sysPermissionService.updatePermission(sysPermission);
        assertEquals(sysPermission, result);
    }

    @Test
    public void testQueryPermissionById() {
        String permissionId = "123";
        SysPermission expectedPermission = new SysPermission();
        expectedPermission.setPermissionId(permissionId);
        permissionListMap.put(permissionId, expectedPermission);
        when(sysPermissionRepository.findById(permissionId)).thenReturn(Optional.of(expectedPermission));
        SysPermission actualPermission = sysPermissionService.queryPermissionById(permissionId);
        assertEquals(expectedPermission, actualPermission);
        verify(sysPermissionRepository, times(1)).findById(permissionId);
    }

    @Test
    public void testQueryPermissionByContent() {
        String permissionContent = "testPermission";
        SysPermission expectedPermission = new SysPermission();
        expectedPermission.setPermissionContent(permissionContent);
        List<SysPermission> permissionList = new ArrayList<>();
        permissionList.add(expectedPermission);
        when(sysPermissionRepository.findByPermissionContent(permissionContent)).thenReturn(Optional.of(expectedPermission));
        SysPermission actualPermission = sysPermissionService.queryPermissionByContent(permissionContent);
        assertEquals(expectedPermission, actualPermission);
        verify(sysPermissionRepository, times(1)).findByPermissionContent(permissionContent);
    }

    @Test
    public void testDeletePermission() {
        SysPermission permission = new SysPermission();
        permission.setPermissionId("1");
        permission.setPermissionType("type");
        permission.setPermissionContent("content");
        sysPermissionService.deletePermission(permission);
        verify(sysPermissionRepository, times(1)).deleteById("1");
        verify(permissionPermissionRelationRepository, times(1)).deleteAllByPermissionId("1");
    }

}
