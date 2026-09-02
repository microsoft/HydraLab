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
        storageToken = extractBearerToken(storageToken);
        if (storageToken == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Invalid visit with no auth code");
        }
        if (!storageTokenManageService.validateAccessToken(storageToken, Const.FilePermission.WRITE, fileUri)) {
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
        storageToken = extractBearerToken(storageToken);
        if (storageToken == null) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Invalid visit with no auth code");
        }
        boolean canRead = storageTokenManageService.validateAccessToken(
                storageToken, Const.FilePermission.READ, fileUri);
        boolean canWrite = storageTokenManageService.validateAccessToken(
                storageToken, Const.FilePermission.WRITE, fileUri);
        if (!canRead && !canWrite) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, error access token for storage actions.");
        }
        if (!LogUtils.isLegalStr(fileUri, Const.RegexString.STORAGE_FILE_REL_PATH, false)) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Invalid file path, file name should not include ';'!");
        }

        File file = LocalStorageIOUtil.resolveFilePath(fileUri).toFile();
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
        final String appendPath = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        String fileUri = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, appendPath);
        if (!LogUtils.isLegalStr(fileUri, Const.RegexString.STORAGE_FILE_REL_PATH, false)) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Invalid file path, file name should not include ';'!");
        }

        File file = LocalStorageIOUtil.resolveFilePath(fileUri).toFile();
        if (!storageTokenManageService.validateTokenVal(token, fileUri)) {
            throw new HydraLabRuntimeException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, error access token for storage actions.");
        }
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

    static String extractBearerToken(String authorizationHeader) {
        String prefix = "Bearer ";
        if (authorizationHeader == null || !authorizationHeader.startsWith(prefix)) {
            return null;
        }
        String token = authorizationHeader.substring(prefix.length());
        return token.trim().isEmpty() ? null : token;
    }

    @GetMapping("/api/storage/getFileDownloadToken")
    public Result generateReadToken(@CurrentSecurityContext SysUser requestor,
                                    @QueryParam("fileUri") String fileUri) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }

        if (fileUri.startsWith("/devices/screenshots/")) {
            String screenshotStoragePath = getScreenshotStoragePath(fileUri);
            return Result.ok(storageTokenManageService.generateReadTokenForFile(
                    requestor.getMailAddress(), screenshotStoragePath).getToken());
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

    static String getScreenshotStoragePath(String fileUri) {
        String screenshotPrefix = "/devices/screenshots/";
        if (fileUri == null || !fileUri.startsWith(screenshotPrefix)) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Invalid screenshot path");
        }
        Path screenshotRoot = Paths.get("images/devices/screenshots").normalize();
        Path screenshotPath = Paths.get("images").resolve(fileUri.substring(1)).normalize();
        if (screenshotPath.equals(screenshotRoot) || !screenshotPath.startsWith(screenshotRoot)) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Invalid screenshot path");
        }
        return screenshotPath.toString().replace(File.separatorChar, '/');
    }
}
