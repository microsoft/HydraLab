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
    private StorageFileInfo packageInfo;
    private List<UpdateMsg> updateMsgs = new ArrayList<>();

    public static final class TaskConst {
        public static final String STATUS_UPDATING = "UPDATING";
        public static final String STATUS_FAIL = "FAIL";
        public static final String STATUS_SUCCESS = "SUCCESS";
        public static final String STATUS_NONE = "NONE";
        public static final String PARAM_VERSION_NAME = "versionName";
        public static final String PARAM_VERSION_CODE = "versionCode";
        public static final String PROPERTY_PATH = "BOOT-INF/classes/version.properties";
        public static final String PROPERTY_VERSION_NAME = "agent.version";
        public static final String PROPERTY_VERSION_CODE = "agent.versionCode";

    }

    public static class UpdateMsg {
        private Boolean isProceed;
        private String message;
        private String errorDesc;
        private Date recordTime;

        public UpdateMsg(Boolean isProceed, String message, String errorDesc) {
            recordTime = new Date();
            this.isProceed = isProceed;
            this.message = message;
            this.errorDesc = errorDesc;
        }

        public Boolean isProceed() {
            return isProceed;
        }

        public String getMessage() {
            return message;
        }

        public String getErrorDesc() {
            return errorDesc;
        }

        public Date getRecordTime() {
            return recordTime;
        }
    }

}
