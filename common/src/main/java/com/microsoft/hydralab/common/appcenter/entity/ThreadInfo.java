package com.microsoft.hydralab.common.appcenter.entity;

import lombok.Data;

import java.util.List;

@Data
public class ThreadInfo {
    /**
     * Thread identifier.
     */
    private long id;

    /**
     * Thread name.
     */
    private String name;

    /**
     * Stack frames.
     */
    private List<StackFrame> frames;
}
