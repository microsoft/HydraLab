// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;


import com.microsoft.hydralab.common.entity.common.*;
import com.microsoft.hydralab.common.file.StorageServiceClient;
import com.microsoft.hydralab.common.repository.StorageFileInfoRepository;
import com.microsoft.hydralab.common.repository.EntityFileRelationRepository;
import com.microsoft.hydralab.common.repository.TestJsonInfoRepository;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
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
    StorageFileInfoRepository storageFileInfoRepository;
    @Resource
    TestJsonInfoRepository testJsonInfoRepository;
    @Resource
    EntityFileRelationRepository entityFileRelationRepository;
    @Resource
    StorageServiceClient storageServiceClient;

    public StorageFileInfo addAttachment(String entityId, EntityType entityType, StorageFileInfo fileInfo, File file, Logger logger) {
        boolean recordExists = false;

        fileInfo.setStorageContainer(entityType.storageContainer);
        List<StorageFileInfo> tempFileInfos = storageFileInfoRepository.queryStorageFileInfoByMd5(fileInfo.getMd5());
        for (StorageFileInfo tempFileInfo : tempFileInfos) {
            if (compareFileInfo(fileInfo, tempFileInfo)) {
                fileInfo = updateFileInStorageAndDB(tempFileInfo, file, entityType, logger);
                recordExists = true;
                break;
            }
        }
        if (!recordExists) {
            fileInfo = saveFileInStorageAndDB(fileInfo, file, entityType, logger);
        }
        saveRelation(entityId, entityType, fileInfo);
        file.delete();
        return fileInfo;
    }

    public void removeAttachment(String entityId, EntityType entityType, String fileId) {
        EntityFileRelation entityFileRelation = new EntityFileRelation(entityId, entityType.typeName, fileId);
        entityFileRelationRepository.delete(entityFileRelation);
    }

    public StorageFileInfo saveFileInStorageAndDB(StorageFileInfo fileInfo, File file, EntityType entityType, Logger logger) {
        fileInfo.setFileId(UUID.randomUUID().toString());
        fileInfo.setFileParser(PkgUtil.analysisFile(file, entityType));
        fileInfo.setCreateTime(new Date());
        fileInfo.setUpdateTime(new Date());
        fileInfo.setStorageContainer(entityType.storageContainer);
        fileInfo.setCDNUrl(storageServiceClient.getCdnUrl());
        fileInfo.setFileDownloadUrl(saveFileInStorage(file, fileInfo, logger));
        storageFileInfoRepository.save(fileInfo);
        return fileInfo;
    }

    public StorageFileInfo updateFileInStorageAndDB(StorageFileInfo oldFileInfo, File file, EntityType entityType, Logger logger) {
        int days = (int) ((new Date().getTime() - oldFileInfo.getUpdateTime().getTime()) / 1000 / 60 / 60 / 24);
        if (days >= storageServiceClient.getFileLimitDay()) {
            oldFileInfo.setUpdateTime(new Date());
            oldFileInfo.setStorageContainer(entityType.storageContainer);
            oldFileInfo.setCDNUrl(storageServiceClient.getCdnUrl());
            oldFileInfo.setFileDownloadUrl(saveFileInStorage(file, oldFileInfo, logger));
            storageFileInfoRepository.save(oldFileInfo);
        }
        return oldFileInfo;
    }

    public TestJsonInfo addTestJsonFile(TestJsonInfo testJsonInfo, File file, EntityType entityType, Logger logger) {
        StorageFileInfo fileInfo = new StorageFileInfo(file, testJsonInfo.getFileRelPath(), StorageFileInfo.FileType.T2C_JSON_FILE, entityType);
        testJsonInfo.setFileDownloadUrl(saveFileInStorage(file, fileInfo, logger));
        testJsonInfo.setStorageContainer(entityType.storageContainer);
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

    public boolean compareFileInfo(StorageFileInfo newFileInfo, StorageFileInfo oldFileInfo) {
        if (newFileInfo.getLoadType() == null && oldFileInfo.getLoadType() == null) {
            return true;
        } else if (newFileInfo.getFileName().equals(oldFileInfo.getFileName())
                && newFileInfo.getLoadType().equals(oldFileInfo.getLoadType())
                && newFileInfo.getLoadDir().equals(oldFileInfo.getLoadDir())
                && newFileInfo.getStorageContainer().equals(oldFileInfo.getStorageContainer())) {
            return true;
        }
        return false;
    }

    public List<StorageFileInfo> getAttachments(String entityId, EntityType entityType) {
        List<StorageFileInfo> result = new ArrayList<>();

        List<EntityFileRelation> fileRelations = entityFileRelationRepository.queryAllByEntityIdAndAndEntityType(entityId, entityType.typeName);
        for (EntityFileRelation fileRelation : fileRelations) {
            StorageFileInfo tempFileInfo = storageFileInfoRepository.findById(fileRelation.getFileId()).get();
            if (tempFileInfo != null) {
                result.add(tempFileInfo);
            }
        }
        return result;
    }

    public void saveRelation(String entityId, EntityType entityType, StorageFileInfo fileInfo) {
        EntityFileRelation entityFileRelation = new EntityFileRelation();
        entityFileRelation.setEntityId(entityId);
        entityFileRelation.setEntityType(entityType.typeName);
        entityFileRelation.setFileId(fileInfo.getFileId());
        entityFileRelationRepository.save(entityFileRelation);
    }

    public void saveRelations(String entityId, EntityType entityType, List<StorageFileInfo> attachments) {
        if (attachments == null) {
            return;
        }
        List<EntityFileRelation> relations = new ArrayList<>();
        for (StorageFileInfo attachment : attachments) {
            EntityFileRelation entityFileRelation = new EntityFileRelation();
            entityFileRelation.setFileId(attachment.getFileId());
            entityFileRelation.setEntityId(entityId);
            entityFileRelation.setEntityType(entityType.typeName);
            relations.add(entityFileRelation);
        }
        entityFileRelationRepository.saveAll(relations);
    }

    public void saveAttachments(String entityId, EntityType entityType, List<StorageFileInfo> attachments) {
        if (attachments == null || attachments.size() == 0) {
            return;
        }
        storageFileInfoRepository.saveAll(attachments);
        saveRelations(entityId, entityType, attachments);
    }

    private String saveFileInStorage(File file, StorageFileInfo fileInfo, Logger logger) {
        storageServiceClient.upload(file, fileInfo);
        if (StringUtils.isBlank(fileInfo.getFileDownloadUrl())) {
            logger.warn("Download URL is empty for file {}", fileInfo.getFileRelPath());
        } else {
            logger.info("upload file {} success: {}", fileInfo.getFileRelPath(), fileInfo.getFileDownloadUrl());
        }
        return fileInfo.getFileDownloadUrl();
    }

    public List<StorageFileInfo> queryFileInfoByFileType(String fileType) {
        return storageFileInfoRepository.queryStorageFileInfoByFileType(fileType);
    }

    public StorageFileInfo getLatestAgentPackage() {
        List<StorageFileInfo> files = storageFileInfoRepository.queryStorageFileInfoByFileTypeOrderByCreateTimeDesc(StorageFileInfo.FileType.AGENT_PACKAGE);
        if (files != null && files.size() > 0) {
            return files.get(0);
        }
        return null;
    }

    public List<StorageFileInfo> filterAttachments(@NotNull List<StorageFileInfo> attachments, @NotNull String fileType) {
        List<StorageFileInfo> data = new ArrayList<>();
        for (StorageFileInfo attachment : attachments) {
            if (fileType.equals(attachment.getFileType())) {
                data.add(attachment);
            }
        }
        return data;
    }

    public StorageFileInfo filterFirstAttachment(@NotNull List<StorageFileInfo> attachments, @NotNull String fileType) {
        for (StorageFileInfo attachment : attachments) {
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
        String filename = FileUtil.getLegalFileName(originFile.getOriginalFilename());
        String fileSuffix = null;
        boolean isMatch = false;
        if (filename == null) {
            throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "error file type: " + filename);
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
                throw new HydraLabRuntimeException(HttpStatus.BAD_REQUEST.value(), "error file type: " + filename);
            }
        }

        if (StringUtils.isEmpty(newFileName)) {
            newFileName = filename.replace(fileSuffix, "") + "_" + System.currentTimeMillis() % 10000 + "_" + fileSuffix;
        }

        newFileName = FileUtil.getLegalFileName(newFileName);
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
