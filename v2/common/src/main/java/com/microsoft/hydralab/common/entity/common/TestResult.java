package com.microsoft.hydralab.common.entity.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.Entity;

@Entity
@Data
@EqualsAndHashCode(callSuper=true)
public class TestResult extends TaskResult {
    private int totalCount;
    private int failCount;
    public TestResult() {
        super();
    }

    public void analysisState() {
        if (failCount <= 0 && totalCount > 0) {
            this.setState(TaskState.PASS.name());
        } else {
            this.setState(TaskState.FAIL.name());
        }
    }
}
