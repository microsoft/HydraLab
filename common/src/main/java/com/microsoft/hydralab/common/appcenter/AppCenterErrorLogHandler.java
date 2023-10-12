/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.hydralab.common.appcenter;


import com.microsoft.hydralab.common.appcenter.entity.Device;
import com.microsoft.hydralab.common.appcenter.entity.ExceptionInfo;
import com.microsoft.hydralab.common.appcenter.entity.HandledErrorLog;
import com.microsoft.hydralab.common.appcenter.entity.StackFrame;
import com.microsoft.hydralab.common.appcenter.entity.ThreadInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;


/**
 * ErrorLogHelper to help constructing, serializing, and de-serializing locally stored error logs.
 * <p>
 * Copied from <a href="https://github.com/microsoft/appcenter-sdk-android/blob/e58dad690beeec67d047bbfb9a3aeecfd6b1ec0b/sdk/appcenter/src/main/java/com/microsoft/appcenter/ingestion/AppCenterIngestion.java">...</a>
 */
public class AppCenterErrorLogHandler {
    private static final Logger logger = LoggerFactory.getLogger(AppCenterErrorLogHandler.class);
    /**
     * For huge stack traces such as giant StackOverflowError, we keep only beginning and end of frames according to this limit.
     */
    public static final int FRAME_LIMIT = 256;

    /**
     * We keep the first half of the limit of frames from the beginning and the second half from end.
     */
    private static final int FRAME_LIMIT_HALF = FRAME_LIMIT / 2;

    /**
     * For huge exception cause chains, we keep only beginning and end of causes according to this limit.
     */
    static final int CAUSE_LIMIT = 16;

    /**
     * We keep the first half of the limit of causes from the beginning and the second half from end.
     */
    private static final int CAUSE_LIMIT_HALF = CAUSE_LIMIT / 2;

    /**
     * Max number of properties.
     */
    private static final int MAX_PROPERTY_COUNT = 64;

    /**
     * Max length of properties.
     */
    public static final int MAX_PROPERTY_ITEM_LENGTH = 125;
    private final Device device;
    private final String userId;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    public AppCenterErrorLogHandler(Device device, String userId) {
        this.device = device;
        this.userId = userId;
    }

    public HandledErrorLog createErrorLog(final Thread thread, final Throwable throwable,
                                          final long initializeTimestamp, boolean fatal) {
        return createErrorLog(thread, throwable, Thread.getAllStackTraces(), initializeTimestamp, fatal);
    }

    public HandledErrorLog createErrorLog(final Thread thread, final Throwable throwable,
                                          final Map<java.lang.Thread, StackTraceElement[]> allStackTraces,
                                          final long initializeTimestamp, boolean fatal) {
        ExceptionInfo exception = getModelExceptionFromThrowable(throwable);

        /* Build error log with a unique identifier. */
        HandledErrorLog errorLog = new HandledErrorLog();
        errorLog.setId(UUID.randomUUID());

        /* Set current time. Will be correlated to session after restart. */
        errorLog.setTimestamp(simpleDateFormat.format(new Date()));

        /* Set user identifier. */
        errorLog.setUserId(userId);

        /* Snapshot device properties. */
        errorLog.setDevice(device);

        /* Process information. Parent one is not available on Android. */
        errorLog.setProcessId(getProcessId());

        loadSystemProperties(errorLog);

        /*
         * Process name is required field for crash processing but cannot always be available,
         * make sure we send a default value if not found.
         */
        if (errorLog.getProcessName() == null) {
            errorLog.setProcessName("");
        }

        /* CPU architecture. */
        errorLog.setArchitecture(System.getProperty("os.arch"));

        /* Thread in error information. */
        errorLog.setErrorThreadId(thread.getId());
        errorLog.setErrorThreadName(thread.getName());

        /* Uncaught exception or managed exception. */
        errorLog.setFatal(fatal);
        if (!fatal) {
            errorLog.setType(HandledErrorLog.TYPE);
        }

        /* Application launch time. */
        errorLog.setAppLaunchTimestamp(simpleDateFormat.format(new Date(initializeTimestamp)));

        /* Attach exceptions. */
        errorLog.setException(exception);

        /* Attach thread states. */
        if (allStackTraces != null) {
            List<ThreadInfo> threads = new ArrayList<>(allStackTraces.size());
            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                ThreadInfo javaThread = new ThreadInfo();
                javaThread.setId(entry.getKey().getId());
                javaThread.setName(entry.getKey().getName());
                javaThread.setFrames(getModelFramesFromStackTrace(entry.getValue()));
                threads.add(javaThread);
            }
            errorLog.setThreads(threads);
        }
        return errorLog;
    }

    private void loadSystemProperties(HandledErrorLog errorLog) {
        Map<String, String> systemInfoMap = new HashMap<>();
        try {
            Runtime r = Runtime.getRuntime();
            Properties props = System.getProperties();
            InetAddress addr = InetAddress.getLocalHost();
            Map<String, String> map = System.getenv();
            String userName = map.get("USERNAME");
            String computerName = map.get("COMPUTERNAME");
            String userDomain = map.get("USERDOMAIN");

            // Store the system information into the HashMap
            systemInfoMap.put("userName", userName);
            systemInfoMap.put("computerName", computerName);
            systemInfoMap.put("userDomain", userDomain);
            systemInfoMap.put("hostname", addr.getHostName());
            systemInfoMap.put("totalMemory", Long.toString(r.totalMemory()));
            systemInfoMap.put("freeMemory", Long.toString(r.freeMemory()));
            systemInfoMap.put("availableProcessors", Integer.toString(r.availableProcessors()));
            systemInfoMap.put("javaVersion", props.getProperty("java.version"));
            systemInfoMap.put("javaVendor", props.getProperty("java.vendor"));
            systemInfoMap.put("javaHome", props.getProperty("java.home"));
            systemInfoMap.put("javaVmSpecVersion", props.getProperty("java.vm.specification.version"));
            systemInfoMap.put("javaVmSpecVendor", props.getProperty("java.vm.specification.vendor"));
            systemInfoMap.put("javaVmSpecName", props.getProperty("java.vm.specification.name"));
            systemInfoMap.put("javaVmVersion", props.getProperty("java.vm.version"));
            systemInfoMap.put("javaVmVendor", props.getProperty("java.vm.vendor"));
            systemInfoMap.put("javaVmName", props.getProperty("java.vm.name"));
            systemInfoMap.put("javaSpecVersion", props.getProperty("java.specification.version"));
            systemInfoMap.put("javaSpecName", props.getProperty("java.specification.name"));
            systemInfoMap.put("javaClassVersion", props.getProperty("java.class.version"));
            systemInfoMap.put("javaIoTmpdir", props.getProperty("java.io.tmpdir"));
            systemInfoMap.put("osName", props.getProperty("os.name"));
            systemInfoMap.put("osArch", props.getProperty("os.arch"));
            systemInfoMap.put("osVersion", props.getProperty("os.version"));
            systemInfoMap.put("fileSeparator", props.getProperty("file.separator"));
            systemInfoMap.put("pathSeparator", props.getProperty("path.separator"));
            systemInfoMap.put("lineSeparator", props.getProperty("line.separator"));
            systemInfoMap.put("userNameProp", props.getProperty("user.name"));
            systemInfoMap.put("userHome", props.getProperty("user.home"));
            systemInfoMap.put("userDir", props.getProperty("user.dir"));

            File file = new File("");
            long totalSpace = file.getTotalSpace();
            long freeSpace = file.getFreeSpace();
            long remainingDiskSpace = freeSpace / (1024 * 1024); // Convert to MB

            systemInfoMap.put("remainingDiskSpace", Long.toString(remainingDiskSpace));
            systemInfoMap.put("totalDiskSpace", Long.toString(totalSpace));

        } catch (Exception e) {
            logger.error("Error while loading system properties", e);
        }
        errorLog.setProperties(systemInfoMap);
    }

    public static ExceptionInfo getModelExceptionFromThrowable(Throwable t) {
        ExceptionInfo topException = null;
        ExceptionInfo parentException = null;
        List<Throwable> causeChain = new LinkedList<>();
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            causeChain.add(cause);
        }
        if (causeChain.size() > CAUSE_LIMIT) {
            logger.warn("Crash causes truncated from " + causeChain.size() + " to " + CAUSE_LIMIT + " causes.");
            causeChain.subList(CAUSE_LIMIT_HALF, causeChain.size() - CAUSE_LIMIT_HALF).clear();
        }
        for (Throwable cause : causeChain) {
            ExceptionInfo exception = new ExceptionInfo();
            exception.setType(cause.getClass().getName());
            exception.setMessage(cause.getMessage());
            exception.setFrames(getModelFramesFromStackTrace(cause));
            if (topException == null) {
                topException = exception;
            } else {
                parentException.setInnerExceptions(Collections.singletonList(exception));
            }
            parentException = exception;
        }

        //noinspection ConstantConditions
        return topException;
    }

    private static List<StackFrame> getModelFramesFromStackTrace(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length > FRAME_LIMIT) {
            StackTraceElement[] stackTraceTruncated = new StackTraceElement[FRAME_LIMIT];
            System.arraycopy(stackTrace, 0, stackTraceTruncated, 0, FRAME_LIMIT_HALF);
            System.arraycopy(stackTrace, stackTrace.length - FRAME_LIMIT_HALF, stackTraceTruncated, FRAME_LIMIT_HALF, FRAME_LIMIT_HALF);
            throwable.setStackTrace(stackTraceTruncated);
            logger.warn("Crash frames truncated from " + stackTrace.length + " to " + stackTraceTruncated.length + " frames.");
            stackTrace = stackTraceTruncated;
        }
        return getModelFramesFromStackTrace(stackTrace);
    }

    public static int getProcessId() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        String runtimeName = runtimeMXBean.getName();
        return Integer.parseInt(runtimeName.split("@")[0]);
    }

    private static List<StackFrame> getModelFramesFromStackTrace(StackTraceElement[] stackTrace) {
        List<StackFrame> stackFrames = new ArrayList<>();
        for (StackTraceElement stackTraceElement : stackTrace) {
            stackFrames.add(getModelStackFrame(stackTraceElement));
        }
        return stackFrames;
    }

    private static StackFrame getModelStackFrame(StackTraceElement stackTraceElement) {
        StackFrame stackFrame = new StackFrame();
        stackFrame.setClassName(stackTraceElement.getClassName());
        stackFrame.setMethodName(stackTraceElement.getMethodName());
        stackFrame.setLineNumber(stackTraceElement.getLineNumber());
        stackFrame.setFileName(stackTraceElement.getFileName());
        return stackFrame;
    }
}