package com.framework.core;

import io.restassured.http.Method;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;


@Getter
@Builder
public class ApiRequest {

    @Builder.Default
    private final Method method = Method.GET;

    private final String path;

    @Builder.Default
    private final Map<String, Object> pathParams = Collections.emptyMap();

    @Builder.Default
    private final Map<String, Object> queryParams = Collections.emptyMap();

    @Builder.Default
    private final Map<String, Object> headers = Collections.emptyMap();

    private final Object body;
}
