// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.logger;

import com.android.ddmlib.MultiLineReceiver;

public abstract class MultiLineNoCancelReceiver extends MultiLineReceiver {
    @Override
    public boolean isCancelled() {
        return false;
    }
}
