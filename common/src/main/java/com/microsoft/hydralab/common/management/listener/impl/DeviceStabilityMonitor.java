// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.listener.impl;

import com.android.ddmlib.IDevice;
import com.microsoft.hydralab.common.entity.agent.DeviceStateChangeRecord;
import com.microsoft.hydralab.common.entity.common.DeviceInfo;
import com.microsoft.hydralab.common.entity.common.MobileDeviceState;
import com.microsoft.hydralab.common.management.AgentManagementService;
import com.microsoft.hydralab.common.management.device.TestDeviceManager;
import com.microsoft.hydralab.common.management.listener.DeviceStatusListener;
import com.microsoft.hydralab.common.util.GlobalConstant;
import com.microsoft.hydralab.common.util.ThreadPoolUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class DeviceStabilityMonitor implements DeviceStatusListener {
    private final Map<String, ConcurrentLinkedDeque<DeviceStateChangeRecord>> deviceStateChangesMap = new HashMap<>();
    // map from device SN to a boolean flagging if a device is UNSTABLE and waiting for converting back to normal
    private final Map<String, AtomicBoolean> deviceStateIsConvertingList = new HashMap<>();
    /**
     * Map from device SN to current active state sliding window.
     * The length of list being larger the preset threshold (default 12 as 4 changes for a loop) will be considered as UNSTABLE state.
     * When it's UNSTABLE, keep all current UNSTABLE changes, until time check for last change is before time setting.
     * Trigger a timer to count down for the rest time to reset the state from UNSTABLE to the last changed state, re-trigger a timer if any change happened during the counting down.
     * <p>
     * Some possible UNSTABLE situation:
     * 1. switching between connected(OFFLINE) and disconnected, without entering deviceChanged (OFFLINE - DISCONNECTED switching but no ONLINE).
     * 2. switching among connected(ONLINE)/connected(OFFLINE)/disconnected.
     * 3. switching between connected(OFFLINE)/connected(ONLINE)
     */

    private AgentManagementService agentManagementService;
    private int deviceStateChangeThreshold;
    private long deviceStateChangeWindowTime;
    private long deviceStateChangeRecoveryTime;
    private MeterRegistry meterRegistry;
    private Logger classLogger = LoggerFactory.getLogger(DeviceStabilityMonitor.class);

    // 2 triggering ways: new device state change, timer trigger
    public void stabilityCheck(DeviceInfo deviceInfo, IDevice.DeviceState adbState, String deviceBehaviour) {
        stabilityCheck(deviceInfo, TestDeviceManager.mobileDeviceStateMapping(adbState), deviceBehaviour);
    }

    public void stabilityCheck(DeviceInfo deviceInfo, MobileDeviceState state, String deviceBehaviour) {
        if (deviceInfo == null) {
            classLogger.error("[Stability] DeviceInfo is null, stability check failed.");
            return;
        }

        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        ConcurrentLinkedDeque<DeviceStateChangeRecord> deviceStateChangeList = deviceStateChangesMap.computeIfAbsent(deviceInfo.getSerialNum(), k -> {
            ConcurrentLinkedDeque<DeviceStateChangeRecord> stateChangeList = new ConcurrentLinkedDeque<>();
            addDeviceMetricRegistration(deviceInfo, stateChangeList);
            return stateChangeList;
        });


        /**
         * deviceBehaviour != null means not checking from an incoming behaviour (for now just UNSTABLE_RESET_TIMER)
         */
        if (StringUtils.isEmpty(deviceBehaviour)) {
            if (state == MobileDeviceState.OTHER) {
                classLogger.error("[Stability] Didn't find IDevice information, SN: {}.", deviceInfo.getSerialNum());
                return;
            }
            DeviceStateChangeRecord newRecord = new DeviceStateChangeRecord(deviceInfo.getSerialNum(), now, state, deviceBehaviour);
            deviceStateChangeList.add(newRecord);
        }

        DeviceStateChangeRecord lastStateChange = deviceStateChangeList.size() == 0 ? null : deviceStateChangeList.getLast();

        if (lastStateChange == null) {
            classLogger.error("[Stability] State change window is empty for device {}.", deviceInfo.getSerialNum());
            return;
        }

        cleanOutdatedDeviceStateChange(deviceStateChangeList, now, deviceInfo);

        int stateChangeSum = deviceStateChangeList.size();
        if (stateChangeSum > deviceStateChangeThreshold) {
            classLogger.warn("[Stability] Window time length: {} minutes, threshold of change number: {}. Device {} currently contains {} changes, which exceeds the threshold.", deviceStateChangeWindowTime, deviceStateChangeThreshold, deviceInfo.getSerialNum(), stateChangeSum);
            synchronized (deviceInfo) {
                deviceInfo.setStatus(DeviceInfo.UNSTABLE);
            }

            long secondsSinceLastChange = now.toEpochSecond(ZoneOffset.UTC) - lastStateChange.getTime().toEpochSecond(ZoneOffset.UTC);
            long sleepTime = deviceStateChangeRecoveryTime * 60 - secondsSinceLastChange + 1; // use 1 second offset to erase millsecond-level error when calculating on second level

            AtomicBoolean isWaitingForConverting = deviceStateIsConvertingList.computeIfAbsent(deviceInfo.getSerialNum(), k -> new AtomicBoolean(false));
            if (isWaitingForConverting.compareAndSet(false, true)) {
                classLogger.info("[Stability] Device {}: last state change happened {} seconds ago, will re-check after {} seconds", deviceInfo.getSerialNum(), secondsSinceLastChange, sleepTime);

                // start a timer to trigger when recovery time limit is reached, to recover state from UNSTABLE to last recorded normal state
                ThreadPoolUtil.TIMER_EXECUTOR.schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (isWaitingForConverting.compareAndSet(true, false)) {
                            classLogger.info("[Stability] Reaches recovery time to check again for device {}", deviceInfo.getSerialNum());
                            stabilityCheck(deviceInfo, MobileDeviceState.OTHER, "UNSTABLE_RESET_TIMER");
                        }
                    }
                }, sleepTime, TimeUnit.SECONDS);
            }
        } else {
            if (DeviceInfo.UNSTABLE.equals(deviceInfo.getStatus())) {
                classLogger.info("[Stability] Device {}: converted back to {} from UNSTABLE state, and clear state change windows.", deviceInfo.getSerialNum(), lastStateChange.getState().toString());
                synchronized (deviceInfo) {
                    deviceInfo.setStatus(lastStateChange.getState().toString());
                }
            } else {
                classLogger.warn("[Stability] Window time length: {} minutes, threshold of change number: {}. Device {} currently contains {} changes.", deviceStateChangeWindowTime, deviceStateChangeThreshold, deviceInfo.getSerialNum(), stateChangeSum);
            }
        }
    }

    private void cleanOutdatedDeviceStateChange(ConcurrentLinkedDeque<DeviceStateChangeRecord> recordWindow, LocalDateTime now, DeviceInfo deviceInfo) {
        /**
         * Clean outdated state change record in window lists
         * 1. Normal recording state change: keep window duration for data within {deviceStateChangeWindowTime} minutes;
         * 2. UNSTABLE state: keep all current content until (3.) happens;
         * 3. Timer thread to turn UNSTABLE back to normal state: clear all data;
         * In any situation, return the latest record in order to get DevuceState
         */
        if (recordWindow == null) {
            classLogger.error("[Stability] State change window of device {} is null", deviceInfo.getSerialNum());
            return;
        }

        if (recordWindow.isEmpty()) {
            return;
        }

        DeviceStateChangeRecord firstRecord = recordWindow.getFirst();
        DeviceStateChangeRecord lastRecord = recordWindow.getLast();

        if (DeviceInfo.UNSTABLE.equals(deviceInfo.getStatus())) {
            // When device is UNSTABLE, and new behavior comes in, keep current records and extend. Or clear all data
            if (lastRecord.getTime().plus(deviceStateChangeRecoveryTime, ChronoUnit.MINUTES).isBefore(now)) {
                // UNSTABLE back to normal state
                synchronized (recordWindow) {
                    recordWindow.clear();
                }
            }
        } else {
            // keep window duration for data within {deviceStateChangeWindowTime} minutes;
            synchronized (recordWindow) {
                while (firstRecord.getTime().plus(deviceStateChangeWindowTime, ChronoUnit.MINUTES).isBefore(now)) {
                    recordWindow.pollFirst();

                    if (recordWindow.isEmpty()) {
                        break;
                    } else {
                        firstRecord = recordWindow.getFirst();
                    }
                }
            }
        }
    }

    private void addDeviceMetricRegistration(DeviceInfo deviceInfo, ConcurrentLinkedDeque<DeviceStateChangeRecord> deviceStateChangeList) {
        // Metric: device state change times
        meterRegistry.gaugeCollectionSize(GlobalConstant.PROMETHEUS_METRIC_DEVICE_STATE_CHANGE_TIMES, Tags.empty().and("device SN", deviceInfo.getSerialNum()), deviceStateChangeList);
        classLogger.info("Metric of agent device state change times for device {} has been registered.", deviceInfo.getSerialNum());
        // Metric: device UNSTABLE state signal
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_TEST_DEVICE_UNSTABLE_SIGNAL,
                Tags.empty().and("device SN", deviceInfo.getSerialNum()),
                deviceInfo,
                this::getDeviceUnstableSignal);
        // Metric: device OFFLINE state signal
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_TEST_DEVICE_OFFLINE_SIGNAL,
                Tags.empty().and("device SN", deviceInfo.getSerialNum()),
                deviceInfo,
                this::getDeviceOfflineSignal);
        classLogger.info("Metric of agent device UNSTABLE state signal for device {} has been registered.", deviceInfo.getSerialNum());
        // Metric: device running test signal
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_TEST_DEVICE_RUNNING_TEST_SIGNAL,
                Tags.empty().and("device SN", deviceInfo.getSerialNum()),
                deviceInfo,
                this::getDeviceRunningTestSignal);
        classLogger.info("Metric of agent device running test signal for device {} has been registered.", deviceInfo.getSerialNum());
        // Metric: device alive signal
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_TEST_DEVICE_ALIVE_SIGNAL,
                Tags.empty().and("device SN", deviceInfo.getSerialNum()),
                deviceInfo,
                this::getDeviceAliveSignal);
        classLogger.info("Metric of agent device alive signal for device {} has been registered.", deviceInfo.getSerialNum());
        // Metric: device adb command timeout signal
        meterRegistry.gauge(GlobalConstant.PROMETHEUS_METRIC_TEST_DEVICE_ADB_TIMEOUT_SIGNAL,
                Tags.empty().and("device SN", deviceInfo.getSerialNum()),
                deviceInfo,
                this::getDeviceADBTimeoutSignal);
        classLogger.info("Metric of agent device ADB command timeout signal for device {} has been registered.", deviceInfo.getSerialNum());
    }

    private int getDeviceOfflineSignal(DeviceInfo deviceInfo) {
        return deviceInfo.isOffline() ? 1 : 0;
    }

    private int getDeviceUnstableSignal(DeviceInfo deviceInfo) {
        return deviceInfo.isUnstable() ? 1 : 0;
    }

    private int getDeviceRunningTestSignal(DeviceInfo deviceInfo) {
        return deviceInfo.isTesting() ? 1 : 0;
    }

    private int getDeviceAliveSignal(DeviceInfo deviceInfo) {
        return deviceInfo.isAlive() ? 1 : 0;
    }

    private int getDeviceADBTimeoutSignal(DeviceInfo deviceInfo) {
        return deviceInfo.isAdbTimeout() ? 1 : 0;
    }

    // todo: dynamically set cron interval to sync with prometheus scrape time
    @Scheduled(cron = "*/10 * * * * *")
    public void refreshDeviceStateChangeTimes() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        Set<DeviceInfo> deviceInfos = agentManagementService.getDeviceList(null);

        for (DeviceInfo info : deviceInfos) {
            cleanOutdatedDeviceStateChange(deviceStateChangesMap.get(info.getSerialNum()), now, info);
        }
    }

    @Override
    public void onDeviceInactive(DeviceInfo deviceInfo) {
        stabilityCheck(deviceInfo, MobileDeviceState.OFFLINE, null);
    }

    @Override
    public void onDeviceConnected(DeviceInfo deviceInfo) {
        stabilityCheck(deviceInfo, MobileDeviceState.ONLINE, null);
    }
}
