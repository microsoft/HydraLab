// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common.scanner;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import javax.persistence.AttributeConverter;
import java.io.Serializable;
import java.util.List;

/**
 * @author zhoule
 * @date 11/16/2023
 */

@Data
public class LeakInfo implements Serializable {
    private String keyword;
    private List<String> LeakWordList;

    public LeakInfo() {
    }
    public LeakInfo(String keyword, List<String> LeakWordList) {
        this.keyword = keyword;
        this.LeakWordList = LeakWordList;
    }

    public static class Converter implements AttributeConverter<List<LeakInfo>, String> {
        @Override
        public String convertToDatabaseColumn(List<LeakInfo> attribute) {
            return JSONObject.toJSONString(attribute);
        }

        @Override
        public List<LeakInfo> convertToEntityAttribute(String dbData) {
            return JSONObject.parseArray(dbData, LeakInfo.class);
        }
    }
}
