// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
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
public class AnalysisTask extends Task implements Serializable {

    @Convert(converter = AnalysisConfig.Converter.class)
    @Column(columnDefinition = "text")
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

        public static class Converter implements AttributeConverter<List<AnalysisConfig>, String> {
            @Override
            public String convertToDatabaseColumn(List<AnalysisConfig> attribute) {
                return JSONObject.toJSONString(attribute);
            }

            @Override
            public List<AnalysisConfig> convertToEntityAttribute(String dbData) {
                return JSONObject.parseArray(dbData, AnalysisConfig.class);
            }
        }
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
