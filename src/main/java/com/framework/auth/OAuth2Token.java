package com.framework.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuth2Token {

    private final String accessToken;
    private final String tokenType;
    private final String refreshToken;
    private final String scope;
    private final long expiresInSeconds;
    private final long obtainedAtEpochMillis;

    /**
     * @param bufferSeconds how much earlier than the real expiry to treat the token as stale,
     *                       so a request in flight never races a token expiring mid-call.
     */
    public boolean isExpired(long bufferSeconds) {
        long expiryEpochMillis = obtainedAtEpochMillis + (expiresInSeconds - bufferSeconds) * 1000L;
        return System.currentTimeMillis() >= expiryEpochMillis;
    }

    public String authorizationHeaderValue() {
        String type = (tokenType == null || tokenType.isBlank()) ? "Bearer" : tokenType;
        return type + " " + accessToken;
    }
}
