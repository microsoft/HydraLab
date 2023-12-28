// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhoule
 * @date 11/23/2023
 */

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class AnalysisTask extends Task implements Serializable {

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private List<AnalysisConfig> analysisConfigs;

    public AnalysisTask() {
        super();
    }

    public AnalysisTask(TestTaskSpec testTaskSpec) {
        super(testTaskSpec);
        this.analysisConfigs = testTaskSpec.analysisConfigs;
    }

    @Override
    public TestTaskSpec convertToTaskSpec() {
        TestTaskSpec testTaskSpec = super.convertToTaskSpec();
        testTaskSpec.analysisConfigs = analysisConfigs;
        return testTaskSpec;
    }

    @Data
    public static class AnalysisConfig implements Serializable {
        String analysisType;
        String executor;
        Map<String, String> analysisConfig = new HashMap<>();
    }

    public enum AnalysisType {
        LEAK_INFO,
        FILE_SIZE;

        final String[] properties;

        AnalysisType(String... properties) {
            this.properties = properties;
        }

        public String[] getProperties() {
            return properties;
        }
    }

}
