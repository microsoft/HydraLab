package com.microsoft.hydralab.common.entity.common.scanner;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import jakarta.persistence.AttributeConverter;
import java.io.Serializable;

@Data
public class ApkManifest implements Serializable {
    private String packageName;
    private String versionName;
    private int versionCode;
    private int targetSDKVersion;
    private int minSDKVersion;

    public static class Converter implements AttributeConverter<ApkManifest, String> {
        @Override
        public String convertToDatabaseColumn(ApkManifest attribute) {
            return JSONObject.toJSONString(attribute);
        }

        @Override
        public ApkManifest convertToEntityAttribute(String dbData) {
            return JSONObject.parseObject(dbData, ApkManifest.class);
        }
    }
}
