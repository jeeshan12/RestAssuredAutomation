package com.framework.services.booking;

import com.framework.core.ApiClient;
import com.framework.core.ApiResponse;
import com.framework.services.BaseService;
import com.framework.services.booking.model.Booking;
import com.framework.services.booking.model.BookingPartialUpdate;

import java.util.Map;

public class BookingServiceImpl extends BaseService implements BookingService {

    public BookingServiceImpl(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    public ApiResponse createBooking(Booking booking) {
        return post(BookingEndpoints.BOOKING, booking);
    }

    @Override
    public ApiResponse getBooking(int id) {
        return get(BookingEndpoints.BOOKING_BY_ID, Map.of("id", id));
    }

    @Override
    public ApiResponse updateBooking(int id, Booking booking) {
        return put(BookingEndpoints.BOOKING_BY_ID, Map.of("id", id), booking);
    }

    @Override
    public ApiResponse partialUpdateBooking(int id, BookingPartialUpdate partialUpdate) {
        return patch(BookingEndpoints.BOOKING_BY_ID, Map.of("id", id), partialUpdate);
    }

    @Override
    public ApiResponse deleteBooking(int id) {
        return delete(BookingEndpoints.BOOKING_BY_ID, Map.of("id", id));
    }
}
