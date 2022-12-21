// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhoule
 * @date 12/20/2022
 */

public class ActionExecutor {
    /**
     * the implementation of supported actions should not be overload
     */
    private Set<String> actionTypes = Set.of("setProperty", "setDefaultLauncher", "backToHome");

    public void doActions(@NotNull DeviceManager deviceManager, @NotNull DeviceInfo deviceInfo, @NotNull Logger logger,
                          @NotNull List<DeviceAction> actions, @NotNull String timing) {
        if (actions.isEmpty()) {
            return;
        }
        //filter todoActions
        //sort: asc
        List<DeviceAction> todoActions = actions.stream().filter(deviceAction -> timing.equals(deviceAction.getTimingToAct()))
                .filter(deviceAction -> actionTypes.contains(deviceAction.getActionType()))
                .sorted(Comparator.comparingInt(DeviceAction::getActionOrder)).collect(Collectors.toList());

        logger.info("Start to execute actions! Current timing is {}, action size is {}", timing, todoActions.size());
        todoActions.forEach(deviceAction -> doAction(deviceManager, deviceInfo, logger, deviceAction));
        logger.info("Execute actions finished!");
        ThreadUtils.safeSleep(3000);
    }

    public void doAction(@NotNull DeviceManager deviceManager, @NotNull DeviceInfo deviceInfo, @NotNull Logger logger,
                         @NotNull DeviceAction deviceAction) {
        try {
            if (!actionTypes.contains(deviceAction.getActionType())) {
                return;
            }
            logger.info("Start to analysis action type! Current action is {}, order is {}", deviceAction.getActionType(), deviceAction.getActionOrder());
            Method method = Arrays.stream(deviceManager.getClass().getMethods())
                    .filter(tempMethod -> tempMethod.getName().equals(deviceAction.getActionType()))
                    .findFirst().get();
            logger.info("Analysis action type: success");

            logger.info("Start to analysis action args! Current action is {}, order is {}", deviceAction.getActionType(), deviceAction.getActionOrder());
            List<String> actionArgs = deviceAction.getActionArgs();
            Object[] methodArgs = convertArgs(deviceInfo, logger, actionArgs, method.getParameterTypes());
            logger.info("Analysis action args: success");

            logger.info("Start to execute action! Current action is {}, order is {}", deviceAction.getActionType(), deviceAction.getActionOrder());
            method.invoke(deviceManager, methodArgs);
            logger.info("Execute action: success");

        } catch (InvocationTargetException | IllegalAccessException e) {
            logger.error("Execute action: fail", e);
        } catch (ClassNotFoundException e) {
            logger.error("Convert action arg: fail", e);
        }
    }


    private Object[] convertArgs(@NotNull DeviceInfo deviceInfo, @NotNull Logger logger, @NotNull List<String> actionArgs, Class<?>[] parameterTypes) throws ClassNotFoundException {
        Object[] methodArgs = new Object[actionArgs.size() + 2];

        methodArgs[0] = deviceInfo;
        methodArgs[actionArgs.size() + 1] = logger;

        for (int i = 0; i < actionArgs.size(); i++) {
            logger.info("Start to convert action arg! Current arg is {}", actionArgs.get(i));
            try {
                methodArgs[i + 1] = parameterTypes[i + 1].cast(actionArgs.get(i));
            } catch (Exception e) {
                logger.info("Convert directly: failed. Try to convert by JSONObject", actionArgs.get(i));
                methodArgs[i + 1] = JSONObject.parseObject(actionArgs.get(i), DeviceAction.class);
            }
            logger.error("Convert action arg: success");
        }
        return methodArgs;
    }

}
