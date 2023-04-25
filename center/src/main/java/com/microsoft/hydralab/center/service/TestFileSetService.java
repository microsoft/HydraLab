// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service;

import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.entity.common.EntityType;
import com.microsoft.hydralab.common.entity.common.TestFileSet;
import com.microsoft.hydralab.common.file.StorageServiceClientProxy;
import com.microsoft.hydralab.common.repository.TestFileSetRepository;
import com.microsoft.hydralab.common.util.AttachmentService;
import com.microsoft.hydralab.common.util.CriteriaTypeUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestFileSetService {
    @Resource
    TestFileSetRepository testFileSetRepository;
    @Resource
    AttachmentService attachmentService;
    @Resource
    StorageServiceClientProxy storageServiceClientProxy;

    private Map<String, TestFileSet> testFileSetMap = new HashMap<>();

    public TestFileSet addTestFileSet(TestFileSet testFileSet) {
        testFileSetRepository.save(testFileSet);
        saveFileSetToMem(testFileSet);
        return testFileSet;
    }

    public synchronized void saveFileSetToMem(TestFileSet testFileSet) {
        testFileSetMap.put(testFileSet.getId(), testFileSet);
    }

    public synchronized TestFileSet getFileSetInfo(String fileSetId) {
        TestFileSet testFileSet = testFileSetMap.get(fileSetId);
        if (testFileSet != null) {
            return testFileSet;
        }
        testFileSet = testFileSetRepository.findById(fileSetId).orElse(null);
        if (testFileSet != null) {
            testFileSet.setAttachments(attachmentService.getAttachments(fileSetId, EntityType.APP_FILE_SET));
            saveFileSetToMem(testFileSet);
        }
        return testFileSet;
    }

    public Page<TestFileSet> queryFileSets(int page, int pageSize, List<CriteriaType> criteriaTypes) {
        Specification<TestFileSet> spec = null;

        if (storageServiceClientProxy.fileLimitEnabled()) {
            CriteriaType fileLimitCriteria = getFileLimitCriteria();
            if (criteriaTypes != null) {
                criteriaTypes.add(fileLimitCriteria);
                spec = new CriteriaTypeUtil<TestFileSet>().transferToSpecification(criteriaTypes, false);
            } else {
                spec = new CriteriaTypeUtil<TestFileSet>().transferToSpecification(List.of(fileLimitCriteria), false);
            }
        }
        Sort sortByDate = Sort.by(Sort.Direction.DESC, "ingestTime");
        Page<TestFileSet> pageObj = testFileSetRepository.findAll(spec, PageRequest.of(page, pageSize, sortByDate));
        return pageObj;
    }

    private CriteriaType getFileLimitCriteria() {
        CriteriaType fileLimitCriteria = new CriteriaType();
        fileLimitCriteria.setKey("ingestTime");
        fileLimitCriteria.setOp(CriteriaType.OpType.GreaterThan);
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        fileLimitCriteria.setValue(sdf.format(DateUtils.addDays(new Date(), -1 * storageServiceClientProxy.getStorageFileLimitDay())));
        fileLimitCriteria.setDateFormatString(dateFormat);

        return fileLimitCriteria;
    }

    public void updateFileSetTeam(String teamId, String teamName) {
        List<TestFileSet> testFileSets = testFileSetRepository.findAllByTeamId(teamId);
        testFileSets.forEach(testFileSet -> {
            testFileSet.setTeamName(teamName);
            saveFileSetToMem(testFileSet);
        });

        testFileSetRepository.saveAll(testFileSets);
    }
}
