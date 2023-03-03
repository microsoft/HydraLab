// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.performance.Entity;

import cn.hutool.core.collection.ConcurrentHashSet;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
public class WindowsBatteryParsedData {

    public static final String[] METRICS_NAME = {"EnergyLoss", "CPUEnergyConsumption", "SocEnergyConsumption",
            "DisplayEnergyConsumption", "DiskEnergyConsumption", "NetworkEnergyConsumption", "MBBEnergyConsumption",
            "OtherEnergyConsumption", "EmiEnergyConsumption", "CPUEnergyConsumptionWorkOnBehalf",
            "CPUEnergyConsumptionAttributed", "TotalEnergyConsumption"};

    @Data
    public static class WindowsBatteryMetrics {
        private long energyLoss;
        private long cpuEnergyConsumption;
        private long socEnergyConsumption;
        private long displayEnergyConsumption;
        private long diskEnergyConsumption;
        private long networkEnergyConsumption;
        private long mbbEnergyConsumption;
        private long otherEnergyConsumption;
        private long emiEnergyConsumption;
        private long cpuEnergyConsumptionWorkOnBehalf;
        private long cpuEnergyConsumptionAttributed;
        private long totalEnergyConsumption;
        @NonNull
        private String timeStamp = "";

        public void accumulate(WindowsBatteryMetrics metrics) {
            this.energyLoss += metrics.energyLoss;
            this.cpuEnergyConsumption += metrics.cpuEnergyConsumption;
            this.socEnergyConsumption += metrics.socEnergyConsumption;
            this.displayEnergyConsumption += metrics.displayEnergyConsumption;
            this.diskEnergyConsumption += metrics.diskEnergyConsumption;
            this.networkEnergyConsumption += metrics.networkEnergyConsumption;
            this.mbbEnergyConsumption += metrics.mbbEnergyConsumption;
            this.otherEnergyConsumption += metrics.otherEnergyConsumption;
            this.emiEnergyConsumption += metrics.emiEnergyConsumption;
            this.cpuEnergyConsumptionWorkOnBehalf += metrics.cpuEnergyConsumptionWorkOnBehalf;
            this.cpuEnergyConsumptionAttributed += metrics.cpuEnergyConsumptionAttributed;
            this.totalEnergyConsumption += metrics.totalEnergyConsumption;

            if (this.timeStamp.compareTo(metrics.timeStamp) < 0) {
                this.timeStamp = metrics.timeStamp;
            }
        }
    }

    private final Set<String> appIdSet = new ConcurrentHashSet<>();
    private final List<WindowsBatteryMetrics> windowsBatteryMetricsList = new ArrayList<>();
    private WindowsBatteryMetrics summarizedWindowsBatteryMetrics;
}
