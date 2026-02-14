package com.microsoft.hydralab.center.repository;

import com.microsoft.hydralab.common.entity.center.RolePermissionRelation;
import com.microsoft.hydralab.common.entity.center.RolePermissionRelationId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import static org.mockito.Mockito.verify;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class RolePermissionRelationRepositoryTest {

    @Mock
    private RolePermissionRelationRepository repository;
    @Mock
    private RolePermissionRelationRepository rolePermissionRelationRepository;
    private RolePermissionRelation rolePermissionRelation;
    private RolePermissionRelationId rolePermissionRelationId;

    @Before
    public void setUp() {
        rolePermissionRelation = new RolePermissionRelation();
        rolePermissionRelationId = new RolePermissionRelationId();
    }

    @Test
    public void testDeleteAllByPermissionId() {
        String permissionId = "testPermissionId";
        repository.deleteAllByPermissionId(permissionId);
        verify(repository).deleteAllByPermissionId(permissionId);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDeleteAllByRoleId() {
        String roleId = "testRoleId";
        rolePermissionRelationRepository.deleteAllByRoleId(roleId);
        verify(rolePermissionRelationRepository).deleteAllByRoleId(roleId);
    }
}