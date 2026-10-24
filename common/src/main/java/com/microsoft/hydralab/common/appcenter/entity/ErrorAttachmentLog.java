package com.microsoft.hydralab.common.appcenter.entity;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ErrorAttachmentLog {

    /**
     * Plain text mime type.
     */
    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

    static final Charset CHARSET = StandardCharsets.UTF_8;

    /**
     * Error attachment identifier.
     */
    private UUID id;

    /**
     * Error log identifier to attach this log to.
     */
    private UUID errorId;

    /**
     * Content type (text/plain for text).
     */
    private String contentType;
    private String type = "errorAttachment";

    /**
     * File name.
     */
    private String fileName;

    /**
     * Data encoded as base64 when in JSON.
     */
    private byte[] data;
}
