// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

/**
 * @author zhoule
 * @date 12/20/2022
 */

@Data
public class ActionArg {
    private int argOrder;
    private String argValue;
    private String className;

    public ActionArg() {
    }

    public ActionArg(int argOrder, String argValue, String className) {
        this.argOrder = argOrder;
        this.argValue = argValue;
        this.className = className;
    }
}
