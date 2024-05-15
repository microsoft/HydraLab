package com.microsoft.hydralab.common.exception.reporter;

public interface ExceptionReporter {

    /**
     * @param e     exception
     * @param fatal If true, this crash report is an application crash.
     */
    void reportException(Exception e, boolean fatal);

    /**
     * @param e      exception
     * @param thread thread
     * @param fatal  If true, this crash report is an application crash.
     */
    void reportException(Exception e, Thread thread, boolean fatal);
}