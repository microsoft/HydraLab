// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;

import java.time.Duration;
import java.util.List;

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
        Duration BLOCKED_DEVICE_TIMEOUT = Duration.ofHours(4);
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
        String RESULT_FOLDER_NAME = "smartTestResult";
        String GRAPH_FILE_NAME = "directed_acyclic_graph.gexf";
        String PY_FILE_NAME = "main.py";
        String BERT_MODEL_NAME = "screenBert.pt";
        String TOPIC_MODEL_NAME = "topic.pt";
        String REQUIRE_FILE_NAME = "requirements.txt";
        String STRING_FOLDER_NAME = "SmartTestString";
        String STRING_FILE_NAMES = "strings,username,password";
        String LLM_ENABLE = "enable_llm";
        String LLM_DEPLOYMENT = "deployment_name";
        String LLM_API_KEY = "openai_api_key";
        String LLM_API_BASE = "openai_api_base";
        String LLM_API_VERSION = "openai_api_version";
    }

    interface ScreenRecoderConfig {
        String DEFAULT_FILE_NAME = "merged_test.mp4";
        String PC_FILE_NAME = "PC_test.mp4";
        String PHONE_FILE_NAME = "PHONE_test.mp4";
    }

    interface PerformanceConfig {
        String DEFAULT_FILE_NAME = "PerformanceReport.json";
    }

    interface NetworkMonitorConfig {
        String AndroidDumpPath = "/Documents/dump.log";
        String DumpPath = "/network_dump.log";
        String ResultPath = "/network_test_result.xml";
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
        String SWAGGER_DOC_PATH = "/v3/api-docs";
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
        // There are almost no restrictions - apart from '/' and '\0', you're allowed to use anything. Hence the below is only an adoption for the most cases
        String LINUX_ABSOLUTE_PATH = "^(\\/[^\\t\\f\\n\\r\\v]+)+\\/?$";
        String WINDOWS_ABSOLUTE_PATH = "^([a-zA-Z]:)(\\\\[^/\\\\:*?\"<>|]+\\\\?)*$";
        String STORAGE_FILE_REL_PATH = "^([^\\/\\\\:*?\"<>|]+\\/)+[^\\/\\\\:*?\"<>|;]+$";

        //Package name
        String PACKAGE_NAME = "\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";

        //Integer
        String INTEGER = "^[0-9]*$";
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
        String LOCAL = "localStorageProperty";
        String AZURE = "azureBlobProperty";
    }

    interface AzureOpenaiConfig {
        String AZURE_OPENAI_CONFIG = "azureOpenaiConfig";
    }

    interface XCTestConfig{
        String XCTEST_ZIP_FOLDER_NAME = "Xctest";
        String XCTEST_RESULT_FILE_NAME = "result.xcresult";
    }

    final class LocalStorageURL {
        public static final String CENTER_LOCAL_STORAGE_UPLOAD = "/api/storage/local/upload";
        public static final String CENTER_LOCAL_STORAGE_DOWNLOAD = "/api/storage/local/download";
        public static final String CENTER_LOCAL_STORAGE_ROOT = "storage/local/";
    }

    final class LocalStorageConst {
        public static final List<String> PATH_PREFIX_LIST = List.of(LocalStorageURL.CENTER_LOCAL_STORAGE_UPLOAD, LocalStorageURL.CENTER_LOCAL_STORAGE_DOWNLOAD);
    }

    interface TestDeviceTag {
        String PRIMARY_PHONE = "PRIMARY_PHONE";
        String SECONDARY_PHONE = "SECONDARY_PHONE";
        String TERTIARY_PHONE = "TERTIARY_PHONE";
        String PRIMARY_PC = "PRIMARY_PC";
    }

    interface OperatedDevice {
        String AGENT = "Agent";
        String WINDOWS = "Windows";
        String ANDROID = "Android";
        String IOS = "iOS";
    }
}
