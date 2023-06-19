// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.command;

import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.TestTask;
import com.microsoft.hydralab.common.util.Const;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author zhoule
 * @date 01/31/2023
 */
@Service
public class DeviceScriptCommandLoader {
    // preset commands in agent application.yml
    @Resource(name = "DeviceCommandProperty")
    private List<DeviceScriptCommand> commands;

    public void loadCommandAction(TestTask testTask) {
        if (testTask.getDeviceActions() == null) {
            testTask.setDeviceActions(new HashMap<>());
        }
        List<DeviceScriptCommand> filteredCommands = filterCommands(testTask.getTestSuite());
        for (DeviceScriptCommand deviceCommand : filteredCommands) {
            List<DeviceAction> actions = command2Action(deviceCommand);
            List<DeviceAction> originActions =
                    testTask.getDeviceActions().getOrDefault(deviceCommand.getWhen(), new ArrayList<>());
            originActions.addAll(actions);
            testTask.getDeviceActions().put(deviceCommand.getWhen(), originActions);
        }
    }

    private List<DeviceScriptCommand> filterCommands(String suiteName) {
        List<DeviceScriptCommand> filteredCommands = new ArrayList<>();
        for (DeviceScriptCommand command : commands) {
            if (suiteName.matches(command.getSuiteClassMatcher())) {
                filteredCommands.add(command);
            }
        }
        return filteredCommands;
    }

    private List<DeviceAction> command2Action(DeviceScriptCommand deviceCommand) {
        List<DeviceAction> actionList = new ArrayList<>();
        ActionConverter converter = ActionConverter.valueOf(deviceCommand.getType());
        if (converter == null) {
            return actionList;
        }
        String[] commandLines = deviceCommand.getInline().split("\n");
        for (String commandLine : commandLines) {
            if (!StringUtils.isEmpty(commandLine)) {
                actionList.add(converter.getAction(commandLine, deviceCommand.getDevice()));
            }
        }
        return actionList;
    }

    private enum ActionConverter {
        //generate action by command type
        ADBShell() {
            @Override
            public DeviceAction getAction(String commandline, String deviceType) {
                if (StringUtils.isEmpty(deviceType)) {
                    deviceType = Const.OperatedDevice.ANDROID;
                }
                DeviceAction deviceAction = new DeviceAction(deviceType, "execCommandOnDevice");
                deviceAction.getArgs().add(commandline);
                return deviceAction;
            }
        },
        AgentShell() {
            @Override
            public DeviceAction getAction(String commandline, String deviceType) {
                if (StringUtils.isEmpty(deviceType)) {
                    deviceType = Const.OperatedDevice.ANY;
                }
                DeviceAction deviceAction = new DeviceAction(deviceType, "execCommandOnAgent");
                deviceAction.getArgs().add(commandline);
                return deviceAction;
            }
        };

        public abstract DeviceAction getAction(String commandline, String deviceType);
    }
}
