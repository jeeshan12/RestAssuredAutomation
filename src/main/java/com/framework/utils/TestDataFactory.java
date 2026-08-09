package com.framework.utils;

import net.datafaker.Faker;


public final class TestDataFactory {

    private static final Faker FAKER = new Faker();

    private TestDataFactory() {
    }

    public static String randomFullName() {
        return FAKER.name().fullName();
    }

    public static String randomJobTitle() {
        return FAKER.job().title();
    }

    public static String randomEmail() {
        return FAKER.internet().emailAddress();
    }

    public static String randomPassword() {
        return FAKER.internet().password(10, 16, true, true, true);
    }
}
