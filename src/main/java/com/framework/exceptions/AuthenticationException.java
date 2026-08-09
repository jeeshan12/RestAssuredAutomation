package com.framework.exceptions;

/** Thrown when an {@link com.framework.auth.AuthStrategy} fails to authenticate a request. */
public class AuthenticationException extends FrameworkException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
