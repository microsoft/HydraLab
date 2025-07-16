// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.controller;

import com.azure.core.annotation.QueryParam;
import com.microsoft.hydralab.center.service.StorageTokenManageService;
import com.microsoft.hydralab.center.util.LocalStorageIOUtil;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.repository.StorageFileInfoRepository;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Li Shen
 * @date 2/20/2023
 */

@RestController
@RequestMapping
public class StorageController {
    private final Logger logger = LoggerFactory.getLogger(StorageController.class);

    @Resource
    private StorageTokenManageService storageTokenManageService;
    @Resource
    private StorageFileInfoRepository storageFileInfoRepository;

    @PostMapping(value = Const.LocalStorageURL.CENTER_LOCAL_STORAGE_UPLOAD, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result uploadFile(HttpServletRequest request,
                             @RequestParam("file") MultipartFile uploadedFile,
                             @RequestParam("fileUri") String fileUri) {
        String storageToken = request.getHeader("Authorization");
        if (storageToken != null) {
            storageToken = storageToken.replaceAll("Bearer ", "");
        } else {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Invalid visit with no auth code");
        }
        if (!storageTokenManageService.validateAccessToken(storageToken)) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, error access token for storage actions.");
        }
        if (!LogUtils.isLegalStr(fileUri, Const.RegexString.STORAGE_FILE_REL_PATH, false)) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Invalid file path, file name should not include ';'!");
        }

        try {
            InputStream inputStream = uploadedFile.getInputStream();
            LocalStorageIOUtil.copyUploadedStreamToFile(inputStream, fileUri);
        } catch (HydraLabRuntimeException e) {
            logger.error(e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }

        return Result.ok(fileUri);
    }


    // used by center/agent
    @PostMapping(Const.LocalStorageURL.CENTER_LOCAL_STORAGE_DOWNLOAD)
    public void postDownloadFile(HttpServletRequest request,
                                 HttpServletResponse response,
                                 @RequestParam("fileUri") String fileUri) {
        String storageToken = request.getHeader("Authorization");
        if (storageToken != null) {
            storageToken = storageToken.replaceAll("Bearer ", "");
        } else {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Invalid visit with no auth code");
        }
        if (!storageTokenManageService.validateAccessToken(storageToken)) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, error access token for storage actions.");
        }
        if (!LogUtils.isLegalStr(fileUri, Const.RegexString.STORAGE_FILE_REL_PATH, false)) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Invalid file path, file name should not include ';'!");
        }

        Path publicFolder = Paths.get(Const.LocalStorageURL.CENTER_LOCAL_STORAGE_ROOT).normalize().toAbsolutePath();
        Path filePath = publicFolder.resolve(fileUri).normalize().toAbsolutePath();
        if (!filePath.startsWith(publicFolder + File.separator)) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Invalid file path");
        }

        File file = new File(Const.LocalStorageURL.CENTER_LOCAL_STORAGE_ROOT + fileUri);
        if (!file.exists()) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), String.format("File %s not exist!", fileUri));
        }

        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength((int) file.length());
        response.setHeader("Content-Disposition", "attachment;filename=" + file.getName());

        int resLen;
        try {
            resLen = LocalStorageIOUtil.copyDownloadedStreamToResponse(file, response.getOutputStream());
        } catch (IOException e) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
        logger.info(String.format("Output file: %s , size: %d!", fileUri, resLen));
    }

    // for front end to download file
    @GetMapping("/api/storage/local/download/**")
    public void getDownloadFile(HttpServletRequest request,
                                HttpServletResponse response,
                                @QueryParam("token") String token) {
        if (token == null) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Invalid visit with no auth code");
        }
        if (!storageTokenManageService.validateTokenVal(token)) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, error access token for storage actions.");
        }
        final String appendPath = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        String fileUri = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, appendPath);
        if (!LogUtils.isLegalStr(fileUri, Const.RegexString.STORAGE_FILE_REL_PATH, false)) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Invalid file path, file name should not include ';'!");
        }

        File file = new File(Const.LocalStorageURL.CENTER_LOCAL_STORAGE_ROOT + fileUri);
        if (!file.exists()) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), String.format("File %s not exist!", fileUri));
        }

        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength((int) file.length());
        response.setHeader("Content-Disposition", "attachment;filename=" + file.getName());

        int resLen;
        try {
            resLen = LocalStorageIOUtil.copyDownloadedStreamToResponse(file, response.getOutputStream());
        } catch (IOException e) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
        logger.info(String.format("Output file: %s , size: %d!", fileUri, resLen));
    }

    @GetMapping("/api/storage/getFileDownloadToken")
    public Result generateReadToken(@CurrentSecurityContext SysUser requestor,
                                    @QueryParam("fileUri") String fileUri) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }

        if (fileUri.startsWith("/devices/screenshots/")) {
            return Result.ok(storageTokenManageService.generateReadTokenForFile(requestor.getMailAddress(), "images" + fileUri).getToken());
        }
        String blobPath = fileUri;
        if (blobPath.startsWith("/")) {
            blobPath = blobPath.substring(1);
        }
        List<StorageFileInfo> storageFiles = storageFileInfoRepository.queryStorageFileInfoByBlobPathOrderByUpdateTimeDesc(blobPath);
        if (storageFiles.size() == 0) {
            return Result.error(HttpStatus.NOT_FOUND.value(), "File not found");
        }
        StorageFileInfo storageFileInfo = storageFiles.get(0);
        if (!storageTokenManageService.checkFileAuthorization(requestor, storageFileInfo)) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "You are not authorized to access this file");
        }
        String fullBlobPath = storageFileInfo.getBlobContainer() + "/" + storageFileInfo.getBlobPath();
        String token = storageTokenManageService.generateReadTokenForFile(requestor.getMailAddress(), fullBlobPath).getToken();

        return Result.ok(token);
    }
}
