// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.hydralab.common.entity.common.scanner;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author zhoule
 * @date 11/16/2023
 */

@Data
public class LeakInfo implements Serializable {
    private String keyword;
    private List<String> LeakWordList;

    public LeakInfo(String keyword, List<String> LeakWordList) {
        this.keyword = keyword;
        this.LeakWordList = LeakWordList;
    }
}
