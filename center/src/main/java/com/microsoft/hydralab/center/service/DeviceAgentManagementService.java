// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.service;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.android.ddmlib.IDevice;
import com.microsoft.hydralab.center.repository.AgentUserRepository;
import com.microsoft.hydralab.center.util.MetricUtil;
import com.microsoft.hydralab.common.entity.agent.MobileDevice;
import com.microsoft.hydralab.common.entity.center.*;
import com.microsoft.hydralab.common.entity.common.*;
import com.microsoft.hydralab.common.repository.BlobFileInfoRepository;
import com.microsoft.hydralab.common.util.*;
import com.microsoft.hydralab.t2c.runner.DriverInfo;
import com.microsoft.hydralab.t2c.runner.T2CJsonParser;
import com.microsoft.hydralab.t2c.runner.TestInfo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    @Resource
    MetricUtil metricUtil;
    @Resource
    AgentUserRepository agentUserRepository;
    @Resource
    TestDataService testDataService;
    @Resource
    DeviceGroupService deviceGroupService;
    @Resource
    TestTaskService testTaskService;
    @Resource
    BlobFileInfoRepository blobFileInfoRepository;
    @Resource
    AttachmentService attachmentService;
    @Resource
    AgentManageService agentManageService;

    @Value("${app.access-token-limit}")
    int accessLimit;
    private long lastTimeRequest;

    public void onOpen(Session session) {
        onlineCount.incrementAndGet();
        log.info("New session checked in, id: {}, online count: {}", session.getId(), onlineCount.get());
        requestAuth(session);
    }

    @Scheduled(cron = "*/30 * * * * *")
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

    private void requestAuth(Session session) {
        sendMessageToSession(session, Message.auth());
    }

    public void onMessage(Message message, Session session) {
        AgentSessionInfo savedSession = agentSessionMap.get(session.getId());
        if (savedSession == null) {
            AgentUser agentUser = searchQualifiedAgent(message);
            if (agentUser == null) {
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Not permitted"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                if (agentDeviceGroups.get(agentUser.getId()) != null && checkIsSessionAliveByAgentId(agentUser.getId())) {
                    try {
                        session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "AgentID has been used"));
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                agentSessionMap.put(session.getId(), new AgentSessionInfo(session, agentUser));
                metricUtil.registerAgentAliveStatusMetric(agentUser);

                log.info("Session {} is saved to map as registered agent, associated agent {}", session.getId(), message.getBody());

                //check is not agent update success
                AgentUpdateTask tempTask = agentUpdateMap.get(agentUser.getId());
                if (tempTask != null) {
                    log.info("Session {} is saved to map as registered agent, associated agent {}", session.getId(), message.getBody());

                    AgentUpdateTask.UpdateMsg updateMag = null;
                    String agentMessage = "Agent Reconnected After Updating.Version is " + agentUser.getVersionName();
                    if (agentUser.getVersionName() == null || !agentUser.getVersionName().equals(tempTask.getTargetVersionName())) {
                        tempTask.setUpdateStatus(AgentUpdateTask.TaskConst.STATUS_FAIL);
                        updateMag = new AgentUpdateTask.UpdateMsg(false, agentMessage, agentUser.getId());
                    } else {
                        tempTask.setUpdateStatus(AgentUpdateTask.TaskConst.STATUS_SUCCESS);
                        updateMag = new AgentUpdateTask.UpdateMsg(true, agentMessage, "");
                    }

                    tempTask.getUpdateMsgs().add(updateMag);
                }
                requestList(session);
            }
        } else {
            handleQualifiedAgentMessage(message, savedSession);
        }
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
                    updateDeviceStatus(data.getString(Const.AgentConfig.serial_param), data.getString(Const.AgentConfig.status_param), data.getString(Const.AgentConfig.task_id_param));
                }
                break;
            case Const.Path.ACCESS_INFO:
                if (message.getBody() instanceof AccessInfo) {
                    AccessInfo accessInfo = (AccessInfo) message.getBody();
                    updateAccessInfo(accessInfo);
                }
                break;
            case Const.Path.TEST_TASK_UPDATE:
                if (message.getBody() instanceof TestTask) {
                    TestTask testTask = (TestTask) message.getBody();
                    boolean isFinished = testTask.getStatus().equals(TestTask.TestStatus.FINISHED);
                    testDataService.saveTestTaskDataFromAgent(testTask, isFinished, savedSession.agentUser.getId());

                    //after the task finishing, update the status of device used
                    if (isFinished) {
                        List<DeviceTestTask> deviceTestResults = testTask.getDeviceTestResults();
                        for (DeviceTestTask deviceTestResult : deviceTestResults) {
                            updateDeviceStatus(deviceTestResult.getDeviceSerialNumber(), DeviceInfo.ONLINE, null);
                        }
                        //run the task saved in queue
                        testTaskService.runTask();
                    }
                }
                break;
            case Const.Path.TEST_TASK_RUN:
                log.info("Message from agent {}", message);
                break;
            case Const.Path.TEST_TASK_RETRY:
                if (message.getBody() instanceof TestTask) {
                    TestTask testTask = (TestTask) message.getBody();
                    if (testTask.getRetryTime() == Const.AgentConfig.retry_time) {
                        testTask.setStatus(TestTask.TestStatus.EXCEPTION);
                        testTask.setTestErrorMsg("Device offline!");
                        testDataService.saveTestTaskData(testTask, false);
                    } else {
                        TestTaskSpec taskSpec = TestTask.convertToTestTaskSpec(testTask);
                        taskSpec.retryTime++;
                        testTaskService.addTask(taskSpec);
                        cancelTestTaskById(testTask.getId(), false);
                        //run the task saved in queue
                        testTaskService.runTask();
                    }
                }
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

    public void cancelTestTaskById(String taskId, boolean isSaveDB) {
        Set<String> agentIds = testDataService.cancelTaskById(taskId, isSaveDB);
        JSONObject data = new JSONObject();
        Message message = new Message();
        data.put(Const.AgentConfig.task_id_param, taskId);
        message.setPath(Const.Path.TEST_TASK_CANCEL);
        message.setBody(data);
        for (String agentId : agentIds) {
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
                    break;
                }
            }
        }
        testTaskService.runTask();
    }

    public void checkAccessInfo(String name, String key) {
        if (key == null) {
            throw new RuntimeException("access key is required!");
        }
        AccessInfo accessInfo = accessInfoMap.get(name);
        if (accessInfo != null) {
            throw new RuntimeException("please generate access key first!");
        }
        if (!key.equals(accessInfo.getKey())) {
            throw new RuntimeException("error access key!");
        }
        int hour = (int) ((new Date().getTime() - accessInfo.getIngestTime().getTime()) / 1000 / 60 / 60);
        if (hour > accessLimit) {
            throw new RuntimeException("access key has expired!");
        }
    }

    private void updateAgentDeviceGroup(AgentSessionInfo savedSession, List<DeviceInfo> latestDeviceInfos) {

        updateDeviceGroup(latestDeviceInfos, savedSession.agentUser.getId());

        AgentDeviceGroup agentDeviceGroup = agentDeviceGroups.get(savedSession.agentUser.getId());
        if (agentDeviceGroup != null) {
            // update
            updateAgentDevices(latestDeviceInfos, agentDeviceGroup);
        } else {
            AgentDeviceGroup newAgentDeviceGroup = new AgentDeviceGroup();
            newAgentDeviceGroup.initWithAgentUser(savedSession.agentUser);
            newAgentDeviceGroup.setDevices(new ArrayList<>(latestDeviceInfos));
            agentDeviceGroups.put(savedSession.agentUser.getId(), newAgentDeviceGroup);
            log.info("Adding info of new agent: {}, device SN: {}", newAgentDeviceGroup.getAgentName(), latestDeviceInfos.stream().map(MobileDevice::getSerialNum).collect(Collectors.joining(",")));
        }
    }

    public void updateDeviceGroup(List<DeviceInfo> deviceInfos, String agentId) {
        for (DeviceInfo deviceInfo : deviceInfos) {
            //init agent info
            deviceInfo.setAgentId(agentId);

            //if the status saved in master is testing, the value will not be covered
            if (deviceListMap.get(deviceInfo.getSerialNum()) != null && deviceListMap.get(deviceInfo.getSerialNum()).isTesting()) {
                deviceInfo.setStatus(DeviceInfo.TESTING);
            }

            deviceListMap.put(deviceInfo.getSerialNum(), deviceInfo);

            //init group info
            List<DeviceGroupRelation> groups = deviceGroupService.getGroupByDevice(deviceInfo.getSerialNum());
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

    public List<DeviceInfo> queryDeviceInfoByGroup(String groupName){
        List<DeviceInfo> devices = new ArrayList<>();
        Set<String> serials = deviceGroupListMap.get(groupName);
        serials.forEach(serial -> devices.add(deviceListMap.get(serial)));

        return devices;
    }

    public Set<String> queryGroupByDevice(String deviceSerial) {
        DeviceInfo deviceInfo = deviceListMap.get(deviceSerial);
        Set<String> groups = deviceInfo.getDeviceGroup();
        if (groups == null) {
            return new HashSet<>();
        }
        return new HashSet<>(groups);
    }

    //add group&device relation and check device access
    public void addDeviceToGroup(String group, String serialNum, String accessKey) {
        DeviceInfo device = deviceListMap.get(serialNum);
        if (device.getIsPrivate()) {
            checkAccessInfo(serialNum, accessKey);
        }
        addDeviceToGroup(group, serialNum);
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

    public boolean checkDeviceAuthorization(SysUser requestor, String serialNum) throws IllegalArgumentException{
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
        data.put(Const.AgentConfig.serial_param, deviceSerial);
        data.put(Const.AgentConfig.scope_param, isPrivate);

        Message message = new Message();
        message.setPath(Const.Path.DEVICE_UPDATE);
        message.setBody(data);

        sendMessageToSession(agentSession.session, message);
    }

    private void updateAgentDevices(List<DeviceInfo> latestDeviceInfos, AgentDeviceGroup agentDeviceGroup) {
        for (DeviceInfo newDeviceInfo : latestDeviceInfos) {
            boolean hasDevice = false;
            for (DeviceInfo device : agentDeviceGroup.getDevices()) {
                //if the status saved in master is testing, the value will not be covered
                if (deviceListMap.get(device.getSerialNum()) != null && deviceListMap.get(device.getSerialNum()).isTesting()) {
                    device.setStatus(DeviceInfo.TESTING);
                    hasDevice = true;
                    log.info("Updating device status of agent: {}, device SN: {}", agentDeviceGroup.getAgentName(), newDeviceInfo.getSerialNum());
                    break;
                }
                if (device.getSerialNum().equals(newDeviceInfo.getSerialNum())) {
                    hasDevice = true;
                    BeanUtil.copyProperties(newDeviceInfo, device);
                    log.info("Updating device info of agent: {}, device SN: {}", agentDeviceGroup.getAgentName(), newDeviceInfo.getSerialNum());
                    break;
                }
            }
            if (!hasDevice) {
                log.info("Adding device info of agent: {}, device SN: {}", agentDeviceGroup.getAgentName(), newDeviceInfo.getSerialNum());
                agentDeviceGroup.getDevices().add(newDeviceInfo);
            }
        }
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
        user.setDeviceType(agentUser.getDeviceType());
        user.setHostname(agentUser.getHostname());
        user.setIp(agentUser.getIp());
        user.setVersionName(agentUser.getVersionName());
        user.setVersionCode(agentUser.getVersionCode());
        user.setSecret(null);
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
            if (agent.getAgentDeviceType() != AgentUser.DeviceType.WINDOWS) {
                continue;
            }
            List<DeviceInfo> devices = agent.getDevices();
            if (devices.size() != 1 || !devices.get(0).isAlive()) {
                continue;
            }
            res.add(agent);
        }
        return new ArrayList<>(res);
    }

    // Todo: Get agent list for android and ios agent

    public JSONObject runTestTaskBySpec(TestTaskSpec testTaskSpec) {
        JSONObject result;

        if (TestTask.TestRunningType.APPIUM_CROSS.equals(testTaskSpec.runningType)) {
            result = runAppiumTestTask(testTaskSpec);
        } else if (TestTask.TestRunningType.T2C_JSON_TEST.equals(testTaskSpec.runningType)) {
            result = runT2CTest(testTaskSpec);
        } else {
            if (testTaskSpec.deviceIdentifier.startsWith(Const.DeviceGroup.groupPre)) {
                result = runTestTaskByGroup(testTaskSpec);
            } else {
                result = runTestTaskByDevice(testTaskSpec);
            }
        }
        return result;
    }

    private JSONObject runT2CTest(TestTaskSpec testTaskSpec) {
        // TODO: upgrade to assign task to agent and check the available device count on the agent
        JSONObject result = new JSONObject();
        BlobFileInfo testAppFileInfo = attachmentService.filterFirstAttachment(testTaskSpec.testFileSet.getAttachments(), BlobFileInfo.FileType.TEST_APP_FILE);
        Assert.notNull(testAppFileInfo, "The testFileSet don't contain a test file!");
        File testApkFile = new File(CENTER_FILE_BASE_DIR, testAppFileInfo.getBlobPath());
        TestInfo testInfo;
        try {
            try {
                DownloadUtils.downloadFileFromUrl(testAppFileInfo.getBlobUrl(), testApkFile.getName(), testApkFile.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }

            T2CJsonParser t2CJsonParser = new T2CJsonParser(LoggerFactory.getLogger(this.getClass()));
            String testJsonFilePath = CENTER_FILE_BASE_DIR + testAppFileInfo.getBlobPath();
            testInfo = t2CJsonParser.parseJsonFile(testJsonFilePath);
        } finally {
            testApkFile.delete();
        }
        Assert.notNull(testInfo, "Failed to parse the json file for test automation.");

        int androidCount = 0, windowsCount = 0;

        for (DriverInfo driverInfo : testInfo.getDrivers()) {
            if (driverInfo.getPlatform().equalsIgnoreCase("android")) {
                androidCount++;
            }
            if (driverInfo.getPlatform().equalsIgnoreCase("windows")) {
                windowsCount++;
            }
            if (driverInfo.getPlatform().equalsIgnoreCase("ios")) {
                throw new RuntimeException("No iOS device connected to this agent");
            }
        }
        Assert.isTrue(androidCount <= 1, "No enough Android device to run this test.");
        Assert.isTrue(windowsCount <= 1, "No enough Windows device to run this test.");

        // Todo: leveraged current E2E agent, need to update to agent level test
        AgentDeviceGroup agentDeviceGroup = agentDeviceGroups.get(testTaskSpec.deviceIdentifier);
        Assert.notNull(agentDeviceGroup, "Error identifier or agent offline");
        List<DeviceInfo> devices = agentDeviceGroup.getDevices();
        Assert.notNull(devices, "Agent has no device");
        Assert.isTrue(devices.size() == 1, "The number of device is not suitable");
        DeviceInfo device = devices.get(0);
        Assert.isTrue(device.isAlive(), "Device offline");

        if (device.isTesting()) {
            return result;
        }

        Message message = new Message();
        message.setBody(testTaskSpec);
        message.setPath(Const.Path.TEST_TASK_RUN);

        AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(testTaskSpec.deviceIdentifier);
        Assert.notNull(agentSessionInfoByAgentId, "Device/Agent Offline!");
        if (isAgentUpdating(agentSessionInfoByAgentId.agentUser.getId())) {
            return result;
        }
        updateDeviceStatus(device.getSerialNum(), DeviceInfo.TESTING, testTaskSpec.testTaskId);
        testTaskSpec.agentIds.add(testTaskSpec.deviceIdentifier);
        sendMessageToSession(agentSessionInfoByAgentId.session, message);
        result.put(Const.Param.TEST_DEVICE_SN, device.getSerialNum());

        return result;
    }

    private JSONObject runAppiumTestTask(TestTaskSpec testTaskSpec) {
        JSONObject result = new JSONObject();

        AgentDeviceGroup agentDeviceGroup = agentDeviceGroups.get(testTaskSpec.deviceIdentifier);
        Assert.notNull(agentDeviceGroup, "Error identifier or agent offline");
        List<DeviceInfo> devices = agentDeviceGroup.getDevices();
        Assert.notNull(devices, "Agent has no device");
        Assert.isTrue(devices.size() == 1, "The number of device is not suitable");
        DeviceInfo device = devices.get(0);
        Assert.isTrue(device.isAlive(), "Device offline");

        if (device.isTesting()) {
            return result;
        }

        Message message = new Message();
        message.setBody(testTaskSpec);
        message.setPath(Const.Path.TEST_TASK_RUN);

        AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(testTaskSpec.deviceIdentifier);
        Assert.notNull(agentSessionInfoByAgentId, "Device/Agent Offline!");
        if (isAgentUpdating(agentSessionInfoByAgentId.agentUser.getId())) {
            return result;
        }
        updateDeviceStatus(device.getSerialNum(), DeviceInfo.TESTING, testTaskSpec.testTaskId);
        testTaskSpec.agentIds.add(testTaskSpec.deviceIdentifier);
        sendMessageToSession(agentSessionInfoByAgentId.session, message);
        result.put(Const.Param.TEST_DEVICE_SN, device.getSerialNum());

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
        if (deviceGroup.getIsPrivate()) {
            checkAccessInfo(testTaskSpec.deviceIdentifier, testTaskSpec.accessKey);
        }
        Map<String, List<String>> agents = new HashMap<>();
        boolean isSingle = Const.DeviceGroup.singleType.equals(testTaskSpec.groupTestType);
        boolean isAll = Const.DeviceGroup.allType.equals(testTaskSpec.groupTestType);
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
            isAllOffline = false;
            if (device.isOnline()) {
                updateDeviceStatus(deviceSerial, DeviceInfo.TESTING, testTaskSpec.testTaskId);
                List<String> devices = agents.getOrDefault(device.getAgentId(), new ArrayList<>());
                devices.add(device.getSerialNum());
                agents.put(device.getAgentId(), devices);
                testTaskSpec.agentIds.add(device.getAgentId());
                if (isSingle) {
                    break;
                }
            } else if (isAll) {
                return result;
            }
        }
        Assert.isTrue(!isAllOffline, "All Device/Agent Offline!");
        for (String agentId : agents.keySet()) {
            AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(agentId);
            String groupDevices = String.join(",", agents.get(agentId));
            Assert.notNull(agentSessionInfoByAgentId, "Device/Agent Offline!");
            if (result.get(Const.Param.TEST_DEVICE_SN) == null) {
                result.put(Const.Param.TEST_DEVICE_SN, groupDevices);
            } else {
                result.put(Const.Param.TEST_DEVICE_SN, result.get(Const.Param.TEST_DEVICE_SN) + "," + groupDevices);
            }
            testTaskSpec.groupDevices = groupDevices;
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
        if (device.getIsPrivate()) {
            checkAccessInfo(device.getSerialNum(), testTaskSpec.accessKey);
        }
        Assert.isTrue(device.isAlive(), "Device/Agent Offline!");
        if (device.isTesting()) {
            return result;
        }
        AgentSessionInfo agentSessionInfoByAgentId = getAgentSessionInfoByAgentId(device.getAgentId());
        Assert.notNull(agentSessionInfoByAgentId, "Device/Agent Offline!");
        if (isAgentUpdating(agentSessionInfoByAgentId.agentUser.getId())) {
            return result;
        }
        updateDeviceStatus(device.getSerialNum(), DeviceInfo.TESTING, testTaskSpec.testTaskId);
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
        BlobFileInfo packageInfo = blobFileInfoRepository.findById(fileId).get();
        if (packageInfo == null || !BlobFileInfo.FileType.AGENT_PACKAGE.equals(packageInfo.getFileType())) {
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

    static class AgentSessionInfo {
        Session session;
        AgentUser agentUser;

        public AgentSessionInfo(Session session, AgentUser agentUser) {
            this.session = session;
            this.agentUser = agentUser;
        }
    }
}
