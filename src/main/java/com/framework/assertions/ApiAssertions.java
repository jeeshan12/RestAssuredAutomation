package com.framework.assertions;

import com.framework.core.ApiResponse;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.testng.Assert;

import java.util.Arrays;


public final class ApiAssertions {

    private final ApiResponse response;

    private ApiAssertions(ApiResponse response) {
        this.response = response;
    }

    public static ApiAssertions assertThat(ApiResponse response) {
        return new ApiAssertions(response);
    }

    public ApiAssertions hasStatusCode(int expected) {
        Assert.assertEquals(response.statusCode(), expected,
                "Unexpected status code. Body: " + response.asString());
        return this;
    }

    public ApiAssertions hasStatusCodeIn(int... expectedAny) {
        int actual = response.statusCode();
        boolean matched = Arrays.stream(expectedAny).anyMatch(candidate -> candidate == actual);
        Assert.assertTrue(matched, "Expected status code in " + Arrays.toString(expectedAny)
                + " but got " + actual + ". Body: " + response.asString());
        return this;
    }

    public ApiAssertions hasHeader(String name, String expectedValue) {
        Assert.assertEquals(response.header(name), expectedValue,
                "Unexpected value for header '" + name + "'");
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ApiAssertions bodyMatches(String jsonPath, Matcher matcher) {
        MatcherAssert.assertThat("JSON path '" + jsonPath + "' mismatch. Body: " + response.asString(),
                response.jsonPath().get(jsonPath), matcher);
        return this;
    }

    public ApiAssertions respondsWithin(long millis) {
        Assert.assertTrue(response.responseTimeMs() <= millis,
                "Response took " + response.responseTimeMs() + "ms, expected <= " + millis + "ms");
        return this;
    }

    /** Drops back to the raw response, for callers that want to extract a typed model next. */
    public ApiResponse andReturn() {
        return response;
    }
}
