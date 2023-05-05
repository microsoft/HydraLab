// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.common.entity.agent.AgentFunctionAvailability;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(indexes = {@Index(columnList = "team_id")})
public class AgentUser {
    @Id
    private String id = UUID.randomUUID().toString();
    private String name;
    private String mailAddress;
    private String secret;
    private String hostname;
    private String ip;
    private String os;
    private String versionName;
    private String versionCode;
    private int status;
    private String role;
    @Column(name = "team_id")
    private String teamId;
    private String teamName;
    @Transient
    private BatteryStrategy batteryStrategy;

    @Transient
    private List<AgentFunctionAvailability> functionAvailabilities;

    public enum BatteryStrategy {
        /**
         * Strategy Name
         */
        Economic(-1, -1),
        Normal(120, 120),
        Aggressive(30, 30);

        public final int wakeUpInterval;
        public final int screenShotInterval;

        BatteryStrategy(int wakeUpInterval, int screenShotInterval) {
            this.wakeUpInterval = wakeUpInterval;
            this.screenShotInterval = screenShotInterval;
        }
    }
}
