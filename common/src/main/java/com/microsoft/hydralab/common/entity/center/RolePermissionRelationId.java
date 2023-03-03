package com.microsoft.hydralab.common.entity.center;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class RolePermissionRelationId implements Serializable {
    private String roleId;
    private String permissionId;
}
