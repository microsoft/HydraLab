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

    public final static String[] METRICS_NAME = {"EnergyLoss", "CPUEnergyConsumption", "SocEnergyConsumption",
            "DisplayEnergyConsumption", "DiskEnergyConsumption", "NetworkEnergyConsumption", "MBBEnergyConsumption",
            "OtherEnergyConsumption", "EmiEnergyConsumption", "CPUEnergyConsumptionWorkOnBehalf",
            "CPUEnergyConsumptionAttributed", "TotalEnergyConsumption"};
    private final Set<String> AppIdSet = new ConcurrentHashSet<>();
    private final List<WindowsBatteryMetrics> windowsBatteryMetricsList = new ArrayList<>();
    private WindowsBatteryMetrics summarizedWindowsBatteryMetrics;

    @Data
    public static class WindowsBatteryMetrics {
        private long energyLoss;
        private long CPUEnergyConsumption;
        private long socEnergyConsumption;
        private long displayEnergyConsumption;
        private long diskEnergyConsumption;
        private long networkEnergyConsumption;
        private long MBBEnergyConsumption;
        private long otherEnergyConsumption;
        private long emiEnergyConsumption;
        private long CPUEnergyConsumptionWorkOnBehalf;
        private long CPUEnergyConsumptionAttributed;
        private long totalEnergyConsumption;
        @NonNull
        private String timeStamp = "";

        public void accumulate(WindowsBatteryMetrics metrics) {
            this.energyLoss += metrics.energyLoss;
            this.CPUEnergyConsumption += metrics.CPUEnergyConsumption;
            this.socEnergyConsumption += metrics.socEnergyConsumption;
            this.displayEnergyConsumption += metrics.displayEnergyConsumption;
            this.diskEnergyConsumption += metrics.diskEnergyConsumption;
            this.networkEnergyConsumption += metrics.networkEnergyConsumption;
            this.MBBEnergyConsumption += metrics.MBBEnergyConsumption;
            this.otherEnergyConsumption += metrics.otherEnergyConsumption;
            this.emiEnergyConsumption += metrics.emiEnergyConsumption;
            this.CPUEnergyConsumptionWorkOnBehalf += metrics.CPUEnergyConsumptionWorkOnBehalf;
            this.CPUEnergyConsumptionAttributed += metrics.CPUEnergyConsumptionAttributed;
            this.totalEnergyConsumption += metrics.totalEnergyConsumption;

            if (this.timeStamp.compareTo(metrics.timeStamp) < 0) {
                this.timeStamp = metrics.timeStamp;
            }
        }
    }
}
