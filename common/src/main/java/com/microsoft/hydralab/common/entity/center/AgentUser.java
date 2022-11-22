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
    String versionName;
    String versionCode;
    int deviceType;
    int capabilities;
    int status;
    String role;
    @Column(name = "team_id")
    String teamId;
    String teamName;
    @Transient
    BatteryStrategy batteryStrategy;

    public interface DeviceType {
        int ANDROID = 1;
        int WINDOWS = 2;
    }

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
