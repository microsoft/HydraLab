// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.center.service.generation;

import com.microsoft.hydralab.common.util.PageNode;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * @author zhoule
 * @date 07/21/2023
 */

@Service
public class T2CCaseGenerationService extends AbstractCaseGeneration {
    @Override
    public File generateCaseFile(PageNode pageNode, List<PageNode.ExplorePath> explorePaths) {
        return null;
    }

    @Override
    public File generateCaseFile(PageNode pageNode, PageNode.ExplorePath explorePaths, File caseFolder) {
        return null;
    }
}
