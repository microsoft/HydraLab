// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.openai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.openai.data.ChatMessage;
import com.microsoft.hydralab.center.openai.data.ChatRequest;
import com.microsoft.hydralab.center.openai.data.ExceptionSuggestion;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.TestDataService;
import com.microsoft.hydralab.center.service.TestFileSetService;
import com.microsoft.hydralab.center.service.TestTaskService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.common.entity.agent.AgentFunctionAvailability;
import com.microsoft.hydralab.common.entity.center.AgentDeviceGroup;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.center.TestTaskQueuedInfo;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.TestFileSet;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import com.microsoft.hydralab.common.file.impl.AzureOpenaiConfig;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.util.AttachmentService;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.performance.InspectionStrategy;
import com.microsoft.hydralab.performance.PerformanceInspection;
import com.microsoft.hydralab.performance.PerformanceInspector;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author zhoule
 * @date 09/26/2023
 */
@Service
public class ChatQAService {
    private final Logger logger = LoggerFactory.getLogger(ChatQAService.class);
    // session pool  id:queue
    private static Map<String, List<ChatMessage>> sessionPool = new HashMap<>();

    // file pool
    private static Map<String, File> filePool = new HashMap<>();

    private AzureOpenAIServiceClient oaiClient = null;

    @Resource
    TestDataService testDataService;
    @Resource
    AttachmentService attachmentService;
    @Resource
    TestFileSetService testFileSetService;
    @Resource
    DeviceAgentManagementService deviceAgentManagementService;
    @Resource
    TestTaskService testTaskService;
    @Resource
    UserTeamManagementService userTeamManagementService;
    @Resource
    SysUserService sysUserService;
    @Resource
    SuggestionService suggestionService;

    static final String INIT_PROMPT = "You are an AI assistant that helps system determine what type of task the user wants to perform and " +
            "whether all of the required parameters of a task are provided. You need to return the result in JSONObject format and do not add any other information." +
            "There are 4 keys in the JSONObject.\n" +
            "1.taskType: the task of the user wants to perform, if you can't determine please set as none.\n" +
            "2.requiredParams: the name and value of required parameters like deviceTestCount:10. " +
            "The value can be filled with the default value that is provided. The value could not be null or empty. \n" +
            "3.checkRes:whether all of the required parameters are provided values, fill it with true or false. If the size of required parameters is 0, fill it with true\n" +
            "4.message: the content you want to talk to user in markdown format. You need to summarize the parameters:" +
            " which is provided, which is default and which has not provided. The top level of markdown title should be 4. " +
            "There tasks are listed as below:\n" + getAllUserIntentionType() +
            "By the way, the appFile should be provided by system role and other value should be provided by user role.\n" +
            "Don’t extrapolate values from thin air, if you don't know the value fill it with null.\n" +
            "You can ask the user questions to determine the user's intention and ask the user to provide the information you need to complete the task.";
    static final String ANALYSIS_RESULT_PROMPT = "You are an AI assistant that can analysis the report of TestTask. " +
            "I will provide you the JSON format data and you need to transfer it to a human readable markdown format. " +
            "The top level of markdown title should be 4." +
            "You need to tell me the front page of the report(reportPage), the test was run in which device(deviceName), " +
            "how many round tested(the length of testUnitList), the test cost time of each round(displaySpentTime), " +
            "how about the test result(success) for each round, and the fileName and download url(cdnUrl) of the attachments. The TestTask is ";

    public ChatQAService(ApplicationContext applicationContext) {
        // Azure openai client
        AzureOpenaiConfig openaiConfig = applicationContext.getBean(Const.AzureOpenaiConfig.AZURE_OPENAI_CONFIG, AzureOpenaiConfig.class);
        if (openaiConfig.getApiKey() != null && openaiConfig.getDeployment() != null && openaiConfig.getEndpoint() != null) {
            this.oaiClient = new AzureOpenAIServiceClient(
                    openaiConfig.getApiKey(),
                    openaiConfig.getDeployment(),
                    openaiConfig.getEndpoint());
        }
    }

    // create session
    public String createSession() {
        String sessionID = UUID.randomUUID().toString().replace("-", "");
        sessionPool.put(sessionID, new ArrayList<>());
        sessionPool.get(sessionID).add(
                new ChatMessage(ChatMessage.Role.SYSTEM, INIT_PROMPT));
        sessionPool.get(sessionID).add(
                new ChatMessage(ChatMessage.Role.ASSISTANT,
                        "{\n  \"taskType\": \"none\",\n  \"checkRes\": false,\n  \"requiredParams\": {},\n  " +
                                "\"message\": \"What task would you like to perform? You can choose from RUN, QUERY, ANALYSIS_PERFORMANCE, or DEVICE.\"\n}"
                ));
        return sessionID;
    }

    // delete session
    public void deleteSession(String id) {
        sessionPool.remove(id);
    }

    //is session exist
    public boolean isSessionExist(String id) {
        return sessionPool.containsKey(id);
    }

    // ask question
    public ChatQAResult askQuestion(SysUser requestor, String role, String sessionId, String question) {
        sessionPool.get(sessionId).add(new ChatMessage(role, question));
        ChatRequest request = new ChatRequest();
        request.setMessages(sessionPool.get(sessionId));
        request.setTemperature(0);
        request.setTopP(0.95);
        String answer = oaiClient.chatCompletion(request);
        String answerText = JSONObject.parseObject(answer).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        logger.info("The answer is " + answerText);
        JSONObject answerJson = null;
        ChatQAResult result = new ChatQAResult();
        result.setSessionId(sessionId);
        try {
            answerJson = JSONObject.parseObject(answerText);
            result.setSuccess(true);
            result.setMessage(answerJson.getString("message"));
        } catch (Exception e) {
            logger.error("The answer is not in the correct format!");
            result.setSuccess(false);
            result.setMessage("The answer is not in the correct format! " + answerText);
            return result;
        }
        sessionPool.get(sessionId).add(new ChatMessage(ChatMessage.Role.ASSISTANT, answerJson.toString()));
        if (answerJson.getBoolean("checkRes")) {
            UserIntentionType executeType = UserIntentionType.convertToType(answerJson.getString("taskType"));
            switch (Objects.requireNonNull(executeType)) {
                case RUN:
                    result = triggerTestTask(requestor, sessionId, answerJson.getJSONObject("requiredParams"));
                    break;
                case QUERY:
                    result = queryResult(requestor, sessionId, answerJson.getJSONObject("requiredParams"));
                    break;
                case ANALYSIS_PERFORMANCE:
                    result = analysisPref(requestor, sessionId, answerJson.getJSONObject("requiredParams"));
                    break;
                /*case ANALYSIS_ERROR:
                    result = analysisError(requestor, sessionId, answerJson.getJSONObject("requiredParams"));
                    break;*/
                case DEVICE:
                    result = getAvailableDevice(requestor, sessionId, answerJson.getJSONObject("requiredParams"));
                    break;
                default:
                    break;
            }
            if (result.isSuccess()) {
                sessionPool.get(sessionId).add(new ChatMessage(ChatMessage.Role.SYSTEM, result.getMessage()));
            }
            result.setSessionId(sessionId);
        }
        return result;
    }

    private ChatQAResult triggerTestTask(SysUser requestor, String sessionId, JSONObject params) {
        ChatQAResult result = new ChatQAResult();
        // upload app file to blob
        //Init test file set info
        TestFileSet testFileSet = new TestFileSet();
        testFileSet.setBuildType("");
        testFileSet.setCommitId("-1");
        testFileSet.setCommitMessage("");
        testFileSet.setTeamId(requestor.getDefaultTeamId());
        testFileSet.setTeamName(requestor.getDefaultTeamName());

        //Save app file to server
        String relativeParent = FileUtil.getPathForToday();
        File tempAppFile = filePool.get(sessionId);
        StorageFileInfo appFileInfo = new StorageFileInfo(tempAppFile, relativeParent, StorageFileInfo.FileType.APP_FILE);
        //Upload app file
        appFileInfo = attachmentService.addAttachment(testFileSet.getId(), EntityType.APP_FILE_SET, appFileInfo, tempAppFile, logger);
        JSONObject appFileParser = appFileInfo.getFileParser();
        testFileSet.setAppName(appFileParser.getString(StorageFileInfo.ParserKey.APP_NAME));
        testFileSet.setPackageName(appFileParser.getString(StorageFileInfo.ParserKey.PKG_NAME));
        testFileSet.setVersion(appFileParser.getString(StorageFileInfo.ParserKey.VERSION));
        testFileSet.getAttachments().add(appFileInfo);
        testFileSetService.addTestFileSet(testFileSet);

        // start test task
        TestTaskSpec testTaskSpec = new TestTaskSpec();
        testTaskSpec.testFileSet = testFileSet;
        testTaskSpec.teamId = testFileSet.getTeamId();
        testTaskSpec.teamName = testFileSet.getTeamName();
        testTaskSpec.testTaskId = UUID.randomUUID().toString();
        testTaskSpec.fileSetId = testFileSet.getId();
        testTaskSpec.deviceIdentifier = params.getString("deviceIdentifier");
        testTaskSpec.maxStepCount = params.getInteger("maxStepCount");
        testTaskSpec.deviceTestCount = params.getInteger("deviceTestCount");
        testTaskSpec.runningType = TestTask.TestRunningType.SMART_TEST;
        testTaskSpec.pkgName = testFileSet.getPackageName();
        testTaskSpec.testSuiteClass = testFileSet.getPackageName();
        testTaskSpec.enablePerformanceSuggestion = true;

        List<InspectionStrategy> inspectionStrategies = new ArrayList<>();
        InspectionStrategy batteryStrategy = new InspectionStrategy();
        batteryStrategy.strategyType = InspectionStrategy.StrategyType.TEST_SCHEDULE;
        batteryStrategy.interval = 5000;
        batteryStrategy.intervalUnit = java.util.concurrent.TimeUnit.MILLISECONDS;
        batteryStrategy.inspection = new PerformanceInspection(
                "Inspect battery",
                PerformanceInspector.PerformanceInspectorType.INSPECTOR_ANDROID_BATTERY_INFO,
                testFileSet.getPackageName(),
                testTaskSpec.deviceIdentifier,
                false);
        inspectionStrategies.add(batteryStrategy);

        InspectionStrategy memoryStrategy = new InspectionStrategy();
        memoryStrategy.strategyType = InspectionStrategy.StrategyType.TEST_SCHEDULE;
        memoryStrategy.interval = 5000;
        memoryStrategy.intervalUnit = java.util.concurrent.TimeUnit.MILLISECONDS;
        memoryStrategy.inspection = new PerformanceInspection(
                "Inspect memory",
                PerformanceInspector.PerformanceInspectorType.INSPECTOR_ANDROID_MEMORY_INFO,
                testFileSet.getPackageName(),
                testTaskSpec.deviceIdentifier,
                false);
        inspectionStrategies.add(memoryStrategy);

        testTaskSpec.inspectionStrategies = inspectionStrategies;
        //if the queue is not empty, the task will be added to the queue directly
        if (testTaskService.isQueueEmpty() || testTaskService.isDeviceFree(testTaskSpec.deviceIdentifier)) {
            JSONObject runResult = deviceAgentManagementService.runTestTaskBySpec(testTaskSpec);
            if (runResult.get(Const.Param.TEST_DEVICE_SN) == null) {
                //if there is no alive device, the task will be added to the queue directly
                testTaskService.addTask(testTaskSpec);
            } else {
                TestTask testTask = TestTask.convertToTestTask(testTaskSpec);
                testTask.setTestDevicesCount(runResult.getString(Const.Param.TEST_DEVICE_SN).split(",").length);
                testDataService.saveTestTaskData(testTask);
            }
        } else {
            testTaskService.addTask(testTaskSpec);
        }
        result.setSuccess(true);
        result.setMessage("The task has queued or started and the taskID is " + testTaskSpec.testTaskId);
        return result;
    }

    private ChatQAResult queryResult(SysUser requestor, String sessionId, JSONObject params) {
        String taskId = params.getString("taskID");
        TestTask testTask = testDataService.getTestTaskDetail(taskId);
        ChatQAResult result = verifyTestTask(testTask, taskId);
        if (!result.isSuccess()) {
            return result;
        }
        result = formatResult(requestor, testTask);
        return result;
    }

    private ChatQAResult verifyTestTask(TestTask testTask, String taskId) {
        ChatQAResult result = new ChatQAResult();
        if (testTask == null) {
            TestTaskQueuedInfo queuedInfo = testTaskService.getTestQueuedInfo(taskId);
            TestTaskSpec queuedTaskSpec = queuedInfo.getTestTaskSpec();
            if (queuedTaskSpec == null) {
                result.setSuccess(false);
                result.setMessage("The task does not exist!");
            }
            result.setSuccess(false);
            result.setMessage("Current position in queue: " + queuedInfo.getQueuedInfo()[0]);
        } else if (!TestTask.TestStatus.FINISHED.equals(testTask.getStatus())) {
            result.setSuccess(false);
            result.setMessage("The task has not finished!");
        } else {
            result.setSuccess(true);
        }
        return result;
    }

    private ChatQAResult formatResult(SysUser requestor, TestTask testTask) {
        JSONObject testTaskCopy = new JSONObject();
        testTaskCopy.put("id", testTask.getId());
        testTaskCopy.put("pkgName", testTask.getPkgName());
        testTaskCopy.put("deviceTestResults", new JSONArray());
        testTaskCopy.put("reportPage", String.format("http://%s/portal/index.html#/info/task/%s", requestor.getHost(), testTask.getId()));
        testTask.getDeviceTestResults().forEach(deviceTestResult -> {
            JSONObject deviceTestResultCopy = new JSONObject();
            deviceTestResultCopy.put("deviceName", deviceTestResult.getDeviceName());
            deviceTestResultCopy.put("attachments", new JSONArray());
            deviceTestResultCopy.put("testUnitList", new JSONArray());
            deviceTestResult.getAttachments().forEach(attachment -> {
                JSONObject attachmentCopy = new JSONObject();
                attachmentCopy.put("fileName", attachment.getFileName());
                attachmentCopy.put("cdnUrl", attachment.getCDNUrl());
                deviceTestResultCopy.getJSONArray("attachments").add(attachmentCopy);
            });
            deviceTestResult.getTestUnitList().forEach(unitTest -> {
                JSONObject unitTestCopy = new JSONObject();
                unitTestCopy.put("displaySpentTime", unitTest.getDisplaySpentTime());
                unitTestCopy.put("success", unitTest.isSuccess());
                deviceTestResultCopy.getJSONArray("testUnitList").add(unitTestCopy);
            });
            testTaskCopy.getJSONArray("deviceTestResults").add(deviceTestResultCopy);
        });

        ChatQAResult result = new ChatQAResult();
        ChatRequest request = new ChatRequest();
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage(ChatMessage.Role.SYSTEM, ANALYSIS_RESULT_PROMPT + testTaskCopy));
        request.setMessages(chatMessages);
        request.setTemperature(0);
        request.setTopP(0.95);
        String answer = oaiClient.chatCompletion(request);
        String answerText = JSONObject.parseObject(answer).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        logger.info("The answer is " + answerText);
        result.setSuccess(true);
        result.setMessage(answerText);
        return result;
    }

    private ChatQAResult analysisPref(SysUser requestor, String sessionId, JSONObject params) {
        String taskId = params.getString("taskID");
        TestTask testTask = testDataService.getTestTaskDetail(taskId);
        ChatQAResult result = verifyTestTask(testTask, taskId);
        if (!result.isSuccess()) {
            return result;
        }
        String prefResult = "";
        List<TestRun> testRunList = testTask.getDeviceTestResults();
        for (int i = 1; i <= testRunList.size(); i++) {
            TestRun testRun = testRunList.get(i - 1);
            suggestionService.performanceAnalyze(testRun);
            prefResult += i + ". The performance of " + testRun.getDeviceName() + " is " + testRun.getSuggestion() + "\n";

        }
        result.setSuccess(true);
        result.setMessage(prefResult);

        return result;
    }

    private ChatQAResult analysisError(SysUser requestor, String sessionId, JSONObject params) {
        String taskId = params.getString("taskID");
        TestTask testTask = testDataService.getTestTaskDetail(taskId);
        ChatQAResult result = verifyTestTask(testTask, taskId);
        if (!result.isSuccess()) {
            return result;
        }
        String prefResult = "";
        List<TestRun> testRunList = testTask.getDeviceTestResults();
        for (int i = 1; i <= testRunList.size(); i++) {
            TestRun testRun = testRunList.get(i - 1);
            ExceptionSuggestion exceptionAnalyze = suggestionService.exceptionAnalyze(testRun);
            prefResult += i + ". The exception suggestion of " + testRun.getDeviceName() + " is " + exceptionAnalyze.getContent() + "\n";

        }
        result.setSuccess(true);
        result.setMessage(prefResult);

        return result;
    }

    private ChatQAResult getAvailableDevice(SysUser requestor, String sessionId, JSONObject params) {
        ChatQAResult result = new ChatQAResult();
        List<DeviceInfo> deviceList = new ArrayList<>();
        deviceAgentManagementService.requestAllAgentDeviceListUpdate();
        // filter the devices which can run smart test
        List<AgentDeviceGroup> deviceGroupList =
                deviceAgentManagementService.getAgentDeviceGroups().stream().filter(
                        deviceGroup ->
                                deviceGroup.getFunctionAvailabilities().stream().anyMatch(functionAvailability ->
                                                functionAvailability.getFunctionName().equals("com.microsoft.hydralab.agent.runner.smart.SmartRunner")
                                                        && functionAvailability.getFunctionType().equals(AgentFunctionAvailability.AgentFunctionType.TEST_RUNNER)
                                        //&& functionAvailability.isAvailable()
                                )
                ).collect(Collectors.toList());
        for (AgentDeviceGroup agentDeviceGroup : deviceGroupList) {
            List<DeviceInfo> devices = agentDeviceGroup.getDevices().stream()
                    .filter(device -> DeviceType.ANDROID.name().equals(device.getType()))
                    .collect(Collectors.toList());
            // if the user neither admin nor the member of the team, the private devices will be filtered
            if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, agentDeviceGroup.getTeamId())) {
                devices = devices.stream().filter(device -> !device.getIsPrivate()).collect(Collectors.toList());
            }
            deviceList.addAll(devices);
        }
        result.setSuccess(true);
        if (deviceList.isEmpty()) {
            result.setMessage("There is no available device!");
        } else if (deviceList.size() == 1) {
            result.setMessage("There is 1 available device.\n" + getDeviceListString(deviceList));
        } else {
            result.setMessage("There are " + deviceList.size() + " available devices.\n" + getDeviceListString(deviceList));
        }
        return result;
    }

    // transfer deviceList to String
    private String getDeviceListString(List<DeviceInfo> deviceList) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (DeviceInfo device : deviceList) {
            sb.append(String.format("%d. identifier is %s, device type is %s \n", i++, device.getSerialNum(), device.getType()));
        }
        return sb.toString();
    }

    // save app file
    public ChatQAResult saveAppFile(SysUser requestor, String sessionId, File tempAppFile) {
        filePool.put(sessionId, tempAppFile);
        return askQuestion(requestor, ChatMessage.Role.SYSTEM, sessionId, " The path of the app file is " + tempAppFile.getAbsolutePath());
    }

    @Getter
    public enum UserIntentionType {
        RUN("start a test task", new Parameter[]{
                new Parameter("maxStepCount", "500", "The number of steps during testing", true),
                new Parameter("deviceTestCount", "1", "The total rounds of testing", true),
                new Parameter("appFile", "", "The install package of the app to be tested, format as D:/Git/Hydra-Lab/storage.", true),
                new Parameter("deviceIdentifier", "", "The identifier/name/id of device which is used to run task.", true)
        }),
        QUERY("query the result of test task", new Parameter[]{
                new Parameter("taskID", "",
                        "The id of test task and can be provided by user or system.", true),
        }),
        ANALYSIS_PERFORMANCE("analysis the performance of test task", new Parameter[]{
                new Parameter("taskID", "",
                        "The id of test task and can be provided by user or system.", true),
        }),
        /*ANALYSIS_ERROR("analysis the error of test task", new Parameter[]{
                new Parameter("taskID",
                        "The test task ID that can be provided by user or system. As long as one party provides it, the verification is passed.", true),
        }),*/
        DEVICE("query the available devices", new Parameter[]{});

        private final String description;
        private final Parameter[] parameters;

        UserIntentionType(String description, Parameter[] parameters) {
            this.description = description;
            this.parameters = parameters;
        }

        public static UserIntentionType convertToType(String text) {
            for (UserIntentionType b : UserIntentionType.values()) {
                if (b.name().equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            String parameterString = "";
            for (int i = 1; i <= parameters.length; i++) {
                parameterString += i + "." + parameters[i - 1].toString() + "\n";
            }
            return "The task " + name() + " is used to " + description +
                    ". And the size of parameter is " + parameters.length + ".\n " + parameterString;
        }
    }

    private static String getAllUserIntentionType() {
        StringBuilder sb = new StringBuilder();
        for (UserIntentionType type : UserIntentionType.values()) {
            sb.append(type.toString() + "\n");
        }
        return sb.toString();
    }

    @Data
    public static class Parameter {
        private String name;
        private String defaultValue;
        private String description;
        private Boolean isRequired;

        public Parameter(String name, String defaultValue, String description, Boolean isRequired) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.description = description;
            this.isRequired = isRequired;
        }

        public String toString() {
            return "The parameter " + name + " is used to simplify " + description + ". It is " + (isRequired ? "required" : "not required")
                    + (!StringUtils.isEmpty(defaultValue)?". The default is "+ defaultValue:"")
                    +".\n";
        }
    }

    @Data
    public static class ChatQAResult {
        private boolean success;
        private String message;
        private String sessionId;
    }
}
