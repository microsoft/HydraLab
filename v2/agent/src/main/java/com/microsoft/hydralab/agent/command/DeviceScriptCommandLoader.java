// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.command;

import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.util.Const;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
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

    public void loadCommandAction(Task task) {
        if (task.getDeviceActions() == null) {
            task.setDeviceActions(new HashMap<>());
        }
        List<DeviceScriptCommand> filteredCommands = filterCommands(task.getTaskAlias());
        for (DeviceScriptCommand deviceCommand : filteredCommands) {
            List<DeviceAction> actions = command2Action(deviceCommand);
            List<DeviceAction> originActions =
                    task.getDeviceActions().getOrDefault(deviceCommand.getWhen(), new ArrayList<>());
            originActions.addAll(actions);
            task.getDeviceActions().put(deviceCommand.getWhen(), originActions);
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
        /**
         * Generate action by command type.
         * ADBShell: execute command on mobile device only
         * AgentShell: execute command on agent only
         *  - type = Agent: run for each Agent device, e.g. Windows, Mac, Linux. The commands would be run for only once.
         *      Used for environment setup and build generation.
         *  - type = Android: run for each Android device. The commands would be run on each Android device.
         *      Used for specific device operation through PC.
         */
        ADBShell() {
            @Override
            public DeviceAction getAction(String commandline, String deviceType) {
                String type = deviceType;
                if (StringUtils.isEmpty(type)) {
                    type = Const.OperatedDevice.ANDROID;
                }
                DeviceAction deviceAction = new DeviceAction(type, "execCommandOnDevice");
                deviceAction.getArgs().add(commandline);
                return deviceAction;
            }
        },
        AgentShell() {
            @Override
            public DeviceAction getAction(String commandline, String deviceType) {
                String type = deviceType;
                if (StringUtils.isEmpty(type)) {
                    type = Const.OperatedDevice.ANDROID;
                }
                DeviceAction deviceAction = new DeviceAction(type, "execCommandOnAgent");
                deviceAction.getArgs().add(commandline);
                return deviceAction;
            }
        };

        public abstract DeviceAction getAction(String commandline, String deviceType);
    }
}
