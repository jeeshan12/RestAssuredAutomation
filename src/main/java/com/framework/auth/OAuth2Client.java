package com.framework.auth;

import com.framework.exceptions.AuthenticationException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.Map;


public class OAuth2Client {

    private final String tokenUrl;

    public OAuth2Client(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public OAuth2Token fetchClientCredentialsToken(String clientId, String clientSecret, String scope) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("grant_type", "client_credentials");
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        putIfPresent(form, "scope", scope);
        return requestToken(form);
    }

    public OAuth2Token fetchPasswordGrantToken(String clientId, String clientSecret,
                                                String username, String password, String scope) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("grant_type", "password");
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("username", username);
        form.put("password", password);
        putIfPresent(form, "scope", scope);
        return requestToken(form);
    }

    private void putIfPresent(Map<String, Object> form, String key, String value) {
        if (value != null && !value.isBlank()) {
            form.put(key, value);
        }
    }

    private OAuth2Token requestToken(Map<String, Object> form) {
        Response response = RestAssured.given()
                .contentType(ContentType.URLENC)
                .formParams(form)
                .post(tokenUrl);

        if (response.statusCode() != 200) {
            throw new AuthenticationException("OAuth2 token request to " + tokenUrl + " failed: HTTP "
                    + response.statusCode() + " - " + response.asString());
        }

        JsonPath json = response.jsonPath();
        Long expiresIn = json.getObject("expires_in", Long.class);
        return OAuth2Token.builder()
                .accessToken(json.getString("access_token"))
                .tokenType(json.getString("token_type"))
                .refreshToken(json.getString("refresh_token"))
                .scope(json.getString("scope"))
                .expiresInSeconds(expiresIn != null ? expiresIn : 3600L)
                .obtainedAtEpochMillis(System.currentTimeMillis())
                .build();
    }
}
