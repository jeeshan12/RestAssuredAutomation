package com.framework.auth;

import io.restassured.specification.RequestSpecification;

public class NoAuthStrategy implements AuthStrategy {

    @Override
    public RequestSpecification apply(RequestSpecification requestSpecification) {
        return requestSpecification;
    }
}
