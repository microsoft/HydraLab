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
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.entity.common.PerformanceTestResultEntity;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.Task;
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.file.AccessToken;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.repository.KeyValueRepository;
import com.microsoft.hydralab.common.repository.StorageFileInfoRepository;
import com.microsoft.hydralab.common.util.AttachmentService;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.DownloadUtils;
import com.microsoft.hydralab.common.util.FileUtil;
import com.microsoft.hydralab.common.util.HydraLabRuntimeException;
import com.microsoft.hydralab.common.util.LogUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.microsoft.hydralab.center.util.CenterConstant.CENTER_TEMP_FILE_DIR;

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
    @Resource
    AttachmentService attachmentService;
    @Resource
    StorageServiceClientProxy storageServiceClientProxy;

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that test case is in
     */
    @GetMapping("/api/test/case/{id}")
    public Result getTestUnitDetail(@CurrentSecurityContext(expression = "authentication") SysUser requester, @PathVariable("id") String testCaseId) {
        try {
            if (requester == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }
            AndroidTestUnit androidTestUnit = keyValueRepository.getAndroidTestUnit(testCaseId);
            testDataService.checkTestDataAuthorization(requester, androidTestUnit.getTestTaskId());

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
    public Result getCrashStack(@CurrentSecurityContext(expression = "authentication") SysUser requester,
                                @PathVariable("id") String crashId) {
        try {
            if (requester == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            TestRun testInfo = testDataService.getTestRunByCrashId(crashId);
            testDataService.checkTestDataAuthorization(requester, testInfo.getTestTaskId());

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
    public Result videoFolder(@CurrentSecurityContext(expression = "authentication") SysUser requester,
                              @PathVariable("id") String resultId) {
        try {
            if (requester == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }
            if (!LogUtils.isLegalStr(resultId, Const.RegexString.UUID, false)) {
                return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error param! Should be UUID");
            } else {
                logger.info("result id {}", resultId); // CodeQL [java/log-injection] False Positive: Has verified the string by regular expression
            }
            TestRun testInfo = testDataService.getTestRunWithVideoInfo(resultId);
            testDataService.checkTestDataAuthorization(requester, testInfo.getTestTaskId());

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
    public Result deviceTaskInfo(@CurrentSecurityContext(expression = "authentication") SysUser requester,
                                 @PathVariable("deviceTaskId") String deviceTaskId) {
        try {
            if (requester == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }
            if (LogUtils.isLegalStr(deviceTaskId, Const.RegexString.UUID, false)) {
                logger.info("result id {}", deviceTaskId); // CodeQL [java/log-injection] False Positive: Has verified the string by regular expression
            } else {
                return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error param! Should be UUID");
            }
            TestRun testInfo = testDataService.getTestRunWithVideoInfo(deviceTaskId);
            testDataService.checkTestDataAuthorization(requester, testInfo.getTestTaskId());

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
    public Result getPerformanceTestReport(@CurrentSecurityContext(expression = "authentication") SysUser requester,
                                           @PathVariable(value = "fileId") String fileId) {
        try {
            if (requester == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            AccessToken token = storageTokenManageService.generateReadToken(requester.getMailAddress());
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

    @PostMapping(value = {"/api/test/performance/history"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<PerformanceTestResultEntity>> getPerformanceTestHistory(@CurrentSecurityContext(expression = "authentication") SysUser requester,
                                                                               @RequestBody List<CriteriaType> criteriaTypes) {
        try {
            if (requester == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            for (CriteriaType criteriaType : criteriaTypes) {
                if (StringUtils.isEmpty(criteriaType.getValue())) {
                    return Result.error(HttpStatus.BAD_REQUEST.value(), "RequestParam should not be empty");
                }
            }
            List<PerformanceTestResultEntity> performanceHistory = testDataService.getPerformanceTestHistory(criteriaTypes);

            return Result.ok(performanceHistory);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    @GetMapping(value = {"/api/test/loadCanaryReport/{fileId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result getAPKScannerCanaryReport(@CurrentSecurityContext(expression = "authentication") SysUser requester,
                                            @PathVariable(value = "fileId") String fileId) {
        if (requester == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }

        try {
            StorageFileInfo canaryReportBlobFile = storageFileInfoRepository.findById(fileId).orElse(null);
            if (canaryReportBlobFile == null) {
                throw new HydraLabRuntimeException("apk canary report file not exist!");
            }
            File canaryReportFile = new File(CENTER_TEMP_FILE_DIR, canaryReportBlobFile.getBlobPath());

            if (!canaryReportFile.exists()) {
                storageServiceClientProxy.download(canaryReportFile, canaryReportBlobFile);
            }
            String json = FileUtil.getStringFromFilePath(canaryReportFile.getAbsolutePath());
            JSONArray objects = JSON.parseArray(json);
            return Result.ok(objects);
        } catch (HydraLabRuntimeException e) {
            logger.error(e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    @GetMapping(value = {"/api/test/loadGraph/{fileId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result getSmartTestGraphXML(@CurrentSecurityContext(expression = "authentication") SysUser requester,
                                       @PathVariable(value = "fileId") String fileId,
                                       HttpServletResponse response) throws IOException {
        if (requester == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }

        try {
            File graphZipFile = loadGraphFile(fileId);

            File graphFile = new File(graphZipFile.getParentFile().getAbsolutePath(), Const.SmartTestConfig.GRAPH_FILE_NAME);
            if (!graphFile.exists()) {
                throw new HydraLabRuntimeException("Graph xml file not found");
            }

            FileInputStream in = new FileInputStream(graphFile);
            ServletOutputStream out = response.getOutputStream();

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            int len;
            byte[] buffer = new byte[1024 * 10];
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (HydraLabRuntimeException e) {
            logger.error(e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        } finally {
            response.flushBuffer();
        }
        return Result.ok();
    }

    @GetMapping(value = {"/api/test/loadNodePhoto/{fileId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result getSmartTestGraphPhoto(@CurrentSecurityContext(expression = "authentication") SysUser requester,
                                         @PathVariable(value = "fileId") String fileId,
                                         @RequestParam(value = "node") String node,
                                         HttpServletResponse response) throws IOException {
        if (requester == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        if (!LogUtils.isLegalStr(node, Const.RegexString.INTEGER, false)) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error param! Should be Integer");
        }
        try {
            File graphZipFile = loadGraphFile(fileId);

            File nodeFile = new File(graphZipFile.getParentFile().getAbsolutePath(), node + "/" + node + "-0.jpg");
            if (!nodeFile.exists()) {
                throw new HydraLabRuntimeException("Graph photo file not found");
            }

            FileInputStream inputStream = new FileInputStream(nodeFile);
            ServletOutputStream out = response.getOutputStream();
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, inputStream.available());
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
            out.write(bytes);
            out.flush();
        } catch (HydraLabRuntimeException e) {
            logger.error(e.getMessage(), e);
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        } finally {
            response.flushBuffer();
        }

        return Result.ok();
    }

    @PostMapping(value = {"/api/test/suggestion/provide"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result saveGPTSuggestion(@CurrentSecurityContext(expression = "authentication") SysUser requester,
                                    @RequestParam(value = "id", defaultValue = "") String id,
                                    @RequestParam(value = "suggestion", defaultValue = "") String suggestion,
                                    @RequestParam(value = "type", defaultValue = "") String type) {
        if (requester == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(suggestion) || StringUtils.isEmpty(type)) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Error param! Should not be empty");
        }
        // try to convert type to enum
        TestDataService.SuggestionType suggestionType;
        try {
            suggestionType = TestDataService.SuggestionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Error param! Suggestion type not exist");
        }

        try {
            saveGPTSuggestion(id, suggestion, suggestionType);
        } catch (HydraLabRuntimeException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
        return Result.ok("Save suggestion success!");
    }

    private void saveGPTSuggestion(String id, String suggestion, TestDataService.SuggestionType suggestionType) {
        switch (suggestionType) {
            case TestRun:
                TestRun testRun = testDataService.findTestRunById(id);
                if (testRun == null) {
                    throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Error param! TestRun not exist");
                }
                try {
                    testDataService.saveTestRunGPTSuggestion(testRun, suggestion);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Save TestRun suggestion meet exception!", e);
                }
                break;
            case TestCase:
                AndroidTestUnit testCase = testDataService.findTestCaseById(id);
                if (testCase == null) {
                    throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Error param! TestCase not exist");
                }
                try {
                    testDataService.saveTestCaseGPTSuggestion(testCase, suggestion);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Save TestCase suggestion meet exception!", e);
                }
                break;
            default:
                throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "Error param! Suggestion type not exist");
        }
    }

    private File loadGraphFile(String fileId) {
        StorageFileInfo graphBlobFile = storageFileInfoRepository.findById(fileId).orElse(null);
        if (graphBlobFile == null) {
            throw new HydraLabRuntimeException("Graph zip file not exist!");
        }
        File graphZipFile = new File(CENTER_TEMP_FILE_DIR, graphBlobFile.getBlobPath());

        if (!graphZipFile.exists()) {
            storageServiceClientProxy.download(graphZipFile, graphBlobFile);
            FileUtil.unzipFile(graphZipFile.getAbsolutePath(), graphZipFile.getParentFile().getAbsolutePath());
        }
        return graphZipFile;
    }


}
