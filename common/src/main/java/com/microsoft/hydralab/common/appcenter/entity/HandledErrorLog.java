package com.microsoft.hydralab.common.appcenter.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class HandledErrorLog implements Log {

    /**
     * Log type.
     */
    public static final String WARNING_TYPE = "handledError";

    /**
     * Exception associated to the error.
     */
    private static final String EXCEPTION = "exception";
    /**
     * Collection of transmissionTargetTokens that this log should be sent to.
     */
    private final Set<String> transmissionTargetTokens = new LinkedHashSet<>();
    private Map<String, String> properties;
    /**
     * Log timestamp.
     */
    private String timestamp;

    /**
     * The session identifier that was provided when the session was started.
     */
    private UUID sid;

    /**
     * Optional distribution group ID value.
     */
    private String distributionGroupId;

    /**
     * The optional user identifier.
     */
    private String userId;
    /**
     * "managedError" -> AppCenter Crash
     * "handledError" -> AppCenter Error
     */
    private String type = "managedError";

    /**
     * Device characteristics associated to this log.
     */
    private Device device;

    /**
     * Transient object tag.
     */
    private Object tag;

    public synchronized void addTransmissionTarget(String transmissionTargetToken) {
        transmissionTargetTokens.add(transmissionTargetToken);
    }

    public synchronized Set<String> getTransmissionTargetTokens() {
        return Collections.unmodifiableSet(transmissionTargetTokens);
    }

    /**
     * Unique identifier for this error.
     */
    private UUID id;

    /**
     * Exception associated to the error.
     */
    private ExceptionInfo exception;
    /**
     * Thread stack traces associated to the error.
     */
    private List<ThreadInfo> threads;

    /**
     * Process identifier.
     */
    private Integer processId;

    /**
     * Process name.
     */
    private String processName;

    /**
     * Parent's process identifier.
     */
    private Integer parentProcessId;

    /**
     * Parent's process name.
     */
    private String parentProcessName;

    /**
     * Error thread identifier.
     */
    private Long errorThreadId;

    /**
     * Error thread name.
     */
    private String errorThreadName;

    /**
     * If true, this crash report is an application crash.
     */
    private Boolean fatal;

    /**
     * Timestamp when the app was launched.
     */
    private String appLaunchTimestamp;

    /**
     * CPU architecture.
     */
    private String architecture;
}
