// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.service.TestDataService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.AndroidTestUnit;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.repository.KeyValueRepository;
import com.microsoft.hydralab.common.util.Const;
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

@RestController
@RequestMapping
public class TestDetailController {
    private final Logger logger = LoggerFactory.getLogger(TestDetailController.class);
    @Resource
    KeyValueRepository keyValueRepository;
    @Resource
    TestDataService testDataService;
    @Resource
    private UserTeamManagementService userTeamManagementService;

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
            String videoRedirectUrl = testInfo.getVideoBlobUrl();

            //use CDN url to access video
            if (testInfo.getAttachments() != null && testInfo.getAttachments().size() > 0) {
                String cdnUrl = testInfo.getAttachments().get(0).getCDNUrl();
                if (cdnUrl != null && !"".equals(cdnUrl)) {
                    String originDomain = testInfo.getVideoBlobUrl().split("//")[1].split("/")[0];
                    videoRedirectUrl = videoRedirectUrl.replace(originDomain, cdnUrl);
                }
            }
            videos.add(videoRedirectUrl);
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

            //use CDN url to access video
            if (testInfo.getAttachments() != null && testInfo.getAttachments().size() > 0) {
                String cdnUrl = testInfo.getAttachments().get(0).getCDNUrl();
                if (cdnUrl != null && !"".equals(cdnUrl)) {
                    String originDomain = testInfo.getVideoBlobUrl().split("//")[1].split("/")[0];
                    String videoRedirectUrl = testInfo.getVideoBlobUrl().replace(originDomain, cdnUrl);
                    testInfo.setVideoBlobUrl(videoRedirectUrl);
                }
            }
            return Result.ok(testInfo);
        } catch (HydraLabRuntimeException e) {
            logger.error(e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }
}
