// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.alibaba.fastjson.JSONArray;

import javax.persistence.AttributeConverter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhoule
 * @date 12/20/2022
 */

public class ActionArgConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> args) {
        JSONArray arrayTemp = new JSONArray();
        args.forEach(actionArg -> arrayTemp.add(actionArg));
        return arrayTemp.toString();
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        List<String> args = new ArrayList<>();
        JSONArray arrayTemp = JSONArray.parseArray(dbData);
        for (int i = 0; i < arrayTemp.size(); i++) {
            args.add(arrayTemp.getString(i));
        }
        return args;
    }
}
