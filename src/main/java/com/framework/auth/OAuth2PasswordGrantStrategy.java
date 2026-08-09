package com.framework.auth;

import io.restassured.specification.RequestSpecification;


public class OAuth2PasswordGrantStrategy implements AuthStrategy {

    private final AuthConfig config;
    private final OAuth2Client client;
    private final String cacheKey;

    public OAuth2PasswordGrantStrategy(AuthConfig config) {
        this.config = config;
        this.client = new OAuth2Client(config.getTokenUrl());
        this.cacheKey = "password|" + config.getTokenUrl() + "|" + config.getClientId() + "|"
                + config.getUsername() + "|" + config.getScope();
    }

    @Override
    public RequestSpecification apply(RequestSpecification requestSpecification) {
        OAuth2Token token = TokenCache.getInstance().getOrFetch(cacheKey, () -> client.fetchPasswordGrantToken(
                config.getClientId(), config.getClientSecret(), config.getUsername(), config.getPassword(), config.getScope()));
        return requestSpecification.header("Authorization", token.authorizationHeaderValue());
    }
}
