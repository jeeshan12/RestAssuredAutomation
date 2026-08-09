package com.framework.auth;

import com.framework.exceptions.AuthenticationException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;


public class CookieTokenAuthStrategy implements AuthStrategy {

    /** No documented expiry for this scheme; treated as valid for this long before a silent re-login. */
    private static final long ASSUMED_TOKEN_LIFETIME_SECONDS = 15 * 60;

    private final String authUrl;
    private final String username;
    private final String password;
    private final String cookieName;
    private final String cacheKey;

    public CookieTokenAuthStrategy(String authUrl, String username, String password) {
        this(authUrl, username, password, "token");
    }

    public CookieTokenAuthStrategy(String authUrl, String username, String password, String cookieName) {
        this.authUrl = authUrl;
        this.username = username;
        this.password = password;
        this.cookieName = cookieName;
        this.cacheKey = "cookie_token|" + authUrl + "|" + username;
    }

    @Override
    public RequestSpecification apply(RequestSpecification requestSpecification) {
        OAuth2Token token = TokenCache.getInstance().getOrFetch(cacheKey, this::login);
        return requestSpecification.cookie(cookieName, token.getAccessToken());
    }

    private OAuth2Token login() {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post(authUrl);

        if (response.statusCode() != 200) {
            throw new AuthenticationException(
                    "Cookie-token login to " + authUrl + " failed: HTTP " + response.statusCode()
                            + " - " + response.asString());
        }

        String token = response.jsonPath().getString("token");
        if (token == null || token.isBlank()) {
            throw new AuthenticationException(
                    "Cookie-token login to " + authUrl + " returned no token: " + response.asString());
        }

        return OAuth2Token.builder()
                .accessToken(token)
                .tokenType("Cookie")
                .expiresInSeconds(ASSUMED_TOKEN_LIFETIME_SECONDS)
                .obtainedAtEpochMillis(System.currentTimeMillis())
                .build();
    }
}
