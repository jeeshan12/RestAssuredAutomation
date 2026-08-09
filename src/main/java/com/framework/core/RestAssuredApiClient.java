package com.framework.core;

import com.framework.auth.AuthStrategy;
import com.framework.exceptions.ApiExecutionException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;


public class RestAssuredApiClient implements ApiClient {

    private final RequestSpecification baseSpec;
    private final AuthStrategy authStrategy;

    public RestAssuredApiClient(RequestSpecification baseSpec, AuthStrategy authStrategy) {
        this.baseSpec = baseSpec;
        this.authStrategy = authStrategy;
    }

    @Override
    public ApiResponse execute(ApiRequest request) {
        try {
            RequestSpecification spec = RestAssured.given().spec(baseSpec);
            spec = authStrategy.apply(spec);

            request.getHeaders().forEach(spec::header);
            request.getPathParams().forEach(spec::pathParam);
            request.getQueryParams().forEach(spec::queryParam);
            if (request.getBody() != null) {
                spec.body(request.getBody());
            }

            Response response = spec.request(request.getMethod(), request.getPath());
            return new ApiResponse(response);
        } catch (Exception e) {
            throw new ApiExecutionException(
                    "Failed to execute " + request.getMethod() + " " + request.getPath(), e);
        }
    }
}
