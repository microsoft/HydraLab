package com.microsoft.hydralab.common.entity.center;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(name = "role_permission_relation")
@IdClass(RolePermissionRelationId.class)
public class RolePermissionRelation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String roleId;
    @Id
    private String permissionId;

    public RolePermissionRelation(String roleId, String permissionId){
        this.roleId = roleId;
        this.permissionId = permissionId;
    }
}
