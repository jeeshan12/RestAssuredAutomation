package com.framework.auth;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class AuthConfig {

    private final AuthType authType;

    // OAuth2 (client-credentials and/or password grant)
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;
    private final String scope;

    // API key
    private final String headerName;
    private final String headerValue;

    // Static bearer
    private final String bearerToken;
}
