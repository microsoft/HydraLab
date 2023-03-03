// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

public interface Const {
    interface Path {
        String AUTH = "/auth";
        String AGENT_INIT = "/agentInit";
        String HEARTBEAT = "/heartbeat";
        String DEVICE_LIST = "/api/device/list";
        String DEVICE_UPDATE = "/api/device/update";
        String DEVICE_STATUS = "/api/device/status";
        String ACCESS_INFO = "/api/device/access";
        String TEST_TASK_RUN = "/api/test/task/run";
        String TEST_TASK_UPDATE = "/api/test/task/update";
        String TEST_TASK_CANCEL = "/api/test/task/cancel";
        String TEST_TASK_RETRY = "/api/test/task/retry";
        String AGENT_UPDATE = "/api/agent/update";
        String AGENT_RESTART = "/api/agent/restart";
        String DEFAULT_PHOTO = "static/dist/images/default_user.png";
    }

    interface DeviceGroup {
        String CI = "CI";
        String REGULAR = "REGULAR";
        String GROUP_NAME_PREFIX = "G.";
        String SYS_GROUP = "SYS";
        String USER_GROUP = "USER";
        String SINGLE_TYPE = "SINGLE";
        String REST_TYPE = "REST";
        String ALL_TYPE = "ALL";
    }

    interface AgentConfig {
        int RETRY_TIME = 5;
        int PHOTO_UPDATE_SEC = 15;
        String TASK_ID_PARAM = "testTaskId";
        String SERIAL_PARAM = "serialNum";
        String STATUS_PARAM = "status";
        String SCOPE_PARAM = "isPrivate";
        String RESTART_FILE_MAC = "restartAgent.sh";
        String RESTART_FILE_WIN = "restartAgent.bat";
    }

    interface TaskResult {
        String ERROR_DEVICE_OFFLINE = "DEVICE_OFFLINE";
        String SUCCESS = "SUCCESS";
    }

    interface Param {
        String TEST_DEVICE_SN = "devices";
        String GROUP = "groups";
        String AGENT = "agents";
        String TEST_TASK_ID = "testTaskId";
    }

    interface SmartTestConfig {
        String SUCCESS_TAG = "success";
        String TASK_EXP_TAG = "exception";
        String APP_EXP_TAG = "appException";
        String COVERAGE_TAG = "coverage";
        String VISIT_TAG = "visited";
        String BERT_PATH_TAG = "path_to_screen_bert_model";
        String TOPIC_PATH_TAG = "path_to_screen_topic_classifier_model";
        String SOURCE_MODEL_TAG = "source_model_id";
        String TARGET_MODEL_TAG = "target_model_id";

        String ZIP_FILE_NAME = "SmartTest.zip";
        String ZIP_FOLDER_NAME = "SmartTest";
        String PY_FILE_NAME = "main.py";
        String BERT_MODEL_NAME = "screenBert.pt";
        String TOPIC_MODEL_NAME = "topic.pt";
        String REQUIRE_FILE_NAME = "requirements.txt";
        String STRING_FOLDER_NAME = "SmartTestString";
        String STRING_FILE_NAMES = "strings,username,password";
        String XCTEST_ZIP_FILE_NAME = "Xctest.zip";
        String XCTEST_ZIP_FOLDER_NAME = "Xctest";
        String XCTEST_RESULT_FILE_NAME = "XctestResult";
    }

    interface ScreenRecoderConfig {
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

    interface FrontEndPath {
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
        String MAIL_ADDRESS = "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";

        //File path
        String LINUX_PATH = "^(\\/[\\w-]+)+\\/?$";
        String WINDOWS_PATH = "^([a-zA-Z]:)(\\\\[^/\\\\:*?\"<>|]+\\\\?)*$";

        //Package name
        String PACKAGE_NAME = "\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";
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

    interface AuthComponent {
        String DEFAULT_TEAM = "DEFAULT_TEAM";
        String TEAM = "TEAM";
        String ROLE = "ROLE";
        // permission
        String AUTHORITY = "AUTHORITY";
    }

    interface PreInstallFailurePolicy {
        String SHUTDOWN = "SHUTDOWN";
        String IGNORE = "IGNORE";
    }

    interface FilePermission {
        String WRITE = "WRITE";
        String READ = "READ";
    }

    interface StorageType {
        String LOCAL = "LOCAL";
        String AZURE = "AZURE";
    }

    interface StoragePropertyBean {
        String LOCAL = "localProperty";
        String AZURE = "azureBlobProperty";
    }
}
