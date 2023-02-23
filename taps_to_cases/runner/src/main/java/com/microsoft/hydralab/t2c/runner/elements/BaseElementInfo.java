// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner.elements;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseElementInfo {
    protected final String accessibilityId;
    protected final String xpath;
    protected final String text;

    public BaseElementInfo(String accessibilityId, String xpath, String text) {
        this.accessibilityId = accessibilityId;
        this.xpath = xpath;
        this.text = text;
    }

    public abstract Map<String, String> getBasisSearchedBy();



    public String getElementInfo(){
        return ToStringBuilder.reflectionToString(this);
    }
}
