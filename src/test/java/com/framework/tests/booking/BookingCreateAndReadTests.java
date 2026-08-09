package com.framework.tests.booking;

import com.framework.annotations.Serial;
import com.framework.assertions.ApiAssertions;
import com.framework.core.ApiResponse;
import com.framework.listeners.RetryAnalyzer;
import com.framework.services.booking.model.Booking;
import com.framework.services.booking.model.BookingDates;
import com.framework.services.booking.model.CreateBookingResponse;
import com.framework.tests.base.BookingBaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;


@Serial(reason = "restful-booker rate-limits request bursts (418); serialize all calls to it")
public class BookingCreateAndReadTests extends BookingBaseTest {

    @Test(description = "POST /booking creates a booking and echoes the submitted fields back",
            retryAnalyzer = RetryAnalyzer.class)
    public void createBooking_returnsCreatedBooking() {
        Booking booking = Booking.builder()
                .firstname("Ada")
                .lastname("Lovelace")
                .totalprice(150)
                .depositpaid(true)
                .bookingdates(BookingDates.builder().checkin("2026-04-01").checkout("2026-04-05").build())
                .additionalneeds("Breakfast")
                .build();

        ApiResponse response = bookingService.createBooking(booking);

        ApiAssertions.assertThat(response).hasStatusCode(200);
        CreateBookingResponse created = response.as(CreateBookingResponse.class);
        Assert.assertNotNull(created.getBookingid());
        Assert.assertEquals(created.getBooking().getFirstname(), "Ada");
        Assert.assertEquals(created.getBooking().getTotalprice(), Integer.valueOf(150));
    }

    @Test(description = "GET /booking/{id} returns a previously created booking",
            retryAnalyzer = RetryAnalyzer.class)
    public void getBooking_returnsCreatedBooking() {
        Booking booking = Booking.builder()
                .firstname("Grace")
                .lastname("Hopper")
                .totalprice(200)
                .depositpaid(false)
                .bookingdates(BookingDates.builder().checkin("2026-05-01").checkout("2026-05-10").build())
                .additionalneeds("Late checkout")
                .build();
        ApiResponse createResponse = bookingService.createBooking(booking);
        ApiAssertions.assertThat(createResponse).hasStatusCode(200);
        int id = createResponse.as(CreateBookingResponse.class).getBookingid();

        ApiResponse response = bookingService.getBooking(id);

        ApiAssertions.assertThat(response).hasStatusCode(200);
        Booking fetched = response.as(Booking.class);
        Assert.assertEquals(fetched.getFirstname(), "Grace");
        Assert.assertEquals(fetched.getLastname(), "Hopper");
    }

    @Test(description = "GET /booking/{id} for a non-existent id returns 404",
            retryAnalyzer = RetryAnalyzer.class)
    public void getBooking_unknownId_returnsNotFound() {
        ApiResponse response = bookingService.getBooking(Integer.MAX_VALUE - 1);

        ApiAssertions.assertThat(response).hasStatusCode(404);
    }
}
