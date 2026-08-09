package com.framework.auth;

import io.restassured.specification.RequestSpecification;


public interface AuthStrategy {

    RequestSpecification apply(RequestSpecification requestSpecification);
}
