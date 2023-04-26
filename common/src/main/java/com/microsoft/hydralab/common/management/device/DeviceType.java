// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.management.device;

import java.util.Set;

public enum DeviceType {
    ANDROID {
        @Override
        public Set<String> getSupportedAppSuffix() {
            return Set.of("apk");
        }
    },
    WINDOWS {
        @Override
        public Set<String> getSupportedAppSuffix() {
            return Set.of("appx", "appxbundle");
        }
    },
    IOS {
        @Override
        public Set<String> getSupportedAppSuffix() {
            return Set.of("ipa", "app");
        }
    };

    public abstract Set<String> getSupportedAppSuffix();
}