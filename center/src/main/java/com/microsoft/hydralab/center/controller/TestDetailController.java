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
import com.microsoft.hydralab.common.entity.common.TestRun;
import com.microsoft.hydralab.common.entity.common.TestTask;
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
import com.microsoft.hydralab.t2c.runner.T2CJsonGenerator;
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

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
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

    @PostMapping(value = {"/api/test/performance/history"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<PerformanceTestResultEntity>> getPerformanceTestHistory(@CurrentSecurityContext SysUser requestor,
                                                                               @RequestBody List<CriteriaType> criteriaTypes) {
        try {
            if (requestor == null) {
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

    @GetMapping(value = {"/api/test/loadGraph/{fileId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result getSmartTestGraphXML(@CurrentSecurityContext SysUser requestor,
                                       @PathVariable(value = "fileId") String fileId,
                                       HttpServletResponse response) throws IOException {
        if (requestor == null) {
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
    public Result getSmartTestGraphPhoto(@CurrentSecurityContext SysUser requestor,
                                         @PathVariable(value = "fileId") String fileId,
                                         @RequestParam(value = "node") String node,
                                         HttpServletResponse response) throws IOException {
        if (requestor == null) {
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
    public Result saveGPTSuggestion(@CurrentSecurityContext SysUser requestor,
                                    @RequestParam(value = "testRunId", defaultValue = "") String testRunId,
                                    @RequestParam(value = "suggestion", defaultValue = "") String suggestion) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        if (StringUtils.isEmpty(testRunId) || StringUtils.isEmpty(suggestion)) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Error param! Should not be empty");
        }
        TestRun testRun = testDataService.findTestRunById(testRunId);
        if (testRun == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Error param! TestRun not exist");
        }
        try {
            testDataService.saveGPTSuggestion(testRun, suggestion);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
        return Result.ok("Save suggestion success!");
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

    @GetMapping(value = {"/api/test/generateT2C/{fileId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<String> generateT2CJsonFromSmartTest(@CurrentSecurityContext SysUser requestor,
                                                       @PathVariable(value = "fileId") String fileId,
                                                       @RequestParam(value = "testRunId") String testRunId,
                                                       @RequestParam(value = "path") String path) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }

        File graphZipFile = loadGraphFile(fileId);
        File graphFile = new File(graphZipFile.getParentFile().getAbsolutePath(), Const.SmartTestConfig.GRAPH_FILE_NAME);
        String t2cJson = null;

        TestRun testRun = testDataService.findTestRunById(testRunId);
        TestTask testTask = testDataService.getTestTaskDetail(testRun.getTestTaskId());
        try (FileInputStream in = new FileInputStream(graphFile)) {
            String graphXml = IOUtils.toString(in, StandardCharsets.UTF_8);
            t2cJson = T2CJsonGenerator.generateT2CJsonFromGraphXml(graphXml, path, logger, testTask.getPkgName(), "ANDROID");
        } catch (Exception e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error when parse graph xml");
        }

        return Result.ok(t2cJson);
    }

}
