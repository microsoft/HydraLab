// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.file;

public interface AccessToken {
    String getToken();
    void copySignature();
}
