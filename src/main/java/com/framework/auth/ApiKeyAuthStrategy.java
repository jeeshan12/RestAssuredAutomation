package com.framework.auth;

import io.restassured.specification.RequestSpecification;

/** Attaches a static API key as a header - e.g. {@code x-api-key} */
public class ApiKeyAuthStrategy implements AuthStrategy {

    private final String headerName;
    private final String headerValue;

    public ApiKeyAuthStrategy(String headerName, String headerValue) {
        this.headerName = headerName;
        this.headerValue = headerValue;
    }

    @Override
    public RequestSpecification apply(RequestSpecification requestSpecification) {
        return requestSpecification.header(headerName, headerValue);
    }
}
