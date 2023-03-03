// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.center;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_team_relation")
@IdClass(UserTeamRelationId.class)
public class UserTeamRelation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String teamId;
    @Id
    private String mailAddress;
    private boolean isTeamAdmin;

    public UserTeamRelation(String teamId, String mailAddress, boolean isTeamAdmin) {
        this.teamId = teamId;
        this.mailAddress = mailAddress;
        this.isTeamAdmin = isTeamAdmin;
    }
}