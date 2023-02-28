// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.monitor;


import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MetricPushGateway extends PushGateway {
    public final AtomicBoolean isBasicAuthSet = new AtomicBoolean(false);

    public MetricPushGateway(String serverBaseURLStr) {
        super(serverBaseURLStr);
    }

    public MetricPushGateway(URL serverBaseURL) {
        super(serverBaseURL);
    }

    public void pushAdd(CollectorRegistry registry, String job, Map<String, String> groupingKey) throws IOException {
        try {
            super.pushAdd(registry, job, groupingKey);
        } catch (IOException e) {
            // if already get basic auth info from center and still fail
            if (isBasicAuthSet.get()) {
                throw e;
            }
        }
    }
}
