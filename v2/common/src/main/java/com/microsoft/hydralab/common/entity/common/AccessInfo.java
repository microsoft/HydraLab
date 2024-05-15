// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class AccessInfo {
    private String name;
    private String key;
    private Date ingestTime;

    public AccessInfo(String name){
        this.name = name;
        key = UUID.randomUUID().toString().replace("-","");
        ingestTime = new Date();
    }

    public interface TYPE{
        String GROUP = "GROUP";
        String DEVICE = "DEVICE";
    }
}
