package com.framework.exceptions;

/** Thrown when an {@link com.framework.core.ApiClient} fails to execute a request. */
public class ApiExecutionException extends FrameworkException {

    public ApiExecutionException(String message) {
        super(message);
    }

    public ApiExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
