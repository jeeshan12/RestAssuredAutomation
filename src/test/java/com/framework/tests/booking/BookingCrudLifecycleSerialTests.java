package com.framework.tests.booking;

import com.framework.annotations.Serial;
import com.framework.assertions.ApiAssertions;
import com.framework.core.ApiResponse;
import com.framework.listeners.RetryAnalyzer;
import com.framework.services.booking.model.Booking;
import com.framework.services.booking.model.BookingDates;
import com.framework.services.booking.model.BookingPartialUpdate;
import com.framework.services.booking.model.CreateBookingResponse;
import com.framework.tests.base.BookingBaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;


@Serial(reason = "create -> read -> put -> patch -> delete share a mutable bookingId and must run in order")
public class BookingCrudLifecycleSerialTests extends BookingBaseTest {

    private Integer bookingId;

    @Test(description = "Step 1 (POST): create a booking and capture its id", priority = 1,
            retryAnalyzer = RetryAnalyzer.class)
    public void step1_createBooking() {
        Booking booking = Booking.builder()
                .firstname("Leia")
                .lastname("Organa")
                .totalprice(500)
                .depositpaid(true)
                .bookingdates(BookingDates.builder().checkin("2026-06-01").checkout("2026-06-07").build())
                .additionalneeds("Extra pillow")
                .build();

        ApiResponse response = bookingService.createBooking(booking);
        ApiAssertions.assertThat(response).hasStatusCode(200);

        bookingId = response.as(CreateBookingResponse.class).getBookingid();
        Assert.assertNotNull(bookingId);
        log.info("Lifecycle test created booking id={}", bookingId);
    }

    @Test(description = "Step 2 (GET): read back exactly what step 1 created", priority = 2,
            dependsOnMethods = "step1_createBooking", retryAnalyzer = RetryAnalyzer.class)
    public void step2_readBooking() {
        ApiResponse response = bookingService.getBooking(bookingId);

        ApiAssertions.assertThat(response).hasStatusCode(200);
        Booking fetched = response.as(Booking.class);
        Assert.assertEquals(fetched.getFirstname(), "Leia");
        Assert.assertEquals(fetched.getTotalprice(), Integer.valueOf(500));
    }

    @Test(description = "Step 3 (PUT): full update replaces every field", priority = 3,
            dependsOnMethods = "step2_readBooking", retryAnalyzer = RetryAnalyzer.class)
    public void step3_fullyUpdateBooking() {
        Booking replacement = Booking.builder()
                .firstname("General Organa")
                .lastname("Organa")
                .totalprice(750)
                .depositpaid(false)
                .bookingdates(BookingDates.builder().checkin("2026-07-01").checkout("2026-07-10").build())
                .additionalneeds("Security detail")
                .build();

        ApiResponse response = bookingService.updateBooking(bookingId, replacement);

        ApiAssertions.assertThat(response).hasStatusCode(200);
        Booking updated = response.as(Booking.class);
        Assert.assertEquals(updated.getFirstname(), "General Organa");
        Assert.assertEquals(updated.getTotalprice(), Integer.valueOf(750));
        Assert.assertEquals(updated.getAdditionalneeds(), "Security detail");
    }

    @Test(description = "Step 4 (PATCH): partial update touches only the given fields", priority = 4,
            dependsOnMethods = "step3_fullyUpdateBooking", retryAnalyzer = RetryAnalyzer.class)
    public void step4_partiallyUpdateBooking() {
        BookingPartialUpdate partial = BookingPartialUpdate.builder()
                .totalprice(999)
                .build();

        ApiResponse response = bookingService.partialUpdateBooking(bookingId, partial);
        ApiAssertions.assertThat(response).hasStatusCode(200);

        Booking patched = response.as(Booking.class);
        Assert.assertEquals(patched.getTotalprice(), Integer.valueOf(999));
        // Untouched by the PATCH - proves only totalprice was sent, not a full-object overwrite.
        Assert.assertEquals(patched.getFirstname(), "General Organa");
        Assert.assertEquals(patched.getAdditionalneeds(), "Security detail");
    }

    @Test(description = "Step 5 (DELETE): remove the booking", priority = 5,
            dependsOnMethods = "step4_partiallyUpdateBooking", retryAnalyzer = RetryAnalyzer.class)
    public void step5_deleteBooking() {
        ApiResponse response = bookingService.deleteBooking(bookingId);

        ApiAssertions.assertThat(response).hasStatusCodeIn(200, 201, 204);
    }

    @Test(description = "Step 6 (GET): the deleted booking is gone for good", priority = 6,
            dependsOnMethods = "step5_deleteBooking", retryAnalyzer = RetryAnalyzer.class)
    public void step6_verifyBookingDeleted() {
        ApiResponse response = bookingService.getBooking(bookingId);

        ApiAssertions.assertThat(response).hasStatusCode(404);
    }
}
