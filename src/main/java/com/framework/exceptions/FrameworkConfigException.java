package com.framework.exceptions;

/** Thrown for missing/invalid configuration - e.g. an unresolvable properties file or a required key with no value. */
public class FrameworkConfigException extends FrameworkException {

    public FrameworkConfigException(String message) {
        super(message);
    }

    public FrameworkConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
