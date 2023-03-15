// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;

import java.util.List;

public interface Const {
    final class Path {
        public static final String AUTH = "/auth";
        public static final String AGENT_INIT = "/agentInit";
        public static final String HEARTBEAT = "/heartbeat";
        public static final String DEVICE_LIST = "/api/device/list";
        public static final String DEVICE_UPDATE = "/api/device/update";
        public static final String DEVICE_STATUS = "/api/device/status";
        public static final String ACCESS_INFO = "/api/device/access";
        public static final String TEST_TASK_RUN = "/api/test/task/run";
        public static final String TEST_TASK_UPDATE = "/api/test/task/update";
        public static final String TEST_TASK_CANCEL = "/api/test/task/cancel";
        public static final String TEST_TASK_RETRY = "/api/test/task/retry";
        public static final String AGENT_UPDATE = "/api/agent/update";
        public static final String AGENT_RESTART = "/api/agent/restart";
        public static final String DEFAULT_PHOTO = "static/dist/images/default_user.png";

        private Path() {
        }
    }

    final class DeviceGroup {
        public static final String CI = "CI";
        public static final String REGULAR = "REGULAR";
        public static final String GROUP_NAME_PREFIX = "G.";
        public static final String SYS_GROUP = "SYS";
        public static final String USER_GROUP = "USER";
        public static final String SINGLE_TYPE = "SINGLE";
        public static final String REST_TYPE = "REST";
        public static final String ALL_TYPE = "ALL";

        private DeviceGroup() {
        }
    }

    final class AgentConfig {
        public static final int RETRY_TIME = 5;
        public static final int PHOTO_UPDATE_SEC = 15;
        public static final String TASK_ID_PARAM = "testTaskId";
        public static final String SERIAL_PARAM = "serialNum";
        public static final String STATUS_PARAM = "status";
        public static final String SCOPE_PARAM = "isPrivate";
        public static final String RESTART_FILE_MAC = "restartAgent.sh";
        public static final String RESTART_FILE_WIN = "restartAgent.bat";

        private AgentConfig() {
        }
    }

    final class TaskResult {
        public static final String ERROR_DEVICE_OFFLINE = "DEVICE_OFFLINE";
        public static final String SUCCESS = "SUCCESS";

        private TaskResult() {
        }
    }

    final class Param {
        public static final String TEST_DEVICE_SN = "devices";
        public static final String GROUP = "groups";
        public static final String AGENT = "agents";
        public static final String TEST_TASK_ID = "testTaskId";

        private Param() {
        }
    }

    final class SmartTestConfig {
        public static final String SUCCESS_TAG = "success";
        public static final String TASK_EXP_TAG = "exception";
        public static final String APP_EXP_TAG = "appException";
        public static final String COVERAGE_TAG = "coverage";
        public static final String VISIT_TAG = "visited";
        public static final String BERT_PATH_TAG = "path_to_screen_bert_model";
        public static final String TOPIC_PATH_TAG = "path_to_screen_topic_classifier_model";
        public static final String SOURCE_MODEL_TAG = "source_model_id";
        public static final String TARGET_MODEL_TAG = "target_model_id";

        public static final String ZIP_FILE_NAME = "SmartTest.zip";
        public static final String ZIP_FOLDER_NAME = "SmartTest";
        public static final String PY_FILE_NAME = "main.py";
        public static final String BERT_MODEL_NAME = "screenBert.pt";
        public static final String TOPIC_MODEL_NAME = "topic.pt";
        public static final String REQUIRE_FILE_NAME = "requirements.txt";
        public static final String STRING_FOLDER_NAME = "SmartTestString";
        public static final String STRING_FILE_NAMES = "strings,username,password";

        private SmartTestConfig() {
        }
    }

    final class ScreenRecoderConfig {
        public static final String DEFAULT_FILE_NAME = "merged_test.mp4";
        public static final String PC_FILE_NAME = "PC_test.mp4";
        public static final String PHONE_FILE_NAME = "PHONE_test.mp4";

        private ScreenRecoderConfig() {
        }
    }

    final class DeviceStability {
        public static final String BEHAVIOUR_GO_ONLINE = "went ONLINE";
        public static final String BEHAVIOUR_GO_OFFLINE = "went OFFLINE";
        public static final String BEHAVIOUR_CONNECT = "connected";
        public static final String BEHAVIOUR_DISCONNECT = "disconnected";

        private DeviceStability() {
        }
    }

    final class FrontEndPath {
        public static final String PREFIX_PATH = "/portal";
        public static final String INDEX_PATH = "/portal/index.html";
        public static final String ANCHOR = "#";
        public static final String REDIRECT_PARAM = "redirectUrl";

        private FrontEndPath() {
        }
    }

    final class RegexString {
        //uuid
        public static final String UUID = "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}";
        //0-1,a-z,A-Z,_
        public static final String COMMON_STR = "\\w*";
        // HTTP url  e.g. /api/auth
        public static final String URL = "(/[A-Za-z0-9_.-]*)*";
        public static final String MAIL_ADDRESS = "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";

        //File path
        // There are almost no restrictions - apart from '/' and '\0', you're allowed to use anything. Hence the below is only an adoption for the most cases
        public static final String LINUX_ABSOLUTE_PATH = "^(\\/[^\\t\\f\\n\\r\\v]+)+\\/?$";
        public static final String WINDOWS_ABSOLUTE_PATH = "^([a-zA-Z]:)(\\\\[^/\\\\:*?\"<>|]+\\\\?)*$";
        public static final String STORAGE_FILE_REL_PATH = "^[^\\/\\\\:*?\"<>|]+(\\/[^\\/\\\\:*?\"<>|]+)+$";

        //Package name
        public static final String PACKAGE_NAME = "\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";

        private RegexString() {
        }
    }

    final class PermissionType {
        public static final String API = "API";
        public static final String METHOD = "METHOD";

        private PermissionType() {
        }
    }

    final class DefaultRole {
        public static final String SUPER_ADMIN = "SUPER_ADMIN";
        public static final String ADMIN = "ADMIN";
        public static final String TEAM_ADMIN = "TEAM_ADMIN";
        public static final String USER = "USER";

        private DefaultRole() {
        }
    }

    final class DefaultTeam {
        public static final String DEFAULT_TEAM_NAME = "Default";

        private DefaultTeam() {
        }
    }

    final class AuthComponent {
        public static final String DEFAULT_TEAM = "DEFAULT_TEAM";
        public static final String TEAM = "TEAM";
        public static final String ROLE = "ROLE";
        // permission
        public static final String AUTHORITY = "AUTHORITY";

        private AuthComponent() {
        }
    }

    final class PreInstallFailurePolicy {
        public static final String SHUTDOWN = "SHUTDOWN";
        public static final String IGNORE = "IGNORE";

        private PreInstallFailurePolicy() {
        }
    }

    final class FilePermission {
        public static final String WRITE = "WRITE";
        public static final String READ = "READ";

        private FilePermission() {
        }
    }

    final class StorageType {
        public static final String LOCAL = "LOCAL";
        public static final String AZURE = "AZURE";

        private StorageType() {
        }
    }

    final class StoragePropertyBean {
        public static final String LOCAL = "localStorageProperty";
        public static final String AZURE = "azureBlobProperty";

        private StoragePropertyBean() {
        }
    }

    final class LocalStorageURL {
        public static final String CENTER_LOCAL_STORAGE_UPLOAD = "/api/storage/local/upload";
        public static final String CENTER_LOCAL_STORAGE_DOWNLOAD = "/api/storage/local/download";
        public static final String CENTER_LOCAL_STORAGE_ROOT = "storage/local/";

        private LocalStorageURL() {

        }
    }

    final class LocalStorageConst {
        public static final List<String> PATH_PREFIX_LIST = List.of(LocalStorageURL.CENTER_LOCAL_STORAGE_UPLOAD, LocalStorageURL.CENTER_LOCAL_STORAGE_DOWNLOAD);

        private LocalStorageConst() {

        }
    }
}
