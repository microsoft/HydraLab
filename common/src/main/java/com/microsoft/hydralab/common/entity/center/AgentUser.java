// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.center;

import lombok.Data;

import javax.persistence.*;
import java.util.UUID;

@Data
@Entity
@Table(indexes = {@Index(columnList = "team_id")})
public class AgentUser {
    @Id
    String id = UUID.randomUUID().toString();
    String name;
    String mailAddress;
    String secret;
    String hostname;
    String ip;
    String os;
    String version;
    int deviceType;
    int capabilities;
    int status;
    String role;
    @Column(name = "team_id")
    String teamId;
    String teamName;

    public interface DeviceType {
        int ANDROID = 1;
        int WINDOWS = 2;
    }

    public interface Status {
        int DISABLED = 0;
        int ENABLED = 1;
    }

    public interface Role {
        String USER = "USER";
        String AGENT = "AGENT";
        String ADMIN = "ADMIN";
    }
}
