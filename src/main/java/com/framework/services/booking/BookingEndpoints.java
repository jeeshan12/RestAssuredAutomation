package com.framework.services.booking;

/** Endpoint paths for the restful-booker "booking" resource (https://restful-booker.herokuapp.com/apidoc). */
public final class BookingEndpoints {

    private BookingEndpoints() {
    }

    public static final String BOOKING = "/booking";
    public static final String BOOKING_BY_ID = "/booking/{id}";
}
