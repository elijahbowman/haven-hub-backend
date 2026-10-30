package com.example.havenhub.booking;

import com.example.havenhub.booking.application.BookingConflictException;
import com.example.havenhub.booking.application.BookingService;
import com.example.havenhub.booking.application.dto.NewBookingDTO;
import com.example.havenhub.listing.domain.Listing;
import com.example.havenhub.sharedkernel.service.StatusNotification;
import com.example.havenhub.support.AbstractIntegrationTest;
import com.example.havenhub.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The application checks availability before inserting, but two requests can pass
 * that check at the same time. This test fires many simultaneous bookings for the
 * same nights and verifies that the booking_no_overlap constraint lets exactly one
 * through.
 */
class BookingConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 20;

    @Autowired
    private BookingService bookingService;

    @Test
    void onlyOneOfManySimultaneousBookingsForTheSameNightsSucceeds() throws Exception {
        User landlord = createUser("landlord@test.dev");
        Listing listing = createListing(landlord, 4);
        List<User> tenants = IntStream.range(0, CONCURRENT_REQUESTS)
                .mapToObj(i -> createUser("tenant" + i + "@test.dev"))
                .toList();

        LocalDate checkIn = LocalDate.now().plusDays(30);
        NewBookingDTO request = new NewBookingDTO(checkIn, checkIn.plusDays(3), listing.getPublicId(), 1);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<Boolean>> outcomes = new ArrayList<>();
            for (User tenant : tenants) {
                outcomes.add(executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(authenticationFor(tenant));
                    try {
                        startGate.await();
                        return bookingService.create(request).getStatus() == StatusNotification.OK;
                    } catch (BookingConflictException | DataIntegrityViolationException rejected) {
                        // Rejected either by the availability check or by the database constraint.
                        return false;
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }

            startGate.countDown();

            long successes = 0;
            for (Future<Boolean> outcome : outcomes) {
                // Any other exception (e.g. a failed commit) fails the test here.
                if (outcome.get(60, SECONDS)) {
                    successes++;
                }
            }

            assertThat(successes).isEqualTo(1);
            assertThat(bookingRepository.findAllByFkListing(listing.getPublicId())).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
