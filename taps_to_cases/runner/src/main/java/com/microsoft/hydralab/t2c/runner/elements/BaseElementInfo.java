// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.t2c.runner.elements;

import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class BaseElementInfo {
    protected final String xpath;

    public BaseElementInfo(String xpath) {
        this.xpath = xpath;
    }

    public String getElementInfo() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getXpath() {
        return xpath;
    }

}
