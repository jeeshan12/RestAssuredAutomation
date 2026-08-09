package com.framework.auth;

import com.framework.exceptions.AuthenticationException;


public final class AuthStrategyFactory {

    private AuthStrategyFactory() {
    }

    public static AuthStrategy from(AuthConfig config) {
        if (config == null || config.getAuthType() == null) {
            return new NoAuthStrategy();
        }
        return switch (config.getAuthType()) {
            case NONE -> new NoAuthStrategy();
            case BASIC -> new BasicAuthStrategy(config.getUsername(), config.getPassword());
            case BEARER -> new BearerTokenAuthStrategy(config.getBearerToken());
            case API_KEY -> new ApiKeyAuthStrategy(config.getHeaderName(), config.getHeaderValue());
            case OAUTH2_CLIENT_CREDENTIALS -> new OAuth2ClientCredentialsStrategy(config);
            case OAUTH2_PASSWORD -> new OAuth2PasswordGrantStrategy(config);
            case COOKIE_TOKEN -> new CookieTokenAuthStrategy(config.getTokenUrl(), config.getUsername(), config.getPassword());
            default -> throw new AuthenticationException("Unsupported auth type: " + config.getAuthType());
        };
    }
}
