// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.management.listener;

import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author zhoule
 * @date 01/17/2023
 */

@Service
public class DeviceStatusListenerManager implements DeviceStatusListener {
    private List<DeviceStatusListener> listeners = new ArrayList<>();

    private <T extends DeviceStatusListener> void notifyEach(List<T> recorders, Consumer<T> consumer) {
        recorders.forEach(recorder -> {
            consumer.accept(recorder);
        });
    }

    public void registerListener(@NotNull DeviceStatusListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onDeviceInactive(DeviceInfo deviceInfo) {
        notifyEach(listeners, recorder -> recorder.onDeviceInactive(deviceInfo));
    }

    @Override
    public void onDeviceConnected(DeviceInfo deviceInfo) {
        notifyEach(listeners, recorder -> recorder.onDeviceConnected(deviceInfo));
    }
}
