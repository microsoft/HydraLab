package com.microsoft.hydralab.common.appcenter.entity;

import lombok.Data;

@Data
public class StackFrame {
    /**
     * The fully qualified name of the Class containing the execution point
     * represented by this stack trace element.
     */
    private String className;

    /**
     * The name of the method containing the execution point represented by
     * this stack trace element.
     */
    private String methodName;

    /**
     * The line number of the source line containing the execution point
     * represented by this stack trace element.
     */
    private Integer lineNumber;

    /**
     * The name of the file containing the execution point represented by this
     * stack trace element.
     */
    private String fileName;

}
