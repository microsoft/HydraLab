package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

@Data
public class CriteriaType {
    private String key;
    private String value;
    private String op;
    private String likeRule;
    private String dateFormatString;


    public interface OpType {
        String Equal = "equal";
        String GreaterThan = "gt";
        String LessThan = "lt";
        String Like = "like";
        String In = "in";
    }

    public interface LikeRuleType {
        String Front = "front";
        String End = "end";
        String All = "all";
    }

}
