// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.management.DeviceManager;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    private Set<String> actionTypes = Set.of("setProperty", "setDefaultLauncher", "backToHome", "changeGlobalSetting", "changeSystemSetting");

    public void doActions(@NotNull DeviceManager deviceManager, @NotNull DeviceInfo deviceInfo, @NotNull Logger logger,
                          @NotNull Map<String, List<DeviceAction>> actions, @NotNull String when) {
        if (actions.get(when) == null || actions.get(when).isEmpty()) {
            return;
        }
        //filter todoActions
        List<DeviceAction> todoActions = actions.get(when).stream().filter(deviceAction -> actionTypes.contains(deviceAction.getMethod())).collect(Collectors.toList());

        logger.info("Start to execute actions! Current timing is {}, action size is {}", when, todoActions.size());
        todoActions.forEach(deviceAction -> doAction(deviceManager, deviceInfo, logger, deviceAction));
        logger.info("Execute actions finished!");
        ThreadUtils.safeSleep(3000);
    }

    public void doAction(@NotNull DeviceManager deviceManager, @NotNull DeviceInfo deviceInfo, @NotNull Logger logger,
                         @NotNull DeviceAction deviceAction) {
        try {
            if (!actionTypes.contains(deviceAction.getMethod())) {
                return;
            }
            logger.info("Start to analysis action type! Current action is {}", deviceAction.getMethod());
            Method method = Arrays.stream(deviceManager.getClass().getMethods())
                    .filter(tempMethod -> tempMethod.getName().equals(deviceAction.getMethod()))
                    .findFirst().orElse(
                            Arrays.stream(deviceManager.getClass().getDeclaredMethods())
                                    .filter(tempMethod -> tempMethod.getName().equals(deviceAction.getMethod()))
                                    .findFirst().orElse(null)
                    );
            if (method == null) {
                logger.error("Analysis action type error:Unsupported method");
                return;
            }

            logger.info("Start to analysis action args! Current action is {}", deviceAction.getMethod());
            List<String> actionArgs = deviceAction.getArgs();
            Object[] methodArgs = convertArgs(deviceInfo, logger, actionArgs, method.getParameterTypes());

            logger.info("Start to execute action! Current action is {}", deviceAction.getMethod());
            method.setAccessible(true);
            method.invoke(deviceManager, methodArgs);

        } catch (InvocationTargetException | IllegalAccessException e) {
            logger.error("Execute action: fail", e);
        } catch (HydraLabRuntimeException e) {
            logger.error("Convert action arg: fail", e);
        }
    }


    private Object[] convertArgs(@NotNull DeviceInfo deviceInfo, @NotNull Logger logger, @NotNull List<String> actionArgs, Class<?>[] parameterTypes) throws HydraLabRuntimeException {
        Object[] methodArgs = new Object[actionArgs.size() + 2];

        methodArgs[0] = deviceInfo;
        methodArgs[actionArgs.size() + 1] = logger;

        for (int i = 0; i < actionArgs.size(); i++) {
            logger.info("Start to convert action arg! Current arg is {}", actionArgs.get(i));
            try {
                methodArgs[i + 1] = parameterTypes[i + 1].cast(actionArgs.get(i));
            } catch (Exception e) {
                logger.info("Convert directly: failed. Try to convert by JSONObject", actionArgs.get(i));
                try {
                    methodArgs[i + 1] = JSONObject.parseObject(actionArgs.get(i), DeviceAction.class);
                } catch (Exception e1) {
                    throw new HydraLabRuntimeException(500, "Convert arg failed!", e1);
                }
            }
        }
        return methodArgs;
    }

}
