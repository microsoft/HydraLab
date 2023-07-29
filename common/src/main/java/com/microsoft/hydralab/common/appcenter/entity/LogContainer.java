/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.hydralab.common.appcenter.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * The LogContainer model. Copied from
 * <a href="https://github.com/microsoft/appcenter-sdk-android/blob/develop/sdk/appcenter/src/main/java/com/microsoft/appcenter/ingestion/models/LogContainer.java">...</a>
 */
@Data
public class LogContainer {

    /**
     * The list of logs.
     */
    private List<HandledErrorLog> logs = new ArrayList<>();

}