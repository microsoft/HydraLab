package com.microsoft.hydralab.performance.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.microsoft.hydralab.performance.IBaselineMetrics;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;

@Data
public class IOSEnergyGaugeInfo implements Serializable, IBaselineMetrics {
    private String appPackageName;
    private long timeStamp;
    private String description;
    /**
     * Sample Data:
     * {
     *  "energy.overhead":490,
     *  "kIDEGaugeSecondsSinceInitialQueryKey":90,
     *  "energy.version":1,
     *  "energy.gpu.cost":0,
     *  "energy.cpu.cost":13.612496010497445,
     *  "energy.networkning.overhead":500,  //Here is Typo in the key
     *  "energy.appstate.cost":8,
     *  "energy.location.overhead":0,
     *  "energy.thermalstate.cost":0,
     *  "energy.networking.cost":958.6126949224124,
     *  "energy.cost":962.2251909329099,
     *  "energy.cpu.overhead":0,
     *  "energy.location.cost":0,
     *  "energy.gpu.overhead":0,
     *  "energy.appstate.overhead":0,
     *  "energy.inducedthermalstate.cost":-1
     * }
     */
    @JSONField(name = "energy.cost")
    private float totalCost;
    @JSONField(name = "energy.cpu.cost")
    private float cpuCost;
    @JSONField(name = "energy.gpu.cost")
    private float gpuCost;
    @JSONField(name = "energy.networking.cost")
    private float networkingCost;
    @JSONField(name = "energy.appstate.cost")
    private float appStateCost;
    @JSONField(name = "energy.location.cost")
    private float locationCost;
    @JSONField(name = "energy.thermalstate.cost")
    private float thermalStateCost;

    @JSONField(name = "energy.overhead")
    private float totalOverhead;
    @JSONField(name = "energy.cpu.overhead")
    private float cpuOverhead;
    @JSONField(name = "energy.gpu.overhead")
    private float gpuOverhead;
    // Todo: Correct the name when typo from energy gauge is fixed
    @JSONField(name = "energy.networkning.overhead")
    private float networkingOverhead;
    @JSONField(name = "energy.appstate.overhead")
    private float appStateOverhead;
    @JSONField(name = "energy.location.overhead")
    private float locationOverhead;
    @JSONField(name = "energy.thermalstate.overhead")
    private float thermalStateOverhead;

    @Override
    @JSONField(serialize = false)
    public LinkedHashMap<String, Double> getBaselineMetricsKeyValue() {
        LinkedHashMap<String, Double> baselineMap = new LinkedHashMap<>();
        baselineMap.put("totalCost", (double) totalCost);
        baselineMap.put("cpuCost", (double) cpuCost);
        baselineMap.put("networkingCost", (double) networkingCost);
        baselineMap.put("appStateCost", (double) appStateCost);
        baselineMap.put("locationCost", (double) locationCost);
        return baselineMap;
    }

    @Override
    @JSONField(serialize = false)
    public SummaryType getSummaryType() {
        return SummaryType.AVERAGE;
    }
}
