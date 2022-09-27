// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.android.client;

import java.io.IOException;

interface Encoder {
    void prepare() throws IOException;

    void stop();

    void release();

    void setCallback(Callback callback);

    interface Callback {
        void onError(Encoder encoder, Exception exception);
    }
}
