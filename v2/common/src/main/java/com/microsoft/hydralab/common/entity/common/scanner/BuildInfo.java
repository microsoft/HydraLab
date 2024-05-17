package com.microsoft.hydralab.common.entity.common.scanner;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import jakarta.persistence.AttributeConverter;
import java.io.Serializable;

@Data
public class BuildInfo implements Serializable {
    private String commitId;
    private String buildFlavor;
    private String buildType;
    private int commitIndex;

    public static class Converter implements AttributeConverter<BuildInfo, String> {
        @Override
        public String convertToDatabaseColumn(BuildInfo attribute) {
            return JSONObject.toJSONString(attribute);
        }

        @Override
        public BuildInfo convertToEntityAttribute(String dbData) {
            return JSONObject.parseObject(dbData, BuildInfo.class);
        }
    }
}
