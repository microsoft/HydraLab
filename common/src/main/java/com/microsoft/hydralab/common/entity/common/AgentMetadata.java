// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.entity.common;

import com.microsoft.hydralab.common.entity.center.AgentUser;
import lombok.Data;

/**
 * @author zhoule
 * @date 11/15/2022
 */
@Data
public class AgentMetadata {
    SASData blobSAS;
    AgentUser agentUser;
    String pushgatewayUsername;
    String pushgatewayPassword;
}
