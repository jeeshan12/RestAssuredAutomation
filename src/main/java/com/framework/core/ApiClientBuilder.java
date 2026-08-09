package com.framework.core;

import com.framework.auth.AuthStrategy;
import com.framework.auth.NoAuthStrategy;
import com.framework.exceptions.FrameworkConfigException;
import com.framework.specs.RequestSpecFactory;
import io.restassured.filter.Filter;
import io.restassured.specification.RequestSpecification;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>{@code
 * ApiClient client = ApiClientBuilder.create()
 *     .baseUri("https://api.example.com")
 *     .authStrategy(AuthStrategyFactory.from(authConfig))
 *     .build();
 * }</pre>
 */
public final class ApiClientBuilder {

    private String baseUri;
    private AuthStrategy authStrategy = new NoAuthStrategy();
    private final List<Filter> extraFilters = new ArrayList<>();
    private int connectionTimeoutMs = 10_000;
    private int readTimeoutMs = 10_000;

    private ApiClientBuilder() {
    }

    public static ApiClientBuilder create() {
        return new ApiClientBuilder();
    }

    public ApiClientBuilder baseUri(String baseUri) {
        this.baseUri = baseUri;
        return this;
    }

    public ApiClientBuilder authStrategy(AuthStrategy authStrategy) {
        this.authStrategy = authStrategy;
        return this;
    }

    public ApiClientBuilder authStrategy() {
        return this;
    }

    public ApiClientBuilder addFilter(Filter filter) {
        this.extraFilters.add(filter);
        return this;
    }

    public ApiClientBuilder connectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
        return this;
    }

    public ApiClientBuilder readTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
        return this;
    }

    public ApiClient build() {
        if (baseUri == null || baseUri.isBlank()) {
            throw new FrameworkConfigException("ApiClientBuilder requires a baseUri");
        }
        RequestSpecification spec = RequestSpecFactory.create(baseUri, connectionTimeoutMs, readTimeoutMs, extraFilters);
        return new RestAssuredApiClient(spec, authStrategy);
    }
}
