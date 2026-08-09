package com.framework.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;


public final class ResponseSpecFactory {

    private ResponseSpecFactory() {
    }

    public static ResponseSpecification defaultJson() {
        return new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .build();
    }

    public static ResponseSpecification withMaxResponseTime(long millis) {
        return new ResponseSpecBuilder()
                .expectResponseTime(Matchers.lessThanOrEqualTo(millis))
                .build();
    }
}
