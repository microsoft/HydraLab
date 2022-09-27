// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

public interface GlobalConstant {

    enum AgentLiveStatus {
        ONLINE("ONLINE"),
        OFFLINE("OFFLINE");

        AgentLiveStatus(String status){
            this.status = status;
        }

        public String status;

        public String getStatus(){
            return status;
        }

    }

    String PROMETHEUS_METRIC_DISK_USAGE_RATIO = "agent_disk_usage_ratio";
    String PROMETHEUS_METRIC_DEVICE_STATE_CHANGE_TIMES = "agent_device_state_change_times";
    String PROMETHEUS_METRIC_RUNNING_TEST_NUM = "agent_running_test_num";
    String PROMETHEUS_METRIC_WEBSOCKET_RECONNECT_RETRY_TIMES = "agent_ws_reconnect_retry_times";
    String PROMETHEUS_METRIC_WEBSOCKET_DISCONNECT_SIGNAL = "agent_ws_disconnect_signal";
    String PROMETHEUS_METRIC_TEST_DEVICE_UNSTABLE_SIGNAL = "agent_device_unstable_signal";
    String PROMETHEUS_METRIC_TEST_DEVICE_OFFLINE_SIGNAL = "agent_device_offline_signal";
    String PROMETHEUS_METRIC_TEST_DEVICE_RUNNING_TEST_SIGNAL= "agent_device_running_test_signal";
    String PROMETHEUS_METRIC_TEST_DEVICE_ALIVE_SIGNAL= "agent_device_alive_signal";
}
