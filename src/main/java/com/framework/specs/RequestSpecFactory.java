package com.framework.specs;

import com.framework.filters.RequestResponseLoggingFilter;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.List;


public final class RequestSpecFactory {

    private RequestSpecFactory() {
    }

    public static RequestSpecification create(String baseUri,
                                                int connectionTimeoutMs,
                                                int readTimeoutMs,
                                                List<Filter> extraFilters) {
        RestAssuredConfig restAssuredConfig = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", connectionTimeoutMs)
                        .setParam("http.socket.timeout", readTimeoutMs));

        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(restAssuredConfig)
                .addFilter(new RequestResponseLoggingFilter())
                .addFilter(new AllureRestAssured());

        extraFilters.forEach(builder::addFilter);
        return builder.build();
    }
}
