// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.controller;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.service.AgentManageService;
import com.microsoft.hydralab.center.service.DeviceAgentManagementService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.AgentDeviceGroup;
import com.microsoft.hydralab.common.entity.center.DeviceGroup;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.DeviceOperation;
import com.microsoft.hydralab.common.util.Const;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class DeviceManageController {
    @Resource
    AgentManageService agentManageService;
    @Resource
    private DeviceAgentManagementService deviceAgentManagementService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private UserTeamManagementService userTeamManagementService;

    /**
     * Authenticated USER: all
     * Data access:
     * 1) For users with ROLE SUPER_ADMIN/ADMIN, return all agents with devices connected.
     * 2) For the rest users, return agents with non-private devices, and agents with devices connected that are in user's TEAMs.
     */
    @GetMapping(Const.Path.DEVICE_LIST)
    public Result<List<AgentDeviceGroup>> list(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }

        deviceAgentManagementService.requestAllAgentDeviceListUpdate();
        List<AgentDeviceGroup> deviceGroupList = deviceAgentManagementService.getAgentDeviceGroups();
        if (!sysUserService.checkUserAdmin(requestor)) {
            deviceGroupList.forEach(agentDeviceGroup -> {
                if (!userTeamManagementService.checkRequestorTeamRelation(requestor, agentDeviceGroup.getTeamId())) {
                    List<DeviceInfo> devices = agentDeviceGroup.getDevices();
                    List<DeviceInfo> newDevices = new ArrayList<>();
                    devices.forEach(device -> {
                        if (!device.getIsPrivate()) {
                            newDevices.add(device);
                        }
                    });
                    agentDeviceGroup.setDevices(newDevices);
                }
            });
        }
        deviceGroupList = deviceGroupList.stream()
                .filter(agentDeviceGroup -> agentDeviceGroup.getDevices() != null && agentDeviceGroup.getDevices().size() > 0)
                .sorted((a, b) -> b.getDevices().size() - a.getDevices().size())
                .collect(Collectors.toList());

        return Result.ok(deviceGroupList);
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @GetMapping("/api/device/listAll")
    public Result<List<AgentDeviceGroup>> queryActiveAgent() {
        deviceAgentManagementService.requestAllAgentDeviceListUpdate();
        List<AgentDeviceGroup> deviceGroupList = deviceAgentManagementService.getAgentDeviceGroups();
        deviceGroupList = deviceGroupList.stream()
                .sorted((a, b) -> b.getDevices().size() - a.getDevices().size())
                .collect(Collectors.toList());
        return Result.ok(deviceGroupList);
    }

    /**
     * Authenticated USER: all
     * Data access:
     * 1) For users with ROLE SUPER_ADMIN/ADMIN, return all devices/groups/appium agents that can run test currently.
     * 2) For the rest users, return non-private or user TEAMs' devices/groups/APPIUM-support agents.
     */
    @GetMapping("/api/device/runnable")
    public Result<JSONObject> getGroupAndDevice(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
        }

        deviceAgentManagementService.requestAllAgentDeviceListUpdate();
        JSONObject res = new JSONObject();
        List<DeviceGroup> groupList = deviceAgentManagementService.getAllGroup();
        List<AgentDeviceGroup> agentDeviceList = deviceAgentManagementService.getAllAppiumAgents();
        List<DeviceInfo> deviceList = deviceAgentManagementService.getAllDevice();

        if (!sysUserService.checkUserAdmin(requestor)) {
            groupList = groupList.stream()
                    .filter(group -> userTeamManagementService.checkRequestorTeamRelation(requestor, group.getTeamId())
                            || !group.getIsPrivate())
                    .collect(Collectors.toList());
            agentDeviceList = agentDeviceList.stream()
                    .filter(agentDeviceGroup -> userTeamManagementService.checkRequestorTeamRelation(requestor, agentDeviceGroup.getTeamId()))
                    .collect(Collectors.toList());
            deviceList = deviceList.stream()
                    .filter(device -> userTeamManagementService.checkRequestorTeamRelation(requestor, agentManageService.getAgent(device.getAgentId()).getTeamId())
                            || !device.getIsPrivate())
                    .collect(Collectors.toList());
        }

        res.put(Const.Param.GROUP, groupList);
        res.put(Const.Param.TEST_DEVICE_SN, deviceList);
        res.put(Const.Param.AGENT, agentDeviceList);
        return Result.ok(res);
    }

    /**
     * Authenticated USER: all
     */
    @GetMapping("/api/center/isAlive")
    public Result<JSONObject> isAlive() {
        JSONObject res = new JSONObject();
        res.put("status", "OK");
        return Result.ok(res);
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) creator of device connected agent,
     * 3) admin of the TEAM that group is in
     */
    @PostMapping(value = "/api/device/updateDeviceScope", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("IllegalCatch")
    public Result updateDeviceScope(@CurrentSecurityContext SysUser requestor,
                                    @RequestParam(value = "deviceSerial") String deviceSerial,
                                    @RequestParam(value = "isPrivate") Boolean isPrivate) {
        try {
            if (!deviceAgentManagementService.checkDeviceAuthorization(requestor, deviceSerial)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            }

            deviceAgentManagementService.updateDeviceScope(deviceSerial, isPrivate);
        } catch (IllegalArgumentException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e);
        } catch (RuntimeException e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }

        return Result.ok("Saved success!");
    }

    // API used to operate the device
    @PostMapping(value = "/api/device/operate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result operateDevice(@CurrentSecurityContext SysUser requestor,
                                @RequestBody DeviceOperation operation) {
        try {
            if (!deviceAgentManagementService.checkDeviceAuthorization(requestor, operation.getDeviceSerial())) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
            }
            if (operation.getOperationType() == null) {
                return Result.error(HttpStatus.BAD_REQUEST.value(), "Invalid operation type");
            }
            switch (operation.getOperationType()) {
                case TAP:
                    if(StringUtils.isEmpty(operation.getFromPositionX())|| StringUtils.isEmpty(operation.getFromPositionY())){
                        return Result.error(HttpStatus.BAD_REQUEST.value(), "Invalid tap position");
                    }
                    break;
                case SWIPE:
                    if (StringUtils.isEmpty(operation.getFromPositionX()) || StringUtils.isEmpty(operation.getFromPositionY())
                            || StringUtils.isEmpty(operation.getToPositionX()) || StringUtils.isEmpty(operation.getToPositionY())) {
                        return Result.error(HttpStatus.BAD_REQUEST.value(), "Invalid swipe position");
                    }
                    break;
                case REBOOT:
                    break;
                case WAKEUP:
                    break;
                default:
                    return Result.error(HttpStatus.BAD_REQUEST.value(), "Invalid operation type");
            }
            deviceAgentManagementService.operateDevice(operation);
        } catch (IllegalArgumentException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), e);
        } catch (Exception e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
        return Result.ok("Operate success!");
    }
}
