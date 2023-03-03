// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.alibaba.fastjson.JSONObject;

import javax.persistence.AttributeConverter;

public class JsonConverter implements AttributeConverter<JSONObject, String> {
    @Override
    public String convertToDatabaseColumn(JSONObject attribute) {
        return attribute.toJSONString();
    }

    @Override
    public JSONObject convertToEntityAttribute(String dbData) {
        return JSONObject.parseObject(dbData);
    }
}
