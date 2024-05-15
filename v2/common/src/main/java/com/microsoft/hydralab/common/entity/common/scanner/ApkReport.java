package com.microsoft.hydralab.common.entity.common.scanner;

import com.microsoft.hydralab.common.entity.common.TaskResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper=true)
public class ApkReport extends TaskResult implements Serializable {
    private String packageName;
    private String buildFlavor;

    @Convert(converter = ApkSizeReport.Converter.class)
    @Column(columnDefinition = "text")
    private ApkSizeReport apkSizeReport = new ApkSizeReport();
    @Convert(converter = ApkManifest.Converter.class)
    @Column(columnDefinition = "text")
    private ApkManifest apkManifest = new ApkManifest();
    @Convert(converter = BuildInfo.Converter.class)
    @Column(columnDefinition = "text")
    private BuildInfo buildInfo = new BuildInfo();
    @Convert(converter = LeakInfo.Converter.class)
    @Column(columnDefinition = "text")
    private List<LeakInfo> leakInfoList = new ArrayList<>();

    public ApkReport(String name) {
        this();
        this.packageName = name;
    }

    public ApkReport() {
        super();
    }

    public void addLeakInfo(LeakInfo leakInfo) {
        leakInfoList.add(leakInfo);
    }
}
