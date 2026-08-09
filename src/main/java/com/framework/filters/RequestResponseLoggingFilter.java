package com.framework.filters;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RequestResponseLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                            FilterableResponseSpecification responseSpec,
                            FilterContext ctx) {
        log.info(">>> {} {}", requestSpec.getMethod(), requestSpec.getURI());
        Object requestBody = requestSpec.getBody();
        if (log.isDebugEnabled() && requestBody != null) {
            log.debug("Request body: {}", requestBody);
        }

        long startedAt = System.currentTimeMillis();
        Response response = ctx.next(requestSpec, responseSpec);
        long elapsedMs = System.currentTimeMillis() - startedAt;

        log.info("<<< {} in {}ms", response.getStatusCode(), elapsedMs);
        if (log.isDebugEnabled()) {
            log.debug("Response body: {}", response.getBody().asPrettyString());
        }
        return response;
    }
}
