package com.microsoft.hydralab.common.exception.reporter;

public interface ExceptionReporter {
    void reportException(Exception e);

    void reportException(Exception e, Thread thread);
}