package com.microsoft.hydralab.common.entity.common.scanner;

import com.microsoft.hydralab.common.entity.common.TaskResult;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper=true)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class ApkReport extends TaskResult implements Serializable {
    private String packageName;
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private ApkSizeReport apkSizeReport = new ApkSizeReport();
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private ApkManifest apkManifest = new ApkManifest();
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private BuildInfo buildInfo = new BuildInfo();
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
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
