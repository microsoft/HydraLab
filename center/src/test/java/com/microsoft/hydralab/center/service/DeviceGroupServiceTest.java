package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.center.test.BaseTest;
import com.microsoft.hydralab.common.entity.center.DeviceGroup;
import com.microsoft.hydralab.common.entity.center.DeviceGroupRelation;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.util.Const;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.List;

class DeviceGroupServiceTest extends BaseTest {

    @Resource
    DeviceGroupService deviceGroupService;
    @Resource
    SysTeamService sysTeamService;

    String groupName = "GROUP";
    String deviceSerial = "DEVICE";

    @Test
    void testDeviceGroupManagement() {
        SysTeam defaultTeam = sysTeamService.getOrCreateDefaultTeam(Const.DefaultTeam.DEFAULT_TEAM_NAME);

        deviceGroupService.createGroup(defaultTeam.getTeamId(), defaultTeam.getTeamName(), groupName, null);
        DeviceGroup group = deviceGroupService.getGroupByName(Const.DeviceGroup.GROUP_NAME_PREFIX + groupName);
        Assertions.assertNotNull(group, "Insert group failed!");

        deviceGroupService.saveRelation(groupName, deviceSerial);
        DeviceGroupRelation relation = deviceGroupService.getRelation(groupName, deviceSerial);
        Assertions.assertNotNull(relation, "Insert relation failed!");

        List<DeviceGroupRelation> relationD = deviceGroupService.getGroupByDevice(deviceSerial);
        Assertions.assertNotNull(relationD, "Query relation by device failed!");
        Assertions.assertEquals(relationD.size(), 1, "Query relation by device failed!");

        List<DeviceGroupRelation> relationG = deviceGroupService.getDeviceByGroup(groupName);
        Assertions.assertNotNull(relationG, "Query relation by group failed!");
        Assertions.assertEquals(relationG.size(), 1, "Query relation by group failed!");

        deviceGroupService.deleteRelation(groupName, deviceSerial);
        relation = deviceGroupService.getRelation(groupName, deviceSerial);
        Assertions.assertNull(relation, "Delete relation failed!");
    }

}