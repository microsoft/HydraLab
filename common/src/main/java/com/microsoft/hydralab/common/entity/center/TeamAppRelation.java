// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(name = "team_app_relation")
@IdClass(TeamAppRelationId.class)
public class TeamAppRelation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String teamId;
    @Id
    @Column(unique = true)
    private String appClientId;

    public TeamAppRelation(String teamId, String appClientId){
        this.teamId = teamId;
        this.appClientId = appClientId;
    }
}