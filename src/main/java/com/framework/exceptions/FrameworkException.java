package com.framework.exceptions;

/**
 * Root of the framework's exception hierarchy. Kept unchecked deliberately: a broken HTTP call,
 * a bad config value, or a failed auth handshake means the test can't meaningfully continue, so
 * forcing every caller to declare/catch it would just add noise.
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
