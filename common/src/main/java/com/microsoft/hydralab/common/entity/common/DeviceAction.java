// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author zhoule
 */
@Entity
@Table(name = "device_action")
@Data
public class DeviceAction {
    @Id
    @Column(name = "id", nullable = false)
    private String id = UUID.randomUUID().toString();
    @Column(name = "test_task_id")
    private String testTaskId;
    private String timingToAct;
    private String actionType;
    private int actionOrder;
    @Convert(converter = ActionArgConverter.class)
    private List<String> actionArgs = new ArrayList<>();


    public interface Timing {
        String SET_UP = "setUp";
        String TEAR_DOWN = "tearDown";
    }
}