// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * Created by bsp on 17/12/16.
 */
@Configuration
@OpenAPIDefinition(info =
@Info(title = "Hydra Lab API", version = "1.0.0", description = "Hydra Lab", contact =
@Contact(name = "Microsoft Hydra Lab Support", email = "hydra_lab_support@microsoft.com")
)
)
public class SwaggerConfig {
}
