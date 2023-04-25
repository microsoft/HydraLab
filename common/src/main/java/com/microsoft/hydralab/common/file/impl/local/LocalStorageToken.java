// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.file.impl.local;

import com.microsoft.hydralab.common.file.AccessToken;
import lombok.Data;

/**
 * @author Li Shen
 * @date 3/6/2023
 */

@Data
public class LocalStorageToken implements AccessToken {
    @Deprecated
    private String signature;
    private String token;
    private String endpoint;

    @Override
    public String getToken() {
        return token;
    }

    @Deprecated
    @Override
    public void copySignature() {
        signature = token;
    }
}
