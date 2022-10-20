// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class AgentUpdateTask {
    private String agentId;
    private String agentName;
    private String updateStatus;
    private String originVersionName;
    private String targetVersionName;
    private String originVersionCode;
    private String targetVersionCode;
    private BlobFileInfo packageInfo;
    private List<UpdateMsg> updateMsgs = new ArrayList<>();


    public interface TaskConst {
        String STATUS_UPDATING = "UPDATING";
        String STATUS_FAIL = "FAIL";
        String STATUS_SUCCESS = "SUCCESS";
        String STATUS_NONE = "NONE";
        String PARAM_VERSION_NAME = "versionName";
        String PARAM_VERSION_CODE = "versionCode";
        String PROPERTY_PATH = "BOOT-INF/classes/version.properties";
        String PROPERTY_VERSION_NAME = "agent.version";
        String PROPERTY_VERSION_CODE = "agent.versionCode";

    }

    public static class UpdateMsg {
        public Boolean isProceed;
        public String message;
        public String errorDesc;
        public Date recordTime;

        public UpdateMsg(Boolean isProceed, String message, String errorDesc) {
            recordTime = new Date();
            this.isProceed = isProceed;
            this.message = message;
            this.errorDesc = errorDesc;
        }
    }

}
