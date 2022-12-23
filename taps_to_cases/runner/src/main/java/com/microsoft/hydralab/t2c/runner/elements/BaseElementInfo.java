// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.t2c.runner.elements;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

public class BaseElementInfo {
    private String accessibilityId;
    private String xpath;
    private String text;

    public BaseElementInfo(String accessibilityId, String xpath, String text) {
        this.accessibilityId = accessibilityId;
        this.xpath = xpath;
        this.text = text;
    }

    public Map<String, String> getBasisSearchedBy() {
        Map<String, String> keyToVal = new HashMap<>();
        keyToVal.put("accessibilityId", accessibilityId);
        keyToVal.put("xpath", xpath);
        keyToVal.put("text", text);
        return keyToVal;
    }



    public String getElementInfo(){
        return ToStringBuilder.reflectionToString(this);
    }
}
