// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.util;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhoule
 * @date 07/14/2023
 */

@Data
public class PageNode {
    int id;
    String pageName;
    List<ActionInfo> actionInfoList = new ArrayList<>();
    // key: action id, value: child page node
    Map<Integer, PageNode> childPageNodeMap = new HashMap<>();

    @Data
    public static class ElementInfo {
        int index;
        String className;
        String text;
        boolean clickable;
        String resourceId;
    }

    @Data
    public static class ActionInfo {
        int id;
        ElementInfo testElement;
        String actionType;
        String driverId;

        String description;
        Map<String, Object> arguments;
        boolean isOptional;
    }

    @Data
    public static class ExplorePath {
        String path;
        String actions;

        public ExplorePath(String path, String action) {
            this.path = path;
            this.actions = action;
        }
    }
}
