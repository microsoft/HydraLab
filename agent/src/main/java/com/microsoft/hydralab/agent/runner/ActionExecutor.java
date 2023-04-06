// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.agent.runner;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.common.entity.common.DeviceAction;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.TestRunDevice;
import com.microsoft.hydralab.common.management.device.DeviceDriver;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.ThreadUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhoule
 * @date 12/20/2022
 */

@Service
public class ActionExecutor {
    /**
     * the implementation of supported actions should not be overload
     */
    private Set<String> actionTypes =
            Set.of("setProperty", "setDefaultLauncher", "backToHome", "changeGlobalSetting",
                    "changeSystemSetting", "execCommandOnDevice", "execCommandOnAgent", "pushFileToDevice",
                    "pullFileFromDevice");

    public List<Exception> doActions(@NotNull DeviceDriver deviceDriverManager,
                                     @NotNull TestRunDevice testRunDevice,
                                     @NotNull Logger logger,
                                     @NotNull Map<String, List<DeviceAction>> actions, @NotNull String when) {
        List<Exception> exceptions = new ArrayList<>();
        //filter todoActions
        List<DeviceAction> todoActions = actions.getOrDefault(when, new ArrayList<>()).stream()
                .filter(deviceAction -> actionTypes.contains(deviceAction.getMethod()))
                .collect(Collectors.toList());

        logger.info("Start to execute actions! Current timing is {}, action size is {}", when, todoActions.size());
        for (DeviceAction deviceAction : todoActions) {
            try {
                doAction(deviceDriverManager, testRunDevice, logger, deviceAction);
            } catch (InvocationTargetException | IllegalAccessException | HydraLabRuntimeException e) {
                exceptions.add(e);
                logger.error("Execute {} action: fail", deviceAction.getMethod(), e);
            }
        }
        logger.info("Execute actions finished!");
        ThreadUtils.safeSleep(3000);
        return exceptions;
    }

    public void doAction(@NotNull DeviceDriver deviceDriverManager,
                         @NotNull TestRunDevice testRunDevice,
                         @NotNull Logger logger,
                         @NotNull DeviceAction deviceAction)
            throws InvocationTargetException, IllegalAccessException {
        if (!actionTypes.contains(deviceAction.getMethod())) {
            return;
        }
        if (!StringUtils.isEmpty(deviceAction.getDeviceType()) && !testRunDevice.getDeviceInfo().getType().equalsIgnoreCase(deviceAction.getDeviceType())){
            return;
        }
        DeviceInfo deviceInfo = testRunDevice.getDeviceInfo();
        logger.info("Start to analysis action type! Current action is {}", deviceAction.getMethod());
        Method method = Arrays.stream(deviceDriverManager.getClass().getMethods())
                .filter(tempMethod -> tempMethod.getName().equals(deviceAction.getMethod()))
                .findFirst().orElse(
                        Arrays.stream(deviceDriverManager.getClass().getDeclaredMethods())
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
        method.invoke(deviceDriverManager, methodArgs);
    }

    private Object[] convertArgs(@NotNull DeviceInfo deviceInfo, @NotNull Logger logger,
                                 @NotNull List<String> actionArgs, Class<?>[] parameterTypes)
            throws HydraLabRuntimeException {
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
                    throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Convert arg failed!", e1);
                }
            }
        }
        return methodArgs;
    }

}
