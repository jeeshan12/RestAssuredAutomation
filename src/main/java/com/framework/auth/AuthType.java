package com.framework.auth;

/** The auth mechanisms {@link AuthStrategyFactory} knows how to build. */
public enum AuthType {
    NONE,
    BASIC,
    BEARER,
    API_KEY,
    OAUTH2_CLIENT_CREDENTIALS,
    OAUTH2_PASSWORD,
    COOKIE_TOKEN
}
