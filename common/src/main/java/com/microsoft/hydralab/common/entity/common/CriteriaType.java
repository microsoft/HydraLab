// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

@Data
public class CriteriaType {
    private String key;
    private String value;
    private String op;
    private String likeRule;
    private String dateFormatString;

    public static final class OpType {
        @SuppressWarnings("ConstantName")
        public static final String Equal = "equal";
        @SuppressWarnings("ConstantName")
        public static final String NotEqual = "ne";
        @SuppressWarnings("ConstantName")
        public static final String GreaterThan = "gt";
        @SuppressWarnings("ConstantName")
        public static final String LessThan = "lt";
        @SuppressWarnings("ConstantName")
        public static final String Like = "like";
        @SuppressWarnings("ConstantName")
        public static final String In = "in";
    }

    public static final class LikeRuleType {
        @SuppressWarnings("ConstantName")
        public static final String Front = "front";
        @SuppressWarnings("ConstantName")
        public static final String End = "end";
        @SuppressWarnings("ConstantName")
        public static final String All = "all";
    }

}
