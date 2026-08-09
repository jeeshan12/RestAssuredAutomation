package com.framework.auth;

import io.restassured.specification.RequestSpecification;


public class OAuth2ClientCredentialsStrategy implements AuthStrategy {

    private final AuthConfig config;
    private final OAuth2Client client;
    private final String cacheKey;

    public OAuth2ClientCredentialsStrategy(AuthConfig config) {
        this.config = config;
        this.client = new OAuth2Client(config.getTokenUrl());
        this.cacheKey = "client_credentials|" + config.getTokenUrl() + "|" + config.getClientId() + "|" + config.getScope();
    }

    @Override
    public RequestSpecification apply(RequestSpecification requestSpecification) {
        OAuth2Token token = TokenCache.getInstance().getOrFetch(cacheKey,
                () -> client.fetchClientCredentialsToken(config.getClientId(), config.getClientSecret(), config.getScope()));
        return requestSpecification.header("Authorization", token.authorizationHeaderValue());
    }
}
