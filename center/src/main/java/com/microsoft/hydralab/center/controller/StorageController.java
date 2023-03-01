// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.controller;

import com.microsoft.hydralab.center.service.StorageTokenManageService;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.SysUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author Li Shen
 * @date 2/20/2023
 */

@RestController
@RequestMapping
public class StorageController {
    @Resource
    private StorageTokenManageService storageTokenManageService;


    @GetMapping("/api/storage/getToken")
    public Result generateReadToken(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        return Result.ok(storageTokenManageService.generateReadToken(requestor.getMailAddress()).getToken());
    }
}
