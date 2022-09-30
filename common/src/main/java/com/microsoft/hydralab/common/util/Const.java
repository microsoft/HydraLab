// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

public interface Const {
    interface Path {
        String AUTH = "/auth";
        String DEVICE_LIST = "/api/device/list";
        String DEVICE_UPDATE = "/api/device/update";
        String DEVICE_STATUS = "/api/device/status";
        String ACCESS_INFO = "/api/device/access";
        String TEST_TASK_RUN = "/api/test/task/run";
        String TEST_TASK_UPDATE = "/api/test/task/update";
        String TEST_TASK_CANCEL = "/api/test/task/cancel";
        String TEST_TASK_RETRY = "/api/test/task/retry";
        String AGENT_UPDATE = "/api/agent/update";
        String default_photo = "static/dist/images/default_user.png";
    }

    interface DeviceGroup {
        String CI = "CI";
        String REGULAR = "REGULAR";
        String groupPre = "G.";
        String sysGroup = "SYS";
        String userGroup = "USER";
        String singleType = "SINGLE";
        String restType = "REST";
        String allType = "ALL";
    }

    interface AgentConfig {
        int retry_time = 5;
        int photo_update_sec = 15;
        String task_id_param = "testTaskId";
        String serial_param = "serialNum";
        String status_param = "status";
        String scope_param = "isPrivate";
        String restartFileIOS = "restartAgent.sh";
        String restartFileWin = "restartAgent.bat";
    }

    interface TaskResult {
        String error_device_offline = "DEVICE_OFFLINE";
        String success = "SUCCESS";
    }

    interface Param {
        String TEST_DEVICE_SN = "devices";
        String GROUP = "groups";
        String AGENT = "agents";
        String TEST_TASK_ID = "testTaskId";
    }

    interface SmartTestConfig {
        String successTag = "success";
        String taskExpTag = "exception";
        String appExpTag = "appException";
        String coverageTag = "coverage";
        String visitTag = "visited";
        String bertPathTag = "path_to_screen_bert_model";
        String topicPathTag = "path_to_screen_topic_classifier_model";
        String sourceModelTag = "source_model_id";
        String targetModelTag = "target_model_id";

        String zipFileName = "SmartTest.zip";
        String zipFolderName = "SmartTest";
        String pyFileName = "main.py";
        String bertModelName = "screenBert.pt";
        String topicModelName = "topic.pt";
        String requireFileName = "requirements.txt";
        String stringFolderName = "SmartTestString";
        String stringFileNames = "strings,username,password";
    }

    interface ScreenRecoderConfig{
        String DEFAULT_FILE_NAME = "merged_test.mp4";
        String PC_FILE_NAME = "PC_test.mp4";
        String PHONE_FILE_NAME = "PHONE_test.mp4";
    }

    interface DeviceStability {
        String BEHAVIOUR_GO_ONLINE = "went ONLINE";
        String BEHAVIOUR_GO_OFFLINE = "went OFFLINE";
        String BEHAVIOUR_CONNECT = "connected";
        String BEHAVIOUR_DISCONNECT = "disconnected";
    }

    interface FontPath {
        String PREFIX_PATH = "/portal";
        String INDEX_PATH = "/portal/index.html";
        String ANCHOR = "#";
        String REDIRECT_PARAM = "redirectUrl";
    }

    interface RegexString {
        //uuid
        String UUID = "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}";
        //0-1,a-z,A-Z,_
        String COMMON_STR = "\\w*";
        // HTTP url  e.g. /api/auth
        String URL = "(/[A-Za-z0-9_.-]*)*";
        String MAIL_ADDRESS = "[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(.[a-zA-Z0-9_-]+)+$";

        //File path
        String LINUX_PATH = "^\\/([^\\/]+\\/?)*$";
        String WINDOWS_PATH = "^([a-zA-Z]:)(\\\\[^/\\\\:*?\"<>|]+\\\\?)*$";

        //Package name
        String PACKAGE_NAME = "[a-zA-Z]+[0-9a-zA-Z_]*(\\.[a-zA-Z]+[0-9a-zA-Z_]*)*";
    }

    interface PermissionType {
        String API = "API";
        String METHOD = "METHOD";
    }

    interface DefaultRole {
        String SUPER_ADMIN = "SUPER_ADMIN";
        String ADMIN = "ADMIN";
        String TEAM_ADMIN = "TEAM_ADMIN";
        String USER = "USER";
    }

    interface DefaultTeam {
        String DEFAULT_TEAM_NAME = "Default";
    }

    interface AUTH_COMPONENT {
        String DEFAULT_TEAM = "DEFAULT_TEAM";
        String TEAM = "TEAM";
        String ROLE = "ROLE";
        // permission
        String AUTHORITY = "AUTHORITY";
    }
}
