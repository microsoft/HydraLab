// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.exception.handler;

import com.microsoft.hydralab.common.exception.reporter.ExceptionReporterManager;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author zhoule
 * @date 08/02/2023
 */

@ControllerAdvice
public class ServiceExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(ServiceExceptionHandler.class);

    @ExceptionHandler({Exception.class, HydraLabRuntimeException.class})
    public void handleException(Exception e) {
        logger.warn("Exception collected in ControllerAdvice with message {}", e.getMessage());
        ExceptionReporterManager.reportException(e);
    }
}
