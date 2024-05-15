package com.microsoft.hydralab.common.appcenter.entity;

import lombok.Data;

import java.util.List;
@Data
public class ExceptionInfo {
    /**
     * Exception type (fully qualified class name).
     */
    private String type;

    /**
     * Exception message.
     */
    private String message;

    /**
     * Raw stack trace. Sent when the frames property is either missing or unreliable (used for Xamarin exceptions).
     */
    private String stackTrace;

    /**
     * Exception stack trace elements.
     */
    private List<StackFrame> frames;

    /**
     * Inner exceptions of this exception.
     */
    private List<ExceptionInfo> innerExceptions;

    /**
     * Name of the wrapper SDK that emitted this exception.
     * Consists of the name of the SDK and the wrapper platform,
     * e.g. "appcenter.xamarin", "hockeysdk.cordova".
     */
    private String wrapperSdkName;

    /**
     * The path to the minidump file. Used for reports from the NDK.
     * This is stored locally but will not be sent to the server.
     */
    private String minidumpFilePath;
}
