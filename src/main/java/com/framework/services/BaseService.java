package com.framework.services;

import com.framework.core.ApiClient;
import com.framework.core.ApiRequest;
import com.framework.core.ApiResponse;
import io.restassured.http.Method;

import java.util.Map;

public abstract class BaseService {

    protected final ApiClient apiClient;

    protected BaseService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    protected ApiResponse get(String path) {
        return execute(Method.GET, path, null, null, null);
    }

    protected ApiResponse get(String path, Map<String, Object> pathParams) {
        return execute(Method.GET, path, pathParams, null, null);
    }

    protected ApiResponse get(String path, Map<String, Object> pathParams, Map<String, Object> queryParams) {
        return execute(Method.GET, path, pathParams, queryParams, null);
    }

    protected ApiResponse post(String path, Object body) {
        return execute(Method.POST, path, null, null, body);
    }

    protected ApiResponse post(String path, Map<String, Object> pathParams, Object body) {
        return execute(Method.POST, path, pathParams, null, body);
    }

    protected ApiResponse put(String path, Map<String, Object> pathParams, Object body) {
        return execute(Method.PUT, path, pathParams, null, body);
    }

    protected ApiResponse patch(String path, Map<String, Object> pathParams, Object body) {
        return execute(Method.PATCH, path, pathParams, null, body);
    }

    protected ApiResponse delete(String path, Map<String, Object> pathParams) {
        return execute(Method.DELETE, path, pathParams, null, null);
    }

    private ApiResponse execute(Method method, String path, Map<String, Object> pathParams,
                                 Map<String, Object> queryParams, Object body) {
        ApiRequest.ApiRequestBuilder builder = ApiRequest.builder()
                .method(method)
                .path(path);
        if (pathParams != null) {
            builder.pathParams(pathParams);
        }
        if (queryParams != null) {
            builder.queryParams(queryParams);
        }
        if (body != null) {
            builder.body(body);
        }
        return apiClient.execute(builder.build());
    }
}
