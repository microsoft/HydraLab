// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.android.ddmlib.IDevice;
import com.microsoft.hydralab.center.openai.SuggestionService;
import com.microsoft.hydralab.center.repository.AgentUserRepository;
import com.microsoft.hydralab.center.util.MetricUtil;
import com.microsoft.hydralab.common.entity.agent.AgentFunctionAvailability;
import com.microsoft.hydralab.common.entity.agent.EnvCapabilityRequirement;
import com.microsoft.hydralab.common.entity.agent.MobileDevice;
import com.microsoft.hydralab.common.entity.center.AgentDeviceGroup;
import com.microsoft.hydralab.common.entity.center.DeviceGroup;
import com.microsoft.hydralab.common.entity.center.DeviceGroupRelation;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.AccessInfo;
import com.microsoft.hydralab.common.entity.common.AgentMetadata;
import com.microsoft.hydralab.common.entity.common.AgentUpdateTask;
import com.microsoft.hydralab.common.entity.common.AgentUser;
import com.microsoft.hydralab.common.entity.common.AnalysisTask;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.Message;
import com.microsoft.hydralab.common.entity.common.StatisticData;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.entity.common.TestTaskSpec;
import com.microsoft.hydralab.common.entity.common.BlockedDeviceInfo;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.management.device.DeviceType;
import com.microsoft.hydralab.common.repository.StatisticDataRepository;
import com.microsoft.hydralab.common.repository.StorageFileInfoRepository;
import com.microsoft.hydralab.common.util.AttachmentService;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.GlobalConstant;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.SerializeUtil;
import com.microsoft.hydralab.t2c.runner.DriverInfo;
import com.microsoft.hydralab.t2c.runner.T2CJsonParser;
import com.microsoft.hydralab.t2c.runner.TestInfo;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.microsoft.hydralab.center.util.CenterConstant.CENTER_FILE_BASE_DIR;

@Slf4j
@Component
public class DeviceAgentManagementService {
    /**
     * Connected session count
     */
    private final AtomicInteger onlineCount = new AtomicInteger(0);
    private static volatile AtomicBoolean isUnblockingDevices = new AtomicBoolean(false);
    //save agent session <sessionId,session&agentUser>
    private final ConcurrentHashMap<String, AgentSessionInfo> agentSessionMap = new ConcurrentHashMap<>();
    //save agent info <agentId,agentInfo>
    private final ConcurrentHashMap<String, AgentDeviceGroup> agentDeviceGroups = new ConcurrentHashMap<>();
    //save group&device relation <groupName,deviceSerials>
    private final ConcurrentHashMap<String, Set<String>> deviceGroupListMap = new ConcurrentHashMap<>();
    //save device info <deviceSerial,deviceInfo>
    private final ConcurrentHashMap<String, DeviceInfo> deviceListMap = new ConcurrentHashMap<>();
    //save access info <deviceSerial/groupName,accessInfo>
    private final ConcurrentHashMap<String, AccessInfo> accessInfoMap = new ConcurrentHashMap<>();
    //save agent update info <agentId,updateTask>
    private final ConcurrentHashMap<String, AgentUpdateTask> agentUpdateMap = new ConcurrentHashMap<>();
    // save blocked devices <deviceSerial, blockedDeviceInfo>
    private final ConcurrentHashMap<String, BlockedDeviceInfo> blockedDevicesMap = new ConcurrentHashMap<>();

    @Resource
    MetricUtil metricUtil;
    @Resource
    StatisticDataRepository statisticDataRepository;
    @Resource
    AgentUserRepository agentUserRepository;
    @Resource
    TestDataService testDataService;
    @Resource
    DeviceGroupService deviceGroupService;
    @Resource
    TestTaskService testTaskService;
    @Resource
    StorageFileInfoRepository storageFileInfoRepository;
    @Resource
    AttachmentService attachmentService;
    @Resource
    AgentManageService agentManageService;
    @Resource
    StorageServiceClientProxy storageServiceClientProxy;
    @Resource
    StorageTokenManageService storageTokenManageService;
    @Resource
    SuggestionService suggestionService;

    @Value("${app.storage.type}")
    private String storageType;

    @Value("${app.access-token-limit}")
    int accessLimit;
    @Value("${app.batteryStrategy}")
    private String batteryStrategy;
    private long lastTimeRequest;
    @Value("${management.metrics.export.prometheus.pushgateway.username}")
    private String pushgatewayUsername;
    @Value("${management.metrics.export.prometheus.pushgateway.password}")
    private String pushgatewayPassword;
    @Value("${app.error-reporter.app-center.agent.enabled: false}")
    private boolean appCenterEnabled;
    @Value("${app.error-reporter.app-center.agent.secret: ''}")
    private String appCenterSecret;

    public void onOpen(Session session) {
        onlineCount.incrementAndGet();
        log.info("New session checked in, id: {}, online count: {}", session.getId(), onlineCount.get());
        requestAuth(session);
    }

    public void requestAllAgentDeviceListUpdate() {
        synchronized (this) {
            if (System.currentTimeMillis() - lastTimeRequest < TimeUnit.SECONDS.toMillis(5)) {
                return;
            }
            lastTimeRequest = System.currentTimeMillis();
        }

        for (AgentSessionInfo value : agentSessionMap.values()) {
            requestList(value.session);
        }
    }

    @Scheduled(cron = "*/10 * * * * *")
    public void heartbeatAll() {
        for (AgentSessionInfo value : agentSessionMap.values()) {
            sendAgentMetadata(value.session, value.agentUser, Const.Path.HEARTBEAT);
        }
    }

    private void sendAgentMetadata(Session session, AgentUser agentUser, String signalName) {
        agentUser.setBatteryStrategy(AgentUser.BatteryStrategy.valueOf(batteryStrategy));
        AgentMetadata data = new AgentMetadata();
        data.setStorageType(storageType);
        data.setAccessToken(storageTokenManageService.generateWriteToken(agentUser.getId()));
        data.setAgentUser(agentUser);
        data.setPushgatewayUsername(pushgatewayUsername);
        data.setPushgatewayPassword(pushgatewayPassword);
        if (appCenterEnabled && appCenterSecret != null && !appCenterSecret.isEmpty()) {
            data.setAppCenterSecret(appCenterSecret);
        }

        Message message = new Message();
        message.setPath(signalName);
        message.setBody(data);
        sendMessageToSession(session, message);
    }

    private void requestAuth(Session session) {
        sendMessageToSession(session, Message.auth());
    }

    public void onMessage(Message message, @NotNull Session session) throws IOException {
        AgentSessionInfo savedSession = agentSessionMap.get(session.getId());
        if (savedSession != null) {
            handleQualifiedAgentMessage(message, savedSession);
            return;
        }

        AgentUser agentUser = searchQualifiedAgent(message);
        if (agentUser == null) {
            log.warn("Session {} is not registered agent, associated agent {}", session.getId(), message.getBody());
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Agent Info is not correct!"));
            return;
        }

        if (agentDeviceGroups.get(agentUser.getId()) != null && checkIsSessionAliveByAgentId(agentUser.getId())) {
            log.warn("Session {} is already connected under another agent, associated agent {}", session.getId(), message.getBody());
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "AgentID has been used!"));
            return;
        }

        agentSessionMap.put(session.getId(), new AgentSessionInfo(session, agentUser));
        metricUtil.registerAgentAliveStatusMetric(agentUser);

        log.info("Session {} is saved to map as registered agent, associated agent {}", session.getId(), message.getBody());

        checkAgentUpdateStatus(message, session, agentUser);

        sendAgentMetadata(session, agentUser, Const.Path.AGENT_INIT);
    }

    /**
     * check is not agent update success
     *
     * @param message
     * @param session
     * @param agentUser
     */
    private void checkAgentUpdateStatus(Message message, Session session, AgentUser agentUser) {
        AgentUpdateTask agentUpdateTask = agentUpdateMap.get(agentUser.getId());
        if (agentUpdateTask == null) {
            return;
        }

        log.info("Session {} is saved to map as registered agent, associated agent {}", session.getId(), message.getBody());

        AgentUpdateTask.UpdateMsg updateMag = null;
        String agentMessage = "Agent Reconnected After Updating.Version is " + agentUser.getVersionName();

        if (agentUser.getVersionName() == null || !agentUser.getVersionName().equals(agentUpdateTask.getTargetVersionName())) {
            agentUpdateTask.setUpdateStatus(AgentUpdateTask.TaskConst.STATUS_FAIL);
            updateMag = new AgentUpdateTask.UpdateMsg(false, agentMessage, agentUser.toString());
        } else {
            agentUpdateTask.setUpdateStatus(AgentUpdateTask.TaskConst.STATUS_SUCCESS);
            updateMag = new AgentUpdateTask.UpdateMsg(true, agentMessage, "");
        }

        agentUpdateTask.getUpdateMsgs().add(updateMag);
    }

    private void handleQualifiedAgentMessage(Message message, AgentSessionInfo savedSession) {
        switch (message.getPath()) {
            case Const.Path.DEVICE_LIST:
                if (message.getBody() instanceof JSONArray) {
                    List<DeviceInfo> latestDeviceInfos = ((JSONArray) message.getBody()).toJavaList(DeviceInfo.class);
                    updateAgentDeviceGroup(savedSession, latestDeviceInfos);
                }
                break;
            case Const.Path.DEVICE_UPDATE:
                if (message.getBody() instanceof DeviceInfo) {
                    DeviceInfo device = (DeviceInfo) message.getBody();
                    List<DeviceInfo> latestDeviceInfos = new ArrayList<>();
                    latestDeviceInfos.add(device);
                    updateAgentDeviceGroup(savedSession, latestDeviceInfos);
                }
                break;
            case Const.Path.AGENT_UPDATE:
                if (message.getBody() instanceof AgentUpdateTask.UpdateMsg) {
                    AgentUpdateTask.UpdateMsg updateMsg = (AgentUpdateTask.UpdateMsg) message.getBody();
                    log.info("Agent {} is updating, message {}", savedSession.agentUser.getId(), updateMsg.message);
                    AgentUpdateTask tempTask = agentUpdateMap.get(savedSession.agentUser.getId());
                    if (tempTask == null || !AgentUpdateTask.TaskConst.STATUS_UPDATING.equals(tempTask.getUpdateStatus())) {
                        break;
                    }
                    tempTask.getUpdateMsgs().add(updateMsg);
                    if (!updateMsg.isProceed) {
                        tempTask.setUpdateStatus(AgentUpdateTask.TaskConst.STATUS_FAIL);
                        agentDeviceGroups.get(savedSession.agentUser.getId()).setAgentStatus(AgentDeviceGroup.Status.HEALTHY);
                    }
                }
                break;
            case Const.Path.DEVICE_STATUS:
                if (message.getBody() instanceof JSONObject) {
                    JSONObject data = (JSONObject) message.getBody();
                    updateDeviceStatus(data.getString(Const.AgentConfig.SERIAL_PARAM), data.getString(Const.AgentConfig.STATUS_PARAM),
                            data.getString(Const.AgentConfig.TASK_ID_PARAM));
                }
                break;
            case Const.Path.ACCESS_INFO:
                if (message.getBody() instanceof AccessInfo) {
                    AccessInfo accessInfo = (AccessInfo) message.getBody();
                    updateAccessInfo(accessInfo);
                }
                break;
            case Const.Path.TEST_TASK_UPDATE:
                if (message.getBody() instanceof Task) {
                    Task task = (Task) message.getBody();
                    boolean isFinished = task.getStatus().equals(Task.TaskStatus.FINISHED);
                    //after the task finishing, update the status of device used
                    if (isFinished) {
                        List<TestRun> deviceTestResults = task.getTaskRunList();
                        for (TestRun deviceTestResult : deviceTestResults) {
                            if (task instanceof TestTask && ((TestTask) task).isEnablePerformanceSuggestion()) {
                                suggestionService.performanceAnalyze(deviceTestResult);
                            }
                            String[] identifiers = deviceTestResult.getDeviceSerialNumber().split(",");
                            for (String identifier : identifiers) {
                                if (Task.RunnerType.APK_SCANNER.name().equals(task.getRunnerType())) {
                                    agentDeviceGroups.get(identifier).finishAnalysisTask(task.getRunnerType());
                                } else {
                                    updateDeviceStatus(identifier, DeviceInfo.ONLINE, null);
                                }
                            }
                        }
                        //run the task saved in queue
                        testTaskService.runTask();
                    }
                    testDataService.saveTaskDataFromAgent(task, isFinished, savedSession.agentUser.getId());
                }
                break;
            case Const.Path.TEST_TASK_RUN:
                log.info("Message from agent {}", message);
                break;
            case Const.Path.TEST_TASK_RETRY:
                if (message.getBody() instanceof Task) {
                    Task task = (Task) message.getBody();
                    if (task.getRetryTime() == Const.AgentConfig.RETRY_TIME) {
                        testDataService.saveTaskData(task);
                    } else {
                        TestTaskSpec taskSpec = task.convertToTaskSpec();
                        taskSpec.retryTime++;
                        testTaskService.addTask(taskSpec);
                        log.info("Retry task {} for {} time", task.getId(), taskSpec.retryTime);
                        cancelTestTaskById(task.getId(), "Error happened:" + task.getErrorMsg() + ". Will cancel the task and retry.");
                        //run the task saved in queue
                        testTaskService.runTask();
                    }
                }
                break;
            case Const.Path.HEARTBEAT:
                sendAgentMetadata(savedSession.session, savedSession.agentUser, Const.Path.HEARTBEAT);
                break;
            default:
                break;
        }
    }

    public void updateAccessInfo(AccessInfo accessInfo) {
        if (accessInfo != null) {
            accessInfoMap.put(accessInfo.getName(), accessInfo);
        }
    }

    public void cancelTestTaskById(String taskId, String reason) {
        Set<String> agentIds = testDataService.cancelTaskById(taskId, reason);
        Task task = testDataService.getTaskDetail(taskId);
        JSONObject data = new JSONObject();
        Message message = new Message();
        data.put(Const.AgentConfig.TASK_ID_PARAM, taskId);
        message.setPath(Const.Path.TEST_TASK_CANCEL);
        message.setBody(data);
        for (String agentId : agentIds) {
            if (Task.RunnerType.APK_SCANNER.name().equals(task.getRunnerType())) {
                agentDeviceGroups.get(agentId).finishAnalysisTask(task.getRunnerType());
            }

            AgentSessionInfo agentSession = getAgentSessionInfoByAgentId(agentId);
            if (agentSession == null || agentSession.session == null) {
                continue;
            }
            sendMessageToSession(agentSession.session, message);
            for (DeviceInfo deviceInfo : agentDeviceGroups.get(agentId).getDevices()) {
                if (taskId.equals(deviceInfo.getRunningTaskId())) {
                    deviceInfo.setStatus(DeviceInfo.ONLINE);
                    deviceInfo.setRunningTaskId(null);

                    DeviceInfo device = deviceListMap.get(deviceInfo.getSerialNum());
                    device.setStatus(DeviceInfo.ONLINE);
                    deviceInfo.setRunningTaskId(null);
                }
            }
        }
    }

    public void checkAccessInfo(String name, String key) {
        if (key == null) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Access key is required!");
        }
        AccessInfo accessInfo = accessInfoMap.get(name);
        if (accessInfo == null) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Please generate access key first!");
        }
        if (!key.equals(accessInfo.getKey())) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Error access key!");
        }
        int hour = (int) ((new Date().getTime() - accessInfo.getIngestTime().getTime()) / 1000 / 60 / 60);
        if (hour > accessLimit) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Access key has expired!");
        }
    }

    private void updateAgentDeviceGroup(AgentSessionInfo savedSession, List<DeviceInfo> latestDeviceInfos) {
        /**
         * Device status update logic:
         *  1. TESTING can only be set in CENTER and sync to AGENT
         *  2. ONLINE/OFFLINE/UNSTABLE can only be set in AGENT and sync to CENTER
         */
        updateDeviceGroup(latestDeviceInfos, savedSession.agentUser.getId());
        AgentDeviceGroup agentDeviceGroup = agentDeviceGroups.get(savedSession.agentUser.getId());
        if (agentDeviceGroup == null) {
            agentDeviceGroup = new AgentDeviceGroup();
            agentDeviceGroup.initWithAgentUser(savedSession.agentUser);
            agentDeviceGroups.put(savedSession.agentUser.getId(), agentDeviceGroup);
            log.info("Adding info of new agent: {}, device SN: {}", agentDeviceGroup.getAgentName(),
                    latestDeviceInfos.stream().map(MobileDevice::getSerialNum).collect(Collectors.joining(",")));
        }
        agentDeviceGroup.setDevices(new ArrayList<>(latestDeviceInfos));
    }

    public void updateDeviceGroup(List<DeviceInfo> agentDeviceInfos, String agentId) {
        for (DeviceInfo agentDeviceInfo : agentDeviceInfos) {
            //init agent info
            agentDeviceInfo.setAgentId(agentId);
            DeviceInfo centerDevice = deviceListMap.get(agentDeviceInfo.getSerialNum());
            // if the status saved in Center is testing, the value will not be covered
            if (centerDevice != null && centerDevice.isTesting()) {
                log.warn("Center status: {}, Agent status: {}, status should be synced to CENTER's value when TESTING.", centerDevice.getStatus(), agentDeviceInfo.getStatus());
                agentDeviceInfo.setStatus(DeviceInfo.TESTING);
            } else if (agentDeviceInfo.isTesting()) {
                log.warn("Test on the device is canceled, status of device in AGENT should be reset to ONLINE, otherwise TESTING would never be covered by agent");
                agentDeviceInfo.setStatus(DeviceInfo.ONLINE);
            }

            deviceListMap.put(agentDeviceInfo.getSerialNum(), agentDeviceInfo);

            //init group info
            List<DeviceGroupRelation> groups = deviceGroupService.getGroupByDevice(agentDeviceInfo.getSerialNum());
            for (DeviceGroupRelation group : groups) {
                addDeviceToGroup(group.getGroupName(), group.getDeviceSerial());
            }
        }
    }

    //update Device Status : start task,complete task,device offline,device online
    public void updateDeviceStatus(String serialNum, String status, String testTaskId) {
        DeviceInfo device = deviceListMap.get(serialNum);
        if (device == null) {
            return;
        }
        device.setStatus(status);
        device.setRunningTaskId(testTaskId);
        for (DeviceInfo deviceInfo : agentDeviceGroups.get(device.getAgentId()).getDevices()) {
            if (serialNum.equals(deviceInfo.getSerialNum())) {
                deviceInfo.setStatus(status);
                deviceInfo.setRunningTaskId(testTaskId);
                break;
            }
        }

    }

    //query devices by groupName
    public List<DeviceInfo> queryDevicesByGroup(String groupName) {
        List<DeviceInfo> devices = new ArrayList<>();
        List<DeviceGroupRelation> relations = deviceGroupService.getDeviceByGroup(groupName);
        for (DeviceGroupRelation relation : relations) {
            String serialNum = relation.getDeviceSerial();
            DeviceInfo device = deviceListMap.get(serialNum);
            if (device == null) {
                device = new DeviceInfo();
                device.setSerialNum(serialNum);
                device.setStatus(IDevice.DeviceState.DISCONNECTED.toString());
            }
            devices.add(device);
        }
        return devices;
    }

    //add group&device relation
    public void addDeviceToGroup(String group, String serialNum) {
        Set<String> serials = deviceGroupListMap.computeIfAbsent(group, k -> new HashSet<>());
        serials.add(serialNum);
        DeviceInfo device = deviceListMap.get(serialNum);
        device.getDeviceGroup().add(group);
    }

    public Set<String> queryDeviceByGroup(String group) {
        Set<String> serials = deviceGroupListMap.get(group);
        if (serials == null) {
            return new HashSet<>();
        }
        return new HashSet<>(serials);
    }

    public List<DeviceInfo> queryDeviceInfoByGroup(String groupName) {
        List<DeviceInfo> devices = new ArrayList<>();
        Set<String> serials = deviceGroupListMap.get(groupName);
        serials.forEach(serial -> devices.add(deviceListMap.get(serial)));

        return devices;
    }

    public Set<String> queryGroupByDevice(String deviceSerial) {
        DeviceInfo deviceInfo = deviceListMap.get(deviceSerial);
        if (deviceInfo == null || deviceInfo.getDeviceGroup() == null) {
            return new HashSet<>();
        }
        return new HashSet<>(deviceInfo.getDeviceGroup());
    }

    //delete group&device relation
    public void deleteDeviceFromGroup(String group, String serialNum) {
        Set<String> serials = deviceGroupListMap.get(group);
        if (serials != null) {
            serials.remove(serialNum);
        }
        DeviceInfo device = deviceListMap.get(serialNum);
        if (device != null) {
            device.getDeviceGroup().remove(group);
        }

    }

    //delete device and group&device relation
    public void removeDevices(List<DeviceInfo> devices) {
        for (DeviceInfo device : devices) {
            Set<String> groups = device.getDeviceGroup();
            for (String group : groups) {
                Set<String> serials = deviceGroupListMap.get(group);
                if (serials != null) {
                    serials.remove(device.getSerialNum());
                }
            }
            deviceListMap.remove(device.getSerialNum());
        }
    }

    //delete group and group&device relation
    public void removeGroup(String groupName) {
        Set<String> devices = deviceGroupListMap.remove(groupName);
        if (devices == null) {
            return;
        }
        for (String device : devices) {
            deviceListMap.get(device).getDeviceGroup().remove(groupName);
        }
    }

    //check the serialNum is correct
    public boolean checkDeviceInfo(String serialNum) {
        return deviceListMap.get(serialNum) != null;
    }

    public boolean checkDeviceAuthorization(SysUser requestor, String serialNum) throws IllegalArgumentException {
        DeviceInfo deviceInfo = deviceListMap.get(serialNum);
        if (deviceInfo == null) {
            throw new IllegalArgumentException("deviceIdentifier is incorrect");
        }

        return agentManageService.checkAgentAuthorization(requestor, deviceInfo.getAgentId());
    }

    public void updateDeviceScope(String deviceSerial, Boolean isPrivate) {
        DeviceInfo deviceInfo = deviceListMap.get(deviceSerial);
        AgentSessionInfo agentSession = getAgentSessionInfoByAgentId(deviceInfo.getAgentId());
        Assert.notNull(agentSession, "agent session error");
        Assert.notNull(agentSession.session, "agent session error");

        JSONObject data = new JSONObject();
        data.put(Const.AgentConfig.SERIAL_PARAM, deviceSerial);
        data.put(Const.AgentConfig.SCOPE_PARAM, isPrivate);

        Message message = new Message();
        message.setPath(Const.Path.DEVICE_UPDATE);
        message.setBody(data);

        sendMessageToSession(agentSession.session, message);
    }

    private void requestList(Session session) {
        Message message = new Message();
        message.setPath(Const.Path.DEVICE_LIST);
        sendMessageToSession(session, message);
    }

    private AgentUser searchQualifiedAgent(Message message) {
        Object body = message.getBody();
        if (!(body instanceof AgentUser)) {
            return null;
        }
        AgentUser agentUser = (AgentUser) body;
        String id = agentUser.getId();

        Optional<AgentUser> findUser = agentUserRepository.findById(id);
        if (!findUser.isPresent()) {
            return null;
        }
        AgentUser user = findUser.get();
        if (!user.getSecret().equals(agentUser.getSecret()) || !user.getName().equals(agentUser.getName())) {
            return null;
        }
        user.setHostname(agentUser.getHostname());
        user.setIp(agentUser.getIp());
        user.setVersionName(agentUser.getVersionName());
        user.setVersionCode(agentUser.getVersionCode());
        user.setSecret(null);
        user.setFunctionAvailabilities(agentUser.getFunctionAvailabilities());
        return user;
    }

    public void onClose(Session session) {
        onlineCount.decrementAndGet();
        log.info("Session closed, id：{}，online count: {}", session.getId(), onlineCount.get());
        deleteSessionAndDevice(session);
    }

    public void onError(Session session, Throwable error) {
        log.error("onError from session " + session.getId(), error);

        error.printStackTrace();
        deleteSessionAndDevice(session);
    }

    public void deleteSessionAndDevice(Session session) {
        if (!agentSessionMap.containsKey(session.getId())) {
            return;
        }
        AgentSessionInfo removed = agentSessionMap.remove(session.getId());
        if (removed == null || removed.agentUser == null) {
            return;
        }
        log.info("Session of agent {} is closed.", removed.agentUser.getName());
        metricUtil.updateAgentAliveStatus(removed.agentUser.getId(), GlobalConstant.AgentLiveStatus.OFFLINE.getStatus());

        AgentDeviceGroup agentDeviceGroup = agentDeviceGroups.remove(removed.agentUser.getId());
        if (agentDeviceGroup == null || agentDeviceGroup.getDevices() == null) {
            return;
        }
        removeDevices(agentDeviceGroup.getDevices());
    }

    private void sendMessageToSession(Session toSession, Message message) {
        try {
            byte[] array = SerializeUtil.messageToByteArr(message);
            log.info("sendMessageToSession[{}], path: {}, message data len: {}", toSession.getId(), message.getPath(), array.length);
            toSession.getBasicRemote().sendBinary(ByteBuffer.wrap(array));
        } catch (IOException e) {
            throw new RuntimeException("Error in sendMessageToSession", e);
        }
    }

    public List<AgentDeviceGroup> getAgentDeviceGroups() {
        return new ArrayList<>(agentDeviceGroups.values());
    }

    public List<DeviceGroup> getAllGroup() {
        List<DeviceGroup> res = new ArrayList<>();
        Set<String> keys = deviceGroupListMap.keySet();
        for (String key : keys) {
            DeviceGroup group = deviceGroupService.getGroupByName(key);
            if (group == null) {
                continue;
            }
            group.setSerialNums(deviceGroupListMap.get(key).toString());
            res.add(group);
        }
        return new ArrayList<>(res);
    }

    public List<DeviceInfo> getAllDevice() {
        List<DeviceInfo> res = new ArrayList<>();
        Set<String> keys = deviceListMap.keySet();
        for (String key : keys) {
            DeviceInfo device = deviceListMap.get(key);
            if (device.isAlive()) {
                res.add(deviceListMap.get(key));
            }
        }
        return new ArrayList<>(res);
    }

    public DeviceInfo getDevice(String deviceId) {
        return deviceListMap.get(deviceId);
    }

    public List<AgentDeviceGroup> getAllAppiumAgents() {
        List<AgentDeviceGroup> res = new ArrayList<>();
        Set<String> keys = agentDeviceGroups.keySet();
        for (String key : keys) {
            AgentDeviceGroup agent = agentDeviceGroups.get(key);
            List<DeviceInfo> windowsDevices = agent.getDevices().stream().filter(deviceInfo -> DeviceType.WINDOWS.name().equals(deviceInfo.getType())).collect(Collectors.toList());
            if (windowsDevices.size() == 1 && windowsDevices.get(0).isAlive()) {
                res.add(agent);
            }
        }
        return new ArrayList<>(res);
    }

    // Todo: Get agent list for android and ios agent

    public JSONObject runTestTaskBySpec(TestTaskSpec testTaskSpec) {
        JSONObject result;
        unblockFrozenBlockedDevices();
        if (Task.RunnerType.APPIUM_CROSS.name().equals(testTaskSpec.runningType)) {
            result = runAppiumTestTask(testTaskSpec);
        } else if (Task.RunnerType.T2C_JSON.name().equals(testTaskSpec.runningType)) {
            result = runT2CTest(testTaskSpec);
        } else if (Task.RunnerType.APK_SCANNER.name().equals(testTaskSpec.runningType)) {
            result = runAnalysisTask(testTaskSpec);
        } else {
            if (testTaskSpec.deviceIdentifier.startsWith(Const.DeviceGroup.GROUP_NAME_PREFIX)) {
                result = runTestTaskByGroup(testTaskSpec);
            } else {
                result = runTestTaskByDevice(testTaskSpec);
            }
        }
        return result;
    }

    private JSONObject runAnalysisTask(TestTaskSpec testTaskSpec) {
        List<AnalysisTask.AnalysisConfig> analysisConfigs = testTaskSpec.analysisConfigs;
        Assert.notEmpty(analysisConfigs, "No analysis config found!");
        JSONObject result = new JSONObject();
        List<AnalysisTask.AnalysisConfig> configs = testTaskSpec.analysisConfigs;

        List<AgentDeviceGroup> availableAgents = new ArrayList<>();

        for (AgentDeviceGroup tempAgentDeviceGroup : agentDeviceGroups.values()) {
            AgentFunctionAvailability function = tempAgentDeviceGroup.getFunctionAvailabilities().stream()
                    .filter(functionAvailability -> functionAvailability.getFunctionName().equals(testTaskSpec.runningType)).findFirst().orElse(null);
            if (function != null && function.isEnabled()) {
                List<EnvCapabilityRequirement> requirements = function.getEnvCapabilityRequirements();
                boolean isMatch = true;
                for (AnalysisTask.AnalysisConfig config : configs) {
                    if ("apkcanary".equals(config.getExecutor())) {
                        continue;
                    }
                    isMatch = requirements.stream().anyMatch(requirement -> requirement.getName().equals(config.getExecutor()) && requirement.isReady()) && isMatch;
                }
                if (isMatch) {
                    availableAgents.add(tempAgentDeviceGroup);
                }
            }
        }

        Assert.notEmpty(availableAgents, "No available agent found!");
        Collections.shuffle(availableAgents);

        AgentDeviceGroup agentDeviceGroup = null;

        for (AgentDeviceGroup availableAgent : availableAgents) {
            if (!isAgentUpdating(availableAgent.getAgentId()) && availableAgent.getAvailableAnalysisTaskCount().get(testTaskSpec.runningType) > 0) {
                agentDeviceGroup = availableAgent;
                break;
            }
        }
        if (agentDeviceGroup == null) {
            return result;
        }
        testTaskSpec.deviceIdentifier = agentDeviceGroup.getAgentId();
        testTaskSpec.agentIds.add(agentDeviceGroup.getAgentId());
        agentDeviceGroups.get(agentDeviceGroup.getAgentId()).runAnalysisTask(testTaskSpec.runningType);
        Message message = new Message();
        message.setBody(testTaskSpec);
        message.setPath(Const.Path.TEST_TASK_RUN);

        AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(agentDeviceGroup.getAgentId());
        Assert.notNull(agentSessionInfoByAgentId, "Device/Agent Offline!");
        result.put(Const.Param.TEST_DEVICE_SN, agentDeviceGroup.getAgentId());
        message.setBody(testTaskSpec);
        sendMessageToSession(agentSessionInfoByAgentId.session, message);

        return result;
    }

    private JSONObject runT2CTest(TestTaskSpec testTaskSpec) {
        JSONObject result = new JSONObject();
        StorageFileInfo initialTestJson = attachmentService.filterFirstAttachment(testTaskSpec.testFileSet.getAttachments(), StorageFileInfo.FileType.TEST_APP_FILE);
        List<StorageFileInfo> attachmentTestJsonList = attachmentService.filterAttachments(testTaskSpec.testFileSet.getAttachments(), StorageFileInfo.FileType.T2C_JSON_FILE);
        if (initialTestJson != null) {
            attachmentTestJsonList.add(initialTestJson);
        }

        Map<String, Integer> aggregateDeviceCountMap = new HashMap<>();
        for (StorageFileInfo testJsonInfo : attachmentTestJsonList) {
            Map<String, Integer> deviceCountMap = new HashMap<>();

            Path publicFolder = Paths.get(CENTER_FILE_BASE_DIR).normalize().toAbsolutePath();
            Path filePath = publicFolder.resolve(testJsonInfo.getBlobPath()).normalize().toAbsolutePath();
            if (!filePath.startsWith(publicFolder + File.separator)) {
                throw new HydraLabRuntimeException("Invalid blob path");
            }
            File testJsonFile = new File(CENTER_FILE_BASE_DIR, testJsonInfo.getBlobPath());
            TestInfo testInfo;
            try {
                storageServiceClientProxy.download(testJsonFile, testJsonInfo);
                T2CJsonParser t2CJsonParser = new T2CJsonParser(LoggerFactory.getLogger(this.getClass()));
                String testJsonFilePath = CENTER_FILE_BASE_DIR + testJsonInfo.getBlobPath();
                testInfo = t2CJsonParser.parseJsonFile(testJsonFilePath);
            } finally {
                testJsonFile.delete();
            }
            Assert.notNull(testInfo, "Failed to parse the json file for test automation.");
            int edgeCount = 0;
            for (DriverInfo driverInfo : testInfo.getDrivers()) {
                if (driverInfo.getPlatform().equalsIgnoreCase(DeviceType.ANDROID.name())) {
                    deviceCountMap.put(DeviceType.ANDROID.name(), deviceCountMap.getOrDefault(DeviceType.ANDROID.name(), 0) + 1);
                }
                if (driverInfo.getPlatform().equalsIgnoreCase("browser")) {
                    edgeCount++;
                }
                if (driverInfo.getPlatform().equalsIgnoreCase(DeviceType.IOS.name())) {
                    deviceCountMap.put(DeviceType.IOS.name(), deviceCountMap.getOrDefault(DeviceType.IOS.name(), 0) + 1);
                }
                if (driverInfo.getPlatform().equalsIgnoreCase(DeviceType.WINDOWS.name())) {
                    deviceCountMap.put(DeviceType.WINDOWS.name(), deviceCountMap.getOrDefault(DeviceType.WINDOWS.name(), 0) + 1);
                }
            }
            Assert.isTrue(deviceCountMap.getOrDefault(DeviceType.WINDOWS.name(), 0) <= 1, "No enough Windows device to run this test.");
            Assert.isTrue(edgeCount <= 1, "No enough Edge browser to run this test.");
            if (deviceCountMap.getOrDefault(DeviceType.WINDOWS.name(), 0) == 0 && edgeCount == 1) {
                deviceCountMap.put(DeviceType.WINDOWS.name(), 1);
            }
            for (Map.Entry<String, Integer> entry : deviceCountMap.entrySet()) {
                aggregateDeviceCountMap.put(entry.getKey(), Math.max(aggregateDeviceCountMap.getOrDefault(entry.getKey(), 0), entry.getValue()));
            }
        }

        String[] deviceIdentifiers = testTaskSpec.deviceIdentifier.split(",");
        String agentId = null;
        List<DeviceInfo> devices = new ArrayList<>();
        for (String tempIdentifier : deviceIdentifiers) {
            DeviceInfo device = deviceListMap.get(tempIdentifier);
            Assert.notNull(device, "error deviceIdentifier!");
            if (agentId == null) {
                agentId = device.getAgentId();
            }
            Assert.isTrue(agentId.equals(device.getAgentId()), "Device not in the same agent");
            Assert.isTrue(device.isAlive(), "Device offline");
            if (device.isTesting()) {
                return result;
            }
            aggregateDeviceCountMap.put(device.getType(), aggregateDeviceCountMap.getOrDefault(device.getType(), 0) - 1);
            devices.add(device);
        }
        for (Map.Entry<String, Integer> entry : aggregateDeviceCountMap.entrySet()) {
            Assert.isTrue(entry.getValue() <= 0, "No enough " + entry.getKey() + " device to run this test.");
        }
        Message message = new Message();
        message.setBody(testTaskSpec);
        message.setPath(Const.Path.TEST_TASK_RUN);

        AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(agentId);
        Assert.notNull(agentSessionInfoByAgentId, "Device/Agent Offline!");
        if (isAgentUpdating(agentSessionInfoByAgentId.agentUser.getId())) {
            return result;
        }
        for (DeviceInfo device : devices) {
            updateDeviceStatus(device.getSerialNum(), DeviceInfo.TESTING, testTaskSpec.testTaskId);
        }
        testTaskSpec.agentIds.add(agentId);
        sendMessageToSession(agentSessionInfoByAgentId.session, message);
        result.put(Const.Param.TEST_DEVICE_SN, testTaskSpec.deviceIdentifier);

        return result;
    }

    private JSONObject runAppiumTestTask(TestTaskSpec testTaskSpec) {
        JSONObject result = new JSONObject();

        String[] deviceIdentifiers = testTaskSpec.deviceIdentifier.split(",");
        String agentId = null;
        List<DeviceInfo> devices = new ArrayList<>();
        for (String tempIdentifier : deviceIdentifiers) {
            DeviceInfo device = deviceListMap.get(tempIdentifier);
            Assert.notNull(device, "error deviceIdentifier!");
            if (agentId == null) {
                agentId = device.getAgentId();
            }
            Assert.isTrue(agentId.equals(device.getAgentId()), "Device not in the same agent");
            Assert.isTrue(device.isAlive(), "Device offline");
            if (device.isTesting()) {
                return result;
            }
            devices.add(device);
        }
        Message message = new Message();
        message.setBody(testTaskSpec);
        message.setPath(Const.Path.TEST_TASK_RUN);

        AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(agentId);
        Assert.notNull(agentSessionInfoByAgentId, "Device/Agent Offline!");
        if (isAgentUpdating(agentSessionInfoByAgentId.agentUser.getId())) {
            return result;
        }
        for (DeviceInfo device : devices) {
            updateDeviceStatus(device.getSerialNum(), DeviceInfo.TESTING, testTaskSpec.testTaskId);
        }
        testTaskSpec.agentIds.add(agentId);
        sendMessageToSession(agentSessionInfoByAgentId.session, message);
        result.put(Const.Param.TEST_DEVICE_SN, testTaskSpec.deviceIdentifier);

        return result;
    }

    private JSONObject runTestTaskByGroup(TestTaskSpec testTaskSpec) {
        JSONObject result = new JSONObject();
        boolean isAllOffline = true;
        Set<String> deviceSerials = deviceGroupListMap.get(testTaskSpec.deviceIdentifier);
        Assert.notNull(deviceSerials, "error deviceIdentifier or there is no devices in the group!");
        Assert.isTrue(deviceSerials.size() > 0, "error deviceIdentifier or there is no devices in the group!");
        DeviceGroup deviceGroup = deviceGroupService.getGroupByName(testTaskSpec.deviceIdentifier);
        Assert.notNull(deviceGroup, "error deviceIdentifier !");
        Map<String, List<String>> testAgentDevicesMap = new HashMap<>();
        boolean isSingle = Const.DeviceGroup.SINGLE_TYPE.equals(testTaskSpec.groupTestType);
        boolean isAll = Const.DeviceGroup.ALL_TYPE.equals(testTaskSpec.groupTestType);
        Message message = new Message();
        message.setPath(Const.Path.TEST_TASK_RUN);

        ArrayList<String> deviceSerialList = new ArrayList<>(deviceSerials);
        Collections.shuffle(deviceSerialList);

        for (String deviceSerial : deviceSerialList) {
            DeviceInfo device = deviceListMap.get(deviceSerial);
            if (device == null || !device.isAlive() || isAgentUpdating(device.getAgentId())) {
                Assert.isTrue(!isAll, "Device/Agent Offline!");
                continue;
            }

            if (isDeviceBlocked(deviceSerial)) {
                Assert.isTrue(!isAll, "Some of the devices in the device group are blocked!");
                continue;
            }

            isAllOffline = false;
            if (device.isOnline()) {
                List<String> devices = testAgentDevicesMap.getOrDefault(device.getAgentId(), new ArrayList<>());
                devices.add(device.getSerialNum());
                testAgentDevicesMap.put(device.getAgentId(), devices);
                testTaskSpec.agentIds.add(device.getAgentId());
                if (testTaskSpec.blockDevice) {
                    blockDevice(deviceSerial, testTaskSpec.testTaskId);
                }
                if (isSingle) {
                    break;
                }
            } else if (isAll) {
                return result;
            }
        }
        Assert.isTrue(!isAllOffline, "All Device/Agent Offline!");
        for (String agentId : testAgentDevicesMap.keySet()) {
            AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(agentId);
            List<String> testDeviceSerials = testAgentDevicesMap.get(agentId);
            for (String temp : testDeviceSerials) {
                updateDeviceStatus(temp, DeviceInfo.TESTING, testTaskSpec.testTaskId);
            }
            String groupDevices = String.join(",", testDeviceSerials);
            Assert.notNull(agentSessionInfoByAgentId, "Device/Agent Offline!");
            if (result.get(Const.Param.TEST_DEVICE_SN) == null) {
                result.put(Const.Param.TEST_DEVICE_SN, groupDevices);
            } else {
                result.put(Const.Param.TEST_DEVICE_SN, result.get(Const.Param.TEST_DEVICE_SN) + "," + groupDevices);
            }
            testTaskSpec.groupDevices = groupDevices;
            if (testTaskSpec.blockDevice) {
                testTaskSpec.blockedDeviceSerialNumber = groupDevices;
                testTaskSpec.unblockDeviceSecretKey = testTaskSpec.testTaskId;
            }
            message.setBody(testTaskSpec);
            sendMessageToSession(agentSessionInfoByAgentId.session, message);
        }

        return result;
    }

    private JSONObject runTestTaskByDevice(TestTaskSpec testTaskSpec) {
        JSONObject result = new JSONObject();

        DeviceInfo device = deviceListMap.get(testTaskSpec.deviceIdentifier);
        Assert.notNull(device, "error deviceIdentifier!");
        Message message = new Message();
        message.setBody(testTaskSpec);
        message.setPath(Const.Path.TEST_TASK_RUN);

        Assert.isTrue(device.isAlive(), "Device/Agent Offline!");
        if (device.isTesting() || (!isRunOnBlockedDevice(testTaskSpec) && isDeviceBlocked(testTaskSpec.deviceIdentifier))) {
            return result;
        }
        AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(device.getAgentId());

        Assert.notNull(agentSessionInfoByAgentId, "Device/Agent Offline!");

        if (isAgentUpdating(agentSessionInfoByAgentId.agentUser.getId())) {
            return result;
        }
        updateDeviceStatus(device.getSerialNum(), DeviceInfo.TESTING, testTaskSpec.testTaskId);

        if (testTaskSpec.unblockDevice) {
            unBlockDevice(testTaskSpec.deviceIdentifier, testTaskSpec.unblockDeviceSecretKey);
            testTaskSpec.unblockedDeviceSerialNumber = testTaskSpec.deviceIdentifier;
        }

        if (testTaskSpec.blockDevice) {
            blockDevice(testTaskSpec.deviceIdentifier, testTaskSpec.testTaskId);
            testTaskSpec.blockedDeviceSerialNumber = testTaskSpec.deviceIdentifier;
            testTaskSpec.unblockDeviceSecretKey = testTaskSpec.testTaskId;
        }
        testTaskSpec.agentIds.add(device.getAgentId());
        sendMessageToSession(agentSessionInfoByAgentId.session, message);
        result.put(Const.Param.TEST_DEVICE_SN, testTaskSpec.deviceIdentifier);
        return result;
    }

    private AgentSessionInfo getAgentSessionInfoByAgentId(String agentId) {
        for (Map.Entry<String, AgentSessionInfo> entry : agentSessionMap.entrySet()) {
            AgentSessionInfo sessionInfo = entry.getValue();
            if (sessionInfo.agentUser.getId().equals(agentId)) {
                return sessionInfo;
            }
        }
        return null;
    }

    private boolean checkIsSessionAliveByAgentId(String agentId) {

        for (Map.Entry<String, AgentSessionInfo> entry : agentSessionMap.entrySet()) {
            AgentSessionInfo sessionInfo = entry.getValue();
            if (sessionInfo.agentUser.getId().equals(agentId)) {
                if (sessionInfo.session != null && sessionInfo.session.isOpen()) {
                    return true;
                } else {
                    agentSessionMap.remove(entry.getKey());
                    log.info("Session of agent {} is not alive.", entry.getValue().agentUser.getName());
                    metricUtil.updateAgentAliveStatus(entry.getValue().agentUser.getId(), GlobalConstant.AgentLiveStatus.OFFLINE.getStatus());
                }
            }
        }

        AgentDeviceGroup agentDeviceGroup = agentDeviceGroups.remove(agentId);
        if (agentDeviceGroup == null || agentDeviceGroup.getDevices() == null) {
            return false;
        }
        removeDevices(agentDeviceGroup.getDevices());

        return false;
    }

    public Boolean isAgentBusy(String agentId) {
        AgentDeviceGroup agentDeviceGroup = agentDeviceGroups.get(agentId);
        if (agentDeviceGroup == null) {
            return false;
        }
        for (DeviceInfo deviceInfo : agentDeviceGroup.getDevices()) {
            if (DeviceInfo.TESTING.equals(deviceInfo.getStatus())) {
                return true;
            }
        }
        return false;
    }

    public Boolean isAgentUpdating(String agentId) {
        AgentUpdateTask tempTask = agentUpdateMap.get(agentId);
        if (tempTask != null) {
            return AgentUpdateTask.TaskConst.STATUS_UPDATING.equals(tempTask.getUpdateStatus());
        }
        return false;
    }

    public void updateAgentPackage(String agentId, String fileId) throws Exception {
        //check is agent connected
        AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(agentId);
        Assert.notNull(agentSessionInfoByAgentId, "Agent Offline!");
        StorageFileInfo packageInfo = storageFileInfoRepository.findById(fileId).get();
        if (packageInfo == null || !StorageFileInfo.FileType.AGENT_PACKAGE.equals(packageInfo.getFileType())) {
            throw new Exception("Error file info!");
        }
        AgentUser agentUser = agentSessionInfoByAgentId.agentUser;

        //check is agent busy and update status
        Assert.isTrue(!isAgentBusy(agentId), "Agent Is Busy! Please Wait For A Second!");

        //Start update agent
        agentDeviceGroups.get(agentId).setAgentStatus(AgentDeviceGroup.Status.UPDATING);
        AgentUpdateTask updateTask = new AgentUpdateTask();
        agentUpdateMap.put(agentId, updateTask);

        updateTask.setUpdateStatus(AgentUpdateTask.TaskConst.STATUS_UPDATING);
        updateTask.setAgentId(agentUser.getId());
        updateTask.setAgentName(agentUser.getName());
        updateTask.setOriginVersionName(agentUser.getVersionName());
        updateTask.setOriginVersionCode(agentUser.getVersionCode());
        updateTask.setTargetVersionName(packageInfo.getFileParser().getString(AgentUpdateTask.TaskConst.PARAM_VERSION_NAME));
        updateTask.setTargetVersionCode(packageInfo.getFileParser().getString(AgentUpdateTask.TaskConst.PARAM_VERSION_CODE));
        updateTask.setPackageInfo(packageInfo);

        Message message = new Message();
        message.setBody(updateTask);
        message.setPath(Const.Path.AGENT_UPDATE);

        sendMessageToSession(agentSessionInfoByAgentId.session, message);
    }

    public void restartAgent(String agentId) {
        //check is agent connected
        AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(agentId);
        Assert.notNull(agentSessionInfoByAgentId, "Agent Offline!");

        //Start restart agent
        Message message = new Message();
        message.setPath(Const.Path.AGENT_RESTART);
        sendMessageToSession(agentSessionInfoByAgentId.session, message);
    }

    public AgentUpdateTask getUpdateTask(String agentId) {
        return agentUpdateMap.get(agentId);
    }

    public List<AgentUpdateTask> getUpdateTasks() {
        List<AgentUpdateTask> res = new ArrayList<>();
        Set<String> keys = agentUpdateMap.keySet();
        for (String key : keys) {
            res.add(agentUpdateMap.get(key));
        }
        return new ArrayList<>(res);
    }

    public void updateAgentDeviceGroupTeam(String teamId, String teamName) {
        List<AgentUser> agents = agentManageService.getAgentsByTeamId(teamId);
        synchronized (agentDeviceGroups) {
            agents.forEach(agent -> agentDeviceGroups.get(agent.getId()).setTeamName(teamName));
        }
    }

    public int getAliveAgentNum() {
        return agentSessionMap.size();
    }

    public int getAliveDeviceNum() {
        return (int) deviceListMap.values().stream().filter(DeviceInfo::isAlive).count();
    }

    public void blockDevice(String deviceIdentifier, String testTaskId) {
        synchronized (blockedDevicesMap) {
            if (blockedDevicesMap.containsKey(deviceIdentifier)) {
                throw new IllegalArgumentException("Device is already blocked by some other task!");
            }

            BlockedDeviceInfo blockedDeviceInfo = new BlockedDeviceInfo();
            blockedDeviceInfo.setBlockedTime(Instant.now());
            blockedDeviceInfo.setBlockingTaskUUID(testTaskId);
            blockedDeviceInfo.setBlockedDeviceSerialNumber(deviceIdentifier);
            blockedDevicesMap.put(deviceIdentifier,blockedDeviceInfo);
        }
    }

    public boolean isDeviceBlocked(String deviceIdentifier) {
        synchronized (blockedDevicesMap) {
            return blockedDevicesMap.containsKey(deviceIdentifier);
        }
    }

    public boolean areAllDevicesBlocked(String deviceIdentifier) {
        if (deviceIdentifier.startsWith(Const.DeviceGroup.GROUP_NAME_PREFIX)) {
            Set<String> deviceSerials = queryDeviceByGroup(deviceIdentifier);
            synchronized (blockedDevicesMap) {
                for (String deviceSerial : deviceSerials) {
                    if (!blockedDevicesMap.containsKey(deviceSerial)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public void unBlockDevice(String deviceIdentifier, String unblockDeviceSecretKey) {
        synchronized (blockedDevicesMap) {
            if (blockedDevicesMap.containsKey(deviceIdentifier)) {
                BlockedDeviceInfo blockedDeviceInfo = blockedDevicesMap.get(deviceIdentifier);
                if (blockedDeviceInfo.getBlockingTaskUUID().equals(unblockDeviceSecretKey)) {
                    blockedDevicesMap.remove(deviceIdentifier);
                } else {
                    throw new IllegalArgumentException("Invalid unblock device secret key!");
                }
            } else {
                log.info("Device {} is already unblocked.", deviceIdentifier);
            }
        }
    }

    public void unblockFrozenBlockedDevices() {

        if (isUnblockingDevices.get()){
            return;
        }
        isUnblockingDevices.set(true);
        synchronized (blockedDevicesMap) {
            Instant currentTime = Instant.now();
            Iterator<Map.Entry<String, BlockedDeviceInfo>> iterator = blockedDevicesMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, BlockedDeviceInfo> entry = iterator.next();
                Instant blockedTime = entry.getValue().getBlockedTime();
                Duration durationBlocked = Duration.between(blockedTime, currentTime);
                if (durationBlocked.compareTo(Const.DeviceGroup.BLOCKED_DEVICE_TIMEOUT) > 0) {
                    log.info("Unblocking device {} since it has been blocked for more than {} hours.", entry.getKey(), durationBlocked);
                    iterator.remove();
                }
            }
        }
        isUnblockingDevices.set(false);
    }

    public boolean isRunOnBlockedDevice(TestTaskSpec testTaskSpec) {
        if (testTaskSpec.unblockDeviceSecretKey == null || testTaskSpec.unblockDeviceSecretKey.isEmpty()) {
            return false;
        }

        synchronized (blockedDevicesMap) {
            BlockedDeviceInfo blockedDeviceInfo = blockedDevicesMap.get(testTaskSpec.deviceIdentifier);

            if (blockedDeviceInfo == null) {
                return false;
            }
            return blockedDeviceInfo.getBlockingTaskUUID().equals(testTaskSpec.unblockDeviceSecretKey);
        }
    }

    @Scheduled(cron = "0 * * * * *")
    public void recordStatisticData() {
        int currentAgentNum = getAliveAgentNum();
        int currentDeviceNum = getAliveDeviceNum();

        statisticDataRepository.save(new StatisticData("agent_num", currentAgentNum));
        log.info("Storing current online agent number {}.", currentAgentNum);
        statisticDataRepository.save(new StatisticData("device_num", currentDeviceNum));
        log.info("Storing current online device number {}.", currentDeviceNum);
    }

    static class AgentSessionInfo {
        Session session;
        AgentUser agentUser;

        public AgentSessionInfo(Session session, AgentUser agentUser) {
            this.session = session;
            this.agentUser = agentUser;
        }
    }
}
