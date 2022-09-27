// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;


import com.microsoft.hydralab.common.entity.common.BlobFileInfo;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.entity.common.EntityFileRelation;
import com.microsoft.hydralab.common.entity.common.TestJsonInfo;
import com.microsoft.hydralab.common.repository.BlobFileInfoRepository;
import com.microsoft.hydralab.common.repository.EntityFileRelationRepository;
import com.microsoft.hydralab.common.repository.TestJsonInfoRepository;
import com.microsoft.hydralab.common.util.blob.BlobStorageClient;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class AttachmentService {
    @Resource
    BlobFileInfoRepository blobFileInfoRepository;
    @Resource
    TestJsonInfoRepository testJsonInfoRepository;
    @Resource
    EntityFileRelationRepository entityFileRelationRepository;
    @Resource
    BlobStorageClient blobStorageClient;
    @Value("${app.blob.fileLimitDay}")
    int fileLimitDay;
    @Value("${app.blob.CDNUrl}")
    String cdnUrl;

    public BlobFileInfo addAttachment(String entityId, EntityFileRelation.EntityType entityType, BlobFileInfo blobFileInfo, File file, Logger logger) {
        boolean flag = false;
        List<BlobFileInfo> tempFileInfos = blobFileInfoRepository.queryBlobFileInfoByMd5(blobFileInfo.getMd5());
        for (BlobFileInfo tempFileInfo : tempFileInfos) {
            if (compareFileInfo(blobFileInfo, tempFileInfo)) {
                blobFileInfo = updateFileInfo(tempFileInfo, file, entityType, logger);
                flag = true;
                break;
            }
        }
        if (!flag) {
            blobFileInfo = addFileInfo(blobFileInfo, file, entityType, logger);
        }
        saveRelation(entityId, entityType, blobFileInfo);
        file.delete();
        return blobFileInfo;
    }

    public void removeAttachment(String entityId, EntityFileRelation.EntityType entityType, String fileId) {
        EntityFileRelation entityFileRelation = new EntityFileRelation(entityId, entityType.typeName, fileId);
        entityFileRelationRepository.delete(entityFileRelation);
    }

    public BlobFileInfo addFileInfo(BlobFileInfo blobFileInfo, File file, EntityFileRelation.EntityType entityType, Logger logger) {
        blobFileInfo.setFileId(UUID.randomUUID().toString());
        blobFileInfo.setFileParser(PkgUtil.analysisFile(file, entityType));
        blobFileInfo.setCreateTime(new Date());
        blobFileInfo.setUpdateTime(new Date());
        blobFileInfo.setBlobUrl(saveFileToBlob(file, entityType.blobConstant, blobFileInfo.getBlobPath(), logger));
        blobFileInfo.setCDNUrl(cdnUrl);
        blobFileInfoRepository.save(blobFileInfo);
        return blobFileInfo;
    }

    public TestJsonInfo addTestJsonFile(TestJsonInfo testJsonInfo, File file, EntityFileRelation.EntityType entityType, Logger logger) {
        testJsonInfo.setBlobUrl(saveFileToBlob(file, entityType.blobConstant, testJsonInfo.getBlobPath(), logger));

        List<TestJsonInfo> oldJsonInfoList = testJsonInfoRepository.findByIsLatestAndPackageNameAndCaseName(true, testJsonInfo.getPackageName(), testJsonInfo.getCaseName());
        if (oldJsonInfoList != null) {
            for (TestJsonInfo json : oldJsonInfoList) {
                json.setLatest(false);
                testJsonInfoRepository.save(json);
            }
        }

        testJsonInfoRepository.save(testJsonInfo);
        return testJsonInfo;
    }

    public List<TestJsonInfo> getLatestTestJsonList(List<CriteriaType> queryParams) {
        List<TestJsonInfo> testJsonInfoList;

        if (queryParams != null && queryParams.size() > 0) {
            Specification<TestJsonInfo> spec;
            CriteriaType isLatestCriteria = new CriteriaType();
            isLatestCriteria.setKey("isLatest");
            isLatestCriteria.setOp(CriteriaType.OpType.Equal);
            isLatestCriteria.setValue(Boolean.TRUE.toString());
            queryParams.add(isLatestCriteria);
            spec = new CriteriaTypeUtil<TestJsonInfo>().transferToSpecification(queryParams, true);

            testJsonInfoList = testJsonInfoRepository.findAll(spec);
        }
        else {
            testJsonInfoList = testJsonInfoRepository.findByIsLatest(true);
        }

        if (testJsonInfoList.size() == 0) {
            return null;
        }
        List<TestJsonInfo> testJsonInfoListCopy = new ArrayList<>(testJsonInfoList);
        testJsonInfoListCopy.sort(Comparator.comparing(o -> o.getPackageName() + o.getCaseName()));
        return testJsonInfoListCopy;
    }

    public List<TestJsonInfo> getTestJsonHistory(String packageName, String caseName) {
        List<TestJsonInfo> testJsonInfoList = testJsonInfoRepository.findByPackageNameAndCaseName(packageName, caseName);
        if (testJsonInfoList.size() == 0) {
            return null;
        }
        List<TestJsonInfo> testJsonInfoListCopy = new ArrayList<>(testJsonInfoList);
        testJsonInfoListCopy.sort(Comparator.comparing(TestJsonInfo::getIngestTime).reversed());
        return testJsonInfoListCopy;
    }

    public boolean compareFileInfo(BlobFileInfo newFileInfo, BlobFileInfo oldFileInfo) {
        if (newFileInfo.getFileName().equals(oldFileInfo.getFileName())
                && newFileInfo.getLoadType().equals(oldFileInfo.getLoadType())
                && newFileInfo.getLoadDir().equals(oldFileInfo.getLoadDir())) {
            return true;
        }
        return false;
    }

    public BlobFileInfo updateFileInfo(BlobFileInfo oldFileInfo, File file, EntityFileRelation.EntityType entityType, Logger logger) {
        int days = (int) ((new Date().getTime() - oldFileInfo.getUpdateTime().getTime()) / 1000 / 60 / 60 / 24);
        if (days >= fileLimitDay) {
            oldFileInfo.setBlobUrl(saveFileToBlob(file, entityType.blobConstant, oldFileInfo.getBlobPath(), logger));
            oldFileInfo.setCDNUrl(cdnUrl);
            oldFileInfo.setUpdateTime(new Date());
            blobFileInfoRepository.save(oldFileInfo);
        }
        return oldFileInfo;
    }


    public List<BlobFileInfo> getAttachments(String entityId, EntityFileRelation.EntityType entityType) {
        List<BlobFileInfo> result = new ArrayList<>();

        List<EntityFileRelation> fileRelations = entityFileRelationRepository.queryAllByEntityIdAndAndEntityType(entityId, entityType.typeName);
        for (EntityFileRelation fileRelation : fileRelations) {
            BlobFileInfo tempFileInfo = blobFileInfoRepository.findById(fileRelation.getFileId()).get();
            if (tempFileInfo != null) {
                result.add(tempFileInfo);
            }
        }
        return result;
    }

    public void saveRelation(String entityId, EntityFileRelation.EntityType entityType, BlobFileInfo blobFileInfo) {
        EntityFileRelation entityFileRelation = new EntityFileRelation();
        entityFileRelation.setEntityId(entityId);
        entityFileRelation.setEntityType(entityType.typeName);
        entityFileRelation.setFileId(blobFileInfo.getFileId());
        entityFileRelationRepository.save(entityFileRelation);
    }

    public void saveRelations(String entityId, EntityFileRelation.EntityType entityType, List<BlobFileInfo> attachments) {
        if (attachments == null) {
            return;
        }
        List<EntityFileRelation> relations = new ArrayList<>();
        for (BlobFileInfo attachment : attachments) {
            EntityFileRelation entityFileRelation = new EntityFileRelation();
            entityFileRelation.setFileId(attachment.getFileId());
            entityFileRelation.setEntityId(entityId);
            entityFileRelation.setEntityType(entityType.typeName);
            relations.add(entityFileRelation);
        }
        entityFileRelationRepository.saveAll(relations);
    }

    public void saveAttachments(String entityId, EntityFileRelation.EntityType entityType, List<BlobFileInfo> attachments) {
        if (attachments == null || attachments.size() == 0) {
            return;
        }
        blobFileInfoRepository.saveAll(attachments);
        saveRelations(entityId, entityType, attachments);
    }

    private String saveFileToBlob(File file, String containerName, String fileName, Logger logger) {
        String blobUrl = blobStorageClient.uploadBlobFromFile(file, containerName, fileName, logger);
        if (StringUtils.isBlank(blobUrl)) {
            logger.warn("blobUrl is empty for file {}", fileName);
        } else {
            logger.info("upload file {} success: {}", fileName, blobUrl);
        }
        return blobUrl;
    }

    public List<BlobFileInfo> queryBlobFileByType(String fileType) {
        return blobFileInfoRepository.queryBlobFileInfoByFileType(fileType);
    }

    public BlobFileInfo getLatestAgentPackage() {
        List<BlobFileInfo> files = blobFileInfoRepository.queryBlobFileInfoByFileTypeOrderByCreateTimeDesc(BlobFileInfo.FileType.AGENT_PACKAGE);
        if (files != null && files.size() > 0) {
            return files.get(0);
        }
        return null;
    }

    public List<BlobFileInfo> filterAttachments(@NotNull List<BlobFileInfo> attachments, @NotNull String fileType) {
        List<BlobFileInfo> data = new ArrayList<>();
        for (BlobFileInfo attachment : attachments) {
            if (fileType.equals(attachment.getFileType())) {
                data.add(attachment);
            }
        }
        return data;
    }

    public BlobFileInfo filterFirstAttachment(@NotNull List<BlobFileInfo> attachments, @NotNull String fileType) {
        for (BlobFileInfo attachment : attachments) {
            if (fileType.equals(attachment.getFileType())) {
                return attachment;
            }
        }
        return null;
    }

    public File verifyAndSaveFile(@NotNull MultipartFile originFile, String parentDir, boolean isBase64, String newFileName, String... fileTypes) throws HydraLabRuntimeException, IOException {
        File parentDirFile = new File(parentDir);// CodeQL [java/path-injection] False Positive: Has verified the string by regular expression
        if (!parentDirFile.exists() && !parentDirFile.mkdirs()) {
            throw new HydraLabRuntimeException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "mkdirs failed!");
        }
        String filename = originFile.getOriginalFilename().replaceAll(" ", "");
        String fileSuffix = null;
        boolean isMatch = false;
        if (filename == null) {
            throw new HydraLabRuntimeException(405, "error file type: " + filename);
        }
        if (fileTypes != null) {
            for (String fileType : fileTypes) {
                if (filename.endsWith(fileType)) {
                    fileSuffix = fileType;
                    isMatch = true;
                    break;
                }
            }
            if (!isMatch) {
                throw new HydraLabRuntimeException(405, "error file type: " + filename);
            }
        }

        if (StringUtils.isEmpty(newFileName)) {
            newFileName = filename.replace(fileSuffix, "") + "_" + System.currentTimeMillis() % 10000 + "_" + fileSuffix;
        }

        File file = new File(parentDir, newFileName);// CodeQL [java/path-injection] False Positive: Has verified the string by regular expression
        InputStream inputStream = originFile.getInputStream();
        if (isBase64) {
            inputStream = new Base64InputStream(originFile.getInputStream());
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            IOUtils.copy(inputStream, fileOutputStream);
            return file;
        }
    }

    public void updateTestJsonTeam(String teamId, String teamName) {
        List<TestJsonInfo> testJsonInfos = testJsonInfoRepository.findAllByTeamId(teamId);
        testJsonInfos.forEach(testJsonInfo -> testJsonInfo.setTeamName(teamName));

        testJsonInfoRepository.saveAll(testJsonInfos);
    }
}
