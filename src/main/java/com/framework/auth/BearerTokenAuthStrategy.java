package com.framework.auth;

import io.restassured.specification.RequestSpecification;

import java.util.function.Supplier;

/**
 * Attaches a Bearer token supplied by the caller. Takes a {@link Supplier} rather than a bare
 * {@code String} so a token obtained/rotated by some external mechanism (e.g. a CI secret that
 * changes per run) can be read lazily on every request instead of being frozen at construction
 * time.
 */
public class BearerTokenAuthStrategy implements AuthStrategy {

    private final Supplier<String> tokenSupplier;

    public BearerTokenAuthStrategy(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    public BearerTokenAuthStrategy(String staticToken) {
        this(() -> staticToken);
    }

    @Override
    public RequestSpecification apply(RequestSpecification requestSpecification) {
        return requestSpecification.header("Authorization", "Bearer " + tokenSupplier.get());
    }
}
