package com.framework.core;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;


public class ApiResponse {

    private final Response rawResponse;

    public ApiResponse(Response rawResponse) {
        this.rawResponse = rawResponse;
    }

    public int statusCode() {
        return rawResponse.statusCode();
    }

    public <T> T as(Class<T> type) {
        return rawResponse.as(type);
    }

    public JsonPath jsonPath() {
        return rawResponse.jsonPath();
    }

    public String asString() {
        return rawResponse.asString();
    }

    public long responseTimeMs() {
        return rawResponse.getTime();
    }

    public String header(String name) {
        return rawResponse.getHeader(name);
    }

    /** Escape hatch for advanced, REST Assured-specific assertions/extraction. */
    public Response raw() {
        return rawResponse;
    }
}
