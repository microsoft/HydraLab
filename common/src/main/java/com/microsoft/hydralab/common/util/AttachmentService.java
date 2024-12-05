// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.util;

import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.entity.common.EntityFileRelation;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.StorageFileInfo;
import com.microsoft.hydralab.common.entity.common.TestJsonInfo;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.repository.EntityFileRelationRepository;
import com.microsoft.hydralab.common.repository.StorageFileInfoRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentService {
    @Resource
    StorageFileInfoRepository storageFileInfoRepository;
    @Resource
    TestJsonInfoRepository testJsonInfoRepository;
    @Resource
    EntityFileRelationRepository entityFileRelationRepository;
    @Resource
    StorageServiceClientProxy storageServiceClientProxy;

    public StorageFileInfo addAttachment(String entityId, EntityType entityType, StorageFileInfo storageFileInfo, File file, Logger logger) {
        boolean recordExists = false;

        storageFileInfo.setBlobContainer(entityType.storageContainer);
        List<StorageFileInfo> tempFileInfos = storageFileInfoRepository.queryStorageFileInfoByMd5(storageFileInfo.getMd5());
        for (StorageFileInfo tempFileInfo : tempFileInfos) {
            if (compareFileInfo(storageFileInfo, tempFileInfo)) {
                storageFileInfo = updateFileInStorageAndDB(tempFileInfo, file, entityType, logger);
                recordExists = true;
                break;
            }
        }
        if (!recordExists) {
            storageFileInfo = saveFileInStorageAndDB(storageFileInfo, file, entityType, logger);
        }
        saveRelation(entityId, entityType, storageFileInfo);
        file.delete();
        return storageFileInfo;
    }

    public void removeAttachment(String entityId, EntityType entityType, String fileId) {
        EntityFileRelation entityFileRelation = new EntityFileRelation(entityId, entityType.typeName, fileId);
        entityFileRelationRepository.delete(entityFileRelation);
    }

    public StorageFileInfo saveFileInStorageAndDB(StorageFileInfo storageFileInfo, File file, EntityType entityType, Logger logger) {
        storageFileInfo.setFileId(UUID.randomUUID().toString());
        storageFileInfo.setFileParser(PkgUtil.analysisFile(file, entityType));
        storageFileInfo.setCreateTime(new Date());
        storageFileInfo.setUpdateTime(new Date());
        storageFileInfo.setBlobContainer(entityType.storageContainer);
        storageFileInfo.setBlobUrl(saveFileInStorage(file, storageFileInfo, logger));
        storageFileInfoRepository.save(storageFileInfo);
        return storageFileInfo;
    }

    public StorageFileInfo updateFileInStorageAndDB(StorageFileInfo oldFileInfo, File file, EntityType entityType, Logger logger) {
        int days = (int) ((new Date().getTime() - oldFileInfo.getUpdateTime().getTime()) / 1000 / 60 / 60 / 24);
        if (storageServiceClientProxy.fileExpiryEnabled() && days >= storageServiceClientProxy.getStorageFileExpiryDay()) {
            oldFileInfo.setUpdateTime(new Date());
            oldFileInfo.setBlobContainer(entityType.storageContainer);
            oldFileInfo.setBlobUrl(saveFileInStorage(file, oldFileInfo, logger));
            storageFileInfoRepository.save(oldFileInfo);
        }
        return oldFileInfo;
    }

    public TestJsonInfo addTestJsonFile(TestJsonInfo testJsonInfo, File file, EntityType entityType, Logger logger) {
        StorageFileInfo storageFileInfo = new StorageFileInfo(file, testJsonInfo.getBlobPath(), StorageFileInfo.FileType.T2C_JSON_FILE, entityType);
        testJsonInfo.setBlobUrl(saveFileInStorage(file, storageFileInfo, logger));
        testJsonInfo.setBlobContainer(entityType.storageContainer);
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
        } else {
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
        if (newFileInfo.getFileName().equals(oldFileInfo.getFileName())
                && ((newFileInfo.getLoadType() == null && oldFileInfo.getLoadType() == null) ||
                (newFileInfo.getLoadType() != null && newFileInfo.getLoadType().equals(oldFileInfo.getLoadType())))
                && newFileInfo.getLoadDir().equals(oldFileInfo.getLoadDir())
                && newFileInfo.getBlobContainer().equals(oldFileInfo.getBlobContainer())
                && newFileInfo.getFileType().equals(oldFileInfo.getFileType())) {
            return true;
        }
        return false;
    }

    public List<StorageFileInfo> getAttachments(String entityId, EntityType entityType) {
        List<StorageFileInfo> result = new ArrayList<>();

        List<EntityFileRelation> fileRelations = entityFileRelationRepository.queryAllByEntityIdAndEntityTypeOrderByFileOrderAsc(entityId, entityType.typeName);
        for (EntityFileRelation fileRelation : fileRelations) {
            StorageFileInfo tempFileInfo = storageFileInfoRepository.findById(fileRelation.getFileId()).get();
            if (tempFileInfo != null) {
                result.add(tempFileInfo);
            }
        }
        return result;
    }

    public void saveRelation(String entityId, EntityType entityType, StorageFileInfo storageFileInfo) {
        int maxOrder = getMaxOrder(entityId, entityType);
        EntityFileRelation entityFileRelation = new EntityFileRelation();
        entityFileRelation.setEntityId(entityId);
        entityFileRelation.setEntityType(entityType.typeName);
        entityFileRelation.setFileId(storageFileInfo.getFileId());
        entityFileRelation.setFileOrder(maxOrder + 1);
        entityFileRelationRepository.save(entityFileRelation);
    }

    public void saveRelations(String entityId, EntityType entityType, List<StorageFileInfo> attachments) {
        if (attachments == null) {
            return;
        }
        int maxOrder = getMaxOrder(entityId, entityType);
        List<EntityFileRelation> relations = new ArrayList<>();
        for (StorageFileInfo attachment : attachments) {
            EntityFileRelation entityFileRelation = new EntityFileRelation();
            entityFileRelation.setFileId(attachment.getFileId());
            entityFileRelation.setEntityId(entityId);
            entityFileRelation.setEntityType(entityType.typeName);
            entityFileRelation.setFileOrder(++maxOrder);
            relations.add(entityFileRelation);
        }
        entityFileRelationRepository.saveAll(relations);
    }

    private int getMaxOrder(String entityId, EntityType entityType) {
        EntityFileRelation latestRelation = entityFileRelationRepository.findTopByEntityIdAndEntityTypeOrderByFileOrderDesc(entityId, entityType.typeName);
        return latestRelation == null ? 0 : latestRelation.getFileOrder();
    }

    public void saveAttachments(String entityId, EntityType entityType, List<StorageFileInfo> attachments) {
        if (attachments == null || attachments.size() == 0) {
            return;
        }
        storageFileInfoRepository.saveAll(attachments);
        saveRelations(entityId, entityType, attachments);
    }

    private String saveFileInStorage(File file, StorageFileInfo storageFileInfo, Logger logger) {
        storageServiceClientProxy.upload(file, storageFileInfo);
        if (StringUtils.isBlank(storageFileInfo.getBlobUrl())) {
            logger.warn("Download URL is empty for file {}", storageFileInfo.getBlobPath());
        } else {
            logger.info("upload file {} success: {}", storageFileInfo.getBlobPath(), storageFileInfo.getBlobUrl());
        }
        return storageFileInfo.getBlobUrl();
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
        String originalFilename = originFile.getOriginalFilename();
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new HydraLabRuntimeException("Invalid filename");
        }
        String filename = FileUtil.getLegalFileName(originalFilename);
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
