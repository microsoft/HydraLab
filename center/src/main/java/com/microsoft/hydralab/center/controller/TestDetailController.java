// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.service.StorageTokenManageService;
import com.microsoft.hydralab.center.service.TestDataService;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.repository.KeyValueRepository;
import com.microsoft.hydralab.common.repository.StorageFileInfoRepository;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.DownloadUtils;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping
public class TestDetailController {
    private final Logger logger = LoggerFactory.getLogger(TestDetailController.class);
    @Resource
    KeyValueRepository keyValueRepository;
    @Resource
    TestDataService testDataService;
    @Resource
    StorageTokenManageService storageTokenManageService;
    @Resource
    StorageFileInfoRepository storageFileInfoRepository;

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that test case is in
     */
    @GetMapping("/api/test/case/{id}")
    public Result getTestUnitDetail(@CurrentSecurityContext SysUser requestor, @PathVariable("id") String testCaseId) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }
            AndroidTestUnit androidTestUnit = keyValueRepository.getAndroidTestUnit(testCaseId);
            testDataService.checkTestDataAuthorization(requestor, androidTestUnit.getTestTaskId());

            return Result.ok(androidTestUnit);
        } catch (HydraLabRuntimeException e) {
            logger.error(e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that crash data is in
     */
    @GetMapping("/api/test/crash/{id}")
    public Result getCrashStack(@CurrentSecurityContext SysUser requestor,
                                @PathVariable("id") String crashId) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            TestRun testInfo = testDataService.getTestRunByCrashId(crashId);
            testDataService.checkTestDataAuthorization(requestor, testInfo.getTestTaskId());

            return Result.ok(keyValueRepository.getCrashStack(crashId));
        } catch (HydraLabRuntimeException e) {
            logger.error(e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that video is in
     */
    @GetMapping("/api/test/videos/{id}")
    public Result videoFolder(@CurrentSecurityContext SysUser requestor,
                              @PathVariable("id") String resultId) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }
            if (!LogUtils.isLegalStr(resultId, Const.RegexString.UUID, false)) {
                return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error param! Should be UUID");
            } else {
                logger.info("result id {}", resultId); // CodeQL [java/log-injection] False Positive: Has verified the string by regular expression
            }
            TestRun testInfo = testDataService.getTestRunWithVideoInfo(resultId);
            testDataService.checkTestDataAuthorization(requestor, testInfo.getTestTaskId());

            JSONObject data = new JSONObject();
            JSONArray videos = new JSONArray();
            videos.add(testInfo.getVideoBlobUrl());
            data.put("videos", videos);
            data.put("videoInfo", testInfo.getVideoTimeTagArr());
            return Result.ok(data);
        } catch (HydraLabRuntimeException e) {
            logger.error(e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that TestRun is in
     */
    @GetMapping("/api/test/task/device/{deviceTaskId}")
    public Result deviceTaskInfo(@CurrentSecurityContext SysUser requestor,
                                 @PathVariable("deviceTaskId") String deviceTaskId) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }
            if (LogUtils.isLegalStr(deviceTaskId, Const.RegexString.UUID, false)) {
                logger.info("result id {}", deviceTaskId); // CodeQL [java/log-injection] False Positive: Has verified the string by regular expression
            } else {
                return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error param! Should be UUID");
            }
            TestRun testInfo = testDataService.getTestRunWithVideoInfo(deviceTaskId);
            testDataService.checkTestDataAuthorization(requestor, testInfo.getTestTaskId());

            return Result.ok(testInfo);
        } catch (HydraLabRuntimeException e) {
            logger.error(e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    @GetMapping(value = {"/api/test/performance/{fileId}"})
    public Result getPerformanceTestReport(@CurrentSecurityContext SysUser requestor,
                                           @PathVariable(value = "fileId") String fileId) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            AccessToken token = storageTokenManageService.generateReadToken(requestor.getMailAddress());
            StorageFileInfo tempFileInfo = storageFileInfoRepository.findById(fileId).get();
            String blobUrl = tempFileInfo.getBlobUrl();

            URL url = new URL(blobUrl + "?" + token.getToken());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            InputStream inputStream = conn.getInputStream();
            byte[] byteData = DownloadUtils.readInputStream(inputStream);

            String jsonStr = new String(byteData);
            JSONArray array = JSON.parseArray(jsonStr);
            return Result.ok(array);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }
}
