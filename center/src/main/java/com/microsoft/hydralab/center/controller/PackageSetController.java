// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.center.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.microsoft.hydralab.center.service.SysTeamService;
import com.microsoft.hydralab.center.service.SysUserService;
import com.microsoft.hydralab.center.service.TestFileSetService;
import com.microsoft.hydralab.center.service.UserTeamManagementService;
import com.microsoft.hydralab.common.entity.agent.Result;
import com.microsoft.hydralab.common.entity.center.SysTeam;
import com.microsoft.hydralab.common.entity.center.SysUser;
import com.microsoft.hydralab.common.entity.common.*;
import com.microsoft.hydralab.common.entity.common.BlobFileInfo.ParserKey;
import com.microsoft.hydralab.common.util.*;
import com.microsoft.hydralab.common.util.PkgUtil.FILE_SUFFIX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.hydralab.center.util.CenterConstant.CENTER_FILE_BASE_DIR;

@RestController
public class PackageSetController {
    private final Logger logger = LoggerFactory.getLogger(PackageSetController.class);
    public SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    public int message_length = 200;
    @Resource
    AttachmentService attachmentService;
    @Resource
    TestFileSetService testFileSetService;
    @Resource
    private SysTeamService sysTeamService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private UserTeamManagementService userTeamManagementService;

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that fileSet is in
     */
    @PostMapping(value = {"/api/package/add"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result add(@CurrentSecurityContext SysUser requestor,
                      // todo: required = false when default TEAM is enabled
                      @RequestParam(value = "teamName") String teamName,
                      @RequestParam(value = "commitId", required = false) String commitId,
                      @RequestParam(value = "commitCount", defaultValue = "-1") String commitCount,
                      @RequestParam(value = "commitMessage", defaultValue = "") String commitMessage,
                      @RequestParam(value = "buildType", defaultValue = "debug") String buildType,
                      @RequestParam("appFile") MultipartFile appFile,
                      @RequestParam(value = "testAppFile", required = false) MultipartFile testAppFile) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
//        if (StringUtils.isEmpty(teamName)){
//            // todo: use user's default Team for package uploading
//        }
        SysTeam team = sysTeamService.queryTeamByName(teamName);
        if (team == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Team doesn't exist.");
        }
        if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, team.getTeamId())) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "User doesn't belong to this Team");
        }
        if (appFile.isEmpty()) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "apk file empty");
        }
        if (!LogUtils.isLegalStr(commitId, Const.RegexString.COMMON_STR, false)) {
            commitId = "commitId";
        }
        if (!LogUtils.isLegalStr(buildType, Const.RegexString.COMMON_STR, false)) {
            buildType = "debug";
        }
        int commitCountInt = Integer.parseInt(commitCount);
        commitMessage = commitMessage.replaceAll("[\\t\\n\\r]", " ");
        if (commitMessage.length() > message_length) {
            commitMessage = commitMessage.substring(0, message_length);
        }
        logger.info("commitId: {}, commitMessage: {}, buildType: {}, commitCount: {}", commitId, commitMessage, buildType, commitCountInt);// CodeQL [java/log-injection] False Positive: Has verified the string by regular expression

        try {
            String relativePath = FileUtil.getPathForToday();
            //Init test file set info
            TestFileSet testFileSet = new TestFileSet();
            testFileSet.setBuildType(buildType);
            testFileSet.setCommitId(commitId);
            testFileSet.setCommitMessage(commitMessage);
            testFileSet.setCommitCount(commitCount);
            testFileSet.setTeamId(team.getTeamId());
            testFileSet.setTeamName(team.getTeamName());

            //Save app file to server
            File tempAppFile = attachmentService.verifyAndSaveFile(appFile, CENTER_FILE_BASE_DIR + relativePath, false, null, new String[]{FILE_SUFFIX.APK_FILE, FILE_SUFFIX.IPA_FILE});
            BlobFileInfo appBlobFile = new BlobFileInfo(tempAppFile, relativePath, BlobFileInfo.FileType.APP_FILE);
            //Upload app file
            appBlobFile = attachmentService.addAttachment(testFileSet.getId(), EntityFileRelation.EntityType.APP_FILE_SET, appBlobFile, tempAppFile, logger);
            JSONObject appFileParser = appBlobFile.getFileParser();
            testFileSet.setAppName(appFileParser.getString(ParserKey.AppName));
            testFileSet.setPackageName(appFileParser.getString(ParserKey.PkgName));
            testFileSet.setVersion(appFileParser.getString(ParserKey.Version));
            testFileSet.getAttachments().add(appBlobFile);

            //Save test app file to server if exist
            if (testAppFile != null && !testAppFile.isEmpty()) {
                File tempTestAppFile = attachmentService.verifyAndSaveFile(testAppFile, CENTER_FILE_BASE_DIR + relativePath, false, null, new String[]{FILE_SUFFIX.APK_FILE, FILE_SUFFIX.JAR_FILE, FILE_SUFFIX.JSON_FILE});

                BlobFileInfo testAppBlobFile = new BlobFileInfo(tempTestAppFile, relativePath, BlobFileInfo.FileType.TEST_APP_FILE);
                //Upload app file
                testAppBlobFile = attachmentService.addAttachment(testFileSet.getId(), EntityFileRelation.EntityType.APP_FILE_SET, testAppBlobFile, tempTestAppFile, logger);
                testFileSet.getAttachments().add(testAppBlobFile);
            }

            //Save file set info to DB and memory
            testFileSetService.addTestFileSet(testFileSet);
            return Result.ok(testFileSet);
        } catch (HydraLabRuntimeException e) {
            return Result.error(e.getCode(), e);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that fileSet is in
     */
    @GetMapping("/api/package/{fileSetId}")
    public Result<TestFileSet> getFileSetInfo(@CurrentSecurityContext SysUser requestor,
                                              @PathVariable(value = "fileSetId") String fileSetId) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        TestFileSet testFileSet = testFileSetService.getFileSetInfo(fileSetId);
        if (testFileSet == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "FileSetId is error!");
        }
        if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, testFileSet.getTeamId())) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestFileSet doesn't belong to user's Teams");
        }

        return Result.ok(testFileSet);
    }

    /**
     * Authenticated USER: all
     * Data access:
     * 1) For users with ROLE SUPER_ADMIN/ADMIN, return all data.
     * 2) For the rest users, return the TestFileSet data that is in user's TEAMs
     */
    @PostMapping("/api/package/list")
    public Result<Page<TestFileSet>> list(@CurrentSecurityContext SysUser requestor,
                                          @RequestBody JSONObject data) {
        try {
            if (requestor == null) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
            }

            List<CriteriaType> criteriaTypes = new ArrayList<>();
            // filter all TestFileSets in TEAMs that user is in
            if (!sysUserService.checkUserAdmin(requestor)) {
                criteriaTypes = userTeamManagementService.formTeamIdCriteria(requestor.getTeamAdminMap());
                if (criteriaTypes.size() == 0) {
                    return Result.error(HttpStatus.UNAUTHORIZED.value(), "User belongs to no TEAM, please contact administrator for binding TEAM");
                }
            }

            int page = data.getIntValue("page");
            int pageSize = data.getIntValue("pageSize");
            if (pageSize <= 0) {
                pageSize = 30;
            }
            JSONArray queryParams = data.getJSONArray("queryParams");
            if (queryParams != null) {
                for (int i = 0; i < queryParams.size(); i++) {
                    CriteriaType temp = queryParams.getJSONObject(i).toJavaObject(CriteriaType.class);
                    criteriaTypes.add(temp);
                }
            }
            return Result.ok(testFileSetService.queryFileSets(page, pageSize, criteriaTypes));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping("/api/package/addAgentPackage")
    public Result uploadAgentPackage(@RequestParam("packageFile") MultipartFile packageFile) {
        if (packageFile.isEmpty()) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "package file empty");
        }

        String fileRelativePath = FileUtil.getPathForToday();
        String parentDir = CENTER_FILE_BASE_DIR + fileRelativePath;
        try {
            File savedPkg = attachmentService.verifyAndSaveFile(packageFile, parentDir, false, null, new String[]{FILE_SUFFIX.JAR_FILE});
            BlobFileInfo blobFileInfo = new BlobFileInfo(savedPkg, fileRelativePath, BlobFileInfo.FileType.AGENT_PACKAGE);
            return Result.ok(attachmentService.addFileInfo(blobFileInfo, savedPkg, EntityFileRelation.EntityType.AGENT_PACKAGE, logger));
        } catch (Exception e) {
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }

    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that TestJsonInfo is in
     */
    @PostMapping(value = {"/api/package/uploadJson"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result uploadTestJson(@CurrentSecurityContext SysUser requestor,
                                 // todo: required = false when default TEAM is enabled
                                 @RequestParam(value = "teamName") String teamName,
                                 @RequestParam(value = "packageName") String packageName,
                                 @RequestParam(value = "caseName") String caseName,
                                 @RequestParam(value = "testJsonFile") MultipartFile testJsonFile) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
//        if (StringUtils.isEmpty(teamId)){
//            // todo: use user's default Team for uploadTestJson
//        }
        if (!LogUtils.isLegalStr(packageName, Const.RegexString.PACKAGE_NAME, false)) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "The packagename is illegal");
        }
        SysTeam team = sysTeamService.queryTeamByName(teamName);
        if (team == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Team doesn't exist.");
        }
        if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, team.getTeamId())) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "User doesn't belong to this Team");
        }

        if (testJsonFile.isEmpty()) {
            return Result.error(HttpStatus.FORBIDDEN.value(), "test Json file empty");
        }

        String fileRelativePath = packageName + "/" + caseName;
        String parentDir = CENTER_FILE_BASE_DIR + fileRelativePath;
        try {
            TestJsonInfo testJsonInfo = new TestJsonInfo();
            testJsonInfo.setPackageName(packageName);
            testJsonInfo.setCaseName(caseName);
            testJsonInfo.setLatest(true);
            testJsonInfo.setTeamId(team.getTeamId());
            testJsonInfo.setTeamName(team.getTeamName());
            String newFileName = formatDate.format(testJsonInfo.getIngestTime()) + FILE_SUFFIX.JSON_FILE;
            File savedJson = attachmentService.verifyAndSaveFile(testJsonFile, parentDir, false, newFileName, new String[]{FILE_SUFFIX.JSON_FILE});
            String blobPath = fileRelativePath + "/" + savedJson.getName();
            testJsonInfo.setBlobPath(blobPath);

            return Result.ok(attachmentService.addTestJsonFile(testJsonInfo, savedJson, EntityFileRelation.EntityType.TEST_JSON, logger));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    /**
     * Authenticated USER: all
     * Data access:
     * 1) For users with ROLE SUPER_ADMIN/ADMIN, return all data.
     * 2) For the rest users, return data that the JSON info is in the user's TEAMs
     */
    @GetMapping("/api/package/testJsonList")
    public Result<List<TestJsonInfo>> testJsonList(@CurrentSecurityContext SysUser requestor) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }

        // filter all TestJsonInfos in TEAMs that user is in
        List<CriteriaType> criteriaTypes = new ArrayList<>();
        List<TestJsonInfo> testJsonInfoList;
        if (!sysUserService.checkUserAdmin(requestor)) {
            criteriaTypes = userTeamManagementService.formTeamIdCriteria(requestor.getTeamAdminMap());
            if (criteriaTypes.size() == 0) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "User belongs to no TEAM, please contact administrator for binding TEAM");
            }
        }

        testJsonInfoList = attachmentService.getLatestTestJsonList(criteriaTypes);
        return Result.ok(testJsonInfoList);
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that TestJsonInfo is in
     */
    @GetMapping("/api/package/testJsonHistory/{packageName}/{caseName}")
    public Result<List<TestJsonInfo>> testJsonHistory(@CurrentSecurityContext SysUser requestor, @PathVariable("packageName") String packageName, @PathVariable("caseName") String caseName) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        List<TestJsonInfo> testJsonInfoList = attachmentService.getTestJsonHistory(packageName, caseName);
        if (!CollectionUtils.isEmpty(testJsonInfoList)) {
            String jsonTeamId = testJsonInfoList.get(0).getTeamId();
            if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, jsonTeamId)) {
                return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestJsonInfos don't belong to user's Teams");
            }
        }

        return Result.ok(testJsonInfoList);
    }
    
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @PostMapping("/api/package/queryAgentPackage")
    public Result queryAgentPackage() {

        return Result.ok(attachmentService.queryBlobFileByType(BlobFileInfo.FileType.AGENT_PACKAGE));
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that TestFileSet is in
     */
    @PostMapping(value = {"/api/package/addAttachment"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result addAttachment(@CurrentSecurityContext SysUser requestor,
                                @RequestParam(value = "fileSetId") String fileSetId,
                                @RequestParam(value = "fileType") String fileType,
                                @RequestParam(value = "loadType", required = false) String loadType,
                                @RequestParam(value = "loadDir", required = false) String loadDir,
                                @RequestParam(value = "attachment") MultipartFile attachment) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        if (attachment.isEmpty()) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "attachment file empty");
        }
        TestFileSet testFileSet = testFileSetService.getFileSetInfo(fileSetId);
        if (testFileSet == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Error fileSetId");
        }
        if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, testFileSet.getTeamId())) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestFileSet doesn't belong to user's Teams");
        }

        String[] limitFileTypes = null;
        switch (fileType) {
            case BlobFileInfo.FileType.WINDOWS_APP:
                limitFileTypes = new String[]{FILE_SUFFIX.APPX_FILE};
                break;
            case BlobFileInfo.FileType.COMMON_FILE:
                Assert.notNull(loadType, "loadType is required");
                Assert.notNull(loadDir, "loadDir is required");
                Assert.isTrue(FileUtil.isLegalFolderPath(loadDir), "illegal loadDir");
                if (BlobFileInfo.LoadType.UNZIP.equals(loadType)) {
                    limitFileTypes = new String[]{FILE_SUFFIX.ZIP_FILE};
                }
                break;
            default:
                return Result.error(HttpStatus.BAD_REQUEST.value(), "Error fileType");
        }

        String newFileName = attachment.getOriginalFilename().replaceAll(" ", "");
        String fileRelativePath = FileUtil.getPathForToday();
        String parentDir = CENTER_FILE_BASE_DIR + fileRelativePath;
        try {
            File savedAttachment = attachmentService.verifyAndSaveFile(attachment, parentDir, false, newFileName, limitFileTypes);
            BlobFileInfo blobFileInfo = new BlobFileInfo(savedAttachment, fileRelativePath, fileType, loadType, loadDir);
            attachmentService.addAttachment(fileSetId, EntityFileRelation.EntityType.APP_FILE_SET, blobFileInfo, savedAttachment, logger);
            testFileSet.setAttachments(attachmentService.getAttachments(fileSetId, EntityFileRelation.EntityType.APP_FILE_SET));
            testFileSetService.saveFileSetToMem(testFileSet);
            return Result.ok(testFileSet);
        } catch (HydraLabRuntimeException e) {
            e.printStackTrace();
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    /**
     * Authenticated USER:
     * 1) users with ROLE SUPER_ADMIN/ADMIN,
     * 2) members of the TEAM that TestFileSet is in
     */
    @PostMapping(value = {"/api/package/removeAttachment"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result removeAttachment(@CurrentSecurityContext SysUser requestor,
                                   @RequestParam(value = "fileSetId") String fileSetId,
                                   @RequestParam(value = "fileId") String fileId) {
        if (requestor == null) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "unauthorized");
        }
        TestFileSet testFileSet = testFileSetService.getFileSetInfo(fileSetId);
        if (testFileSet == null) {
            return Result.error(HttpStatus.BAD_REQUEST.value(), "Error fileSetId");
        }
        if (!sysUserService.checkUserAdmin(requestor) && !userTeamManagementService.checkRequestorTeamRelation(requestor, testFileSet.getTeamId())) {
            return Result.error(HttpStatus.UNAUTHORIZED.value(), "Unauthorized, the TestFileSet doesn't belong to user's Teams");
        }

        attachmentService.removeAttachment(fileSetId, EntityFileRelation.EntityType.APP_FILE_SET, fileId);
        testFileSet.setAttachments(attachmentService.getAttachments(fileSetId, EntityFileRelation.EntityType.APP_FILE_SET));
        testFileSetService.saveFileSetToMem(testFileSet);
        return Result.ok(testFileSet);
    }
}
