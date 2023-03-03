// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
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
    private int deviceType;
    private int capabilities;
    private int status;
    private String role;
    @Column(name = "team_id")
    private String teamId;
    private String teamName;
    @Transient
    private BatteryStrategy batteryStrategy;

    public static final class DeviceType {
        public static final int ANDROID = 1;
        public static final int WINDOWS = 2;
    }

    public enum BatteryStrategy {
        /**
         * Strategy Name
         */
        Economic(-1, -1),
        Normal(120, 120),
        Aggressive(30, 30);

        private final int wakeUpInterval;
        private final int screenShotInterval;

        BatteryStrategy(int wakeUpInterval, int screenShotInterval) {
            this.wakeUpInterval = wakeUpInterval;
            this.screenShotInterval = screenShotInterval;
        }

        public int getWakeUpInterval() {
            return wakeUpInterval;
        }

        public int getScreenShotInterval() {
            return screenShotInterval;
        }
    }
}
