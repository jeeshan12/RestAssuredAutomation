package com.framework.services.booking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response body for {@code POST /booking}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingResponse {
    private Integer bookingid;
    private Booking booking;
}
