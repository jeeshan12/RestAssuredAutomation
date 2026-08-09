package com.framework.services.booking;

import com.framework.core.ApiResponse;
import com.framework.services.booking.model.Booking;
import com.framework.services.booking.model.BookingPartialUpdate;


public interface BookingService {

    ApiResponse createBooking(Booking booking);

    ApiResponse getBooking(int id);

    /** PUT - full replace of every field. */
    ApiResponse updateBooking(int id, Booking booking);

    /** PATCH - only the fields set on {@code partialUpdate} are sent. */
    ApiResponse partialUpdateBooking(int id, BookingPartialUpdate partialUpdate);

    ApiResponse deleteBooking(int id);
}
