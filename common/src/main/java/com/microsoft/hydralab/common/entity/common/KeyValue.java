// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
public class KeyValue implements Serializable {
    @Id
    private String keyid;
    @Column(name = "value", columnDefinition = "text", nullable = true)
    private String value;

    public KeyValue() {
    }

    public String getKey() {
        return keyid;
    }

    public void setKey(String key) {
        this.keyid = key;
    }
    @Lob
    @Basic(fetch = FetchType.EAGER)
    @Column(name="value", columnDefinition="CLOB", nullable=true)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public KeyValue(String key, String value) {
        this.keyid = key;
        this.value = value;
    }
}
