// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.common.file.AccessToken;
import lombok.Data;

/**
 * @author zhoule
 * @date 11/15/2022
 */
@Data
public class AgentMetadata {
    AccessToken accessToken;
    String storageType;
    AgentUser agentUser;
    String pushgatewayUsername;
    String pushgatewayPassword;
}
