package com.framework.auth;

import io.restassured.specification.RequestSpecification;

/** HTTP Basic auth, sent preemptively (no challenge/response round trip). */
public class BasicAuthStrategy implements AuthStrategy {

    private final String username;
    private final String password;

    public BasicAuthStrategy(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public RequestSpecification apply(RequestSpecification requestSpecification) {
        return requestSpecification.auth().preemptive().basic(username, password);
    }
}
