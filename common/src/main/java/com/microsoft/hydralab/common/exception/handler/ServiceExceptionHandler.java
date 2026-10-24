// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.exception.handler;

import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.exception.reporter.ExceptionReporterManager;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author zhoule
 * @date 08/02/2023
 */

@ControllerAdvice
public class ServiceExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(ServiceExceptionHandler.class);

    @ExceptionHandler({Exception.class, HydraLabRuntimeException.class})
    @ResponseBody
    public Result handleException(Exception e) {
        logger.info("Exception collected in ControllerAdvice with message {}", e.getMessage());
        e.printStackTrace();
        ExceptionReporterManager.reportException(e, true);
        return Result.error(e instanceof HydraLabRuntimeException ? ((HydraLabRuntimeException) e).getCode() : HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
    }
}
