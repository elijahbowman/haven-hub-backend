package hearthway.booking;

import hearthway.booking.domain.Booking;
import hearthway.listing.domain.Listing;
import hearthway.support.AbstractIntegrationTest;
import hearthway.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Users can only cancel their own bookings, landlords only reservations on their
 * own listings, and only owners can delete a listing.
 */
class OwnershipAuthorizationIntegrationTest extends AbstractIntegrationTest {

    private User landlord;
    private User otherLandlord;
    private User tenant;
    private User otherTenant;
    private Listing listing;
    private Booking booking;

    @BeforeEach
    void setUp() {
        landlord = createUser("landlord@test.dev");
        otherLandlord = createUser("other-landlord@test.dev");
        tenant = createUser("tenant@test.dev");
        otherTenant = createUser("other-tenant@test.dev");
        listing = createListing(landlord, 2);
        LocalDate checkIn = LocalDate.now().plusDays(10);
        booking = createBooking(tenant, listing, checkIn, checkIn.plusDays(2));
    }

    @Test
    void tenantCanCancelTheirOwnBooking() throws Exception {
        cancel(tenant, false, ROLE_TENANT).andExpect(status().isOk());
        assertThat(bookingRepository.findById(booking.getId())).isEmpty();
    }

    @Test
    void tenantCannotCancelSomeoneElsesBooking() throws Exception {
        cancel(otherTenant, false, ROLE_TENANT).andExpect(status().isBadRequest());
        assertThat(bookingRepository.findById(booking.getId())).isPresent();
    }

    @Test
    void landlordCanCancelAReservationOnTheirListing() throws Exception {
        cancel(landlord, true, ROLE_TENANT, ROLE_LANDLORD).andExpect(status().isOk());
        assertThat(bookingRepository.findById(booking.getId())).isEmpty();
    }

    @Test
    void landlordCannotCancelAReservationOnAnotherLandlordsListing() throws Exception {
        cancel(otherLandlord, true, ROLE_TENANT, ROLE_LANDLORD).andExpect(status().isBadRequest());
        assertThat(bookingRepository.findById(booking.getId())).isPresent();
    }

    @Test
    void landlordCannotDeleteAnotherLandlordsListing() throws Exception {
        bookingRepository.deleteAllInBatch();

        mockMvc.perform(delete("/api/landlord-listing/delete")
                        .param("publicId", listing.getPublicId().toString())
                        .with(loginAs(otherLandlord, ROLE_TENANT, ROLE_LANDLORD))
                        .with(csrf()))
                .andExpect(status().is4xxClientError());

        assertThat(listingRepository.findByPublicId(listing.getPublicId())).isPresent();
    }

    @Test
    void tenantWithoutLandlordRoleCannotListReservations() throws Exception {
        mockMvc.perform(get("/api/booking/get-booked-listing-for-landlord")
                        .with(loginAs(tenant, ROLE_TENANT)))
                .andExpect(status().isForbidden());
    }

    private ResultActions cancel(User user, boolean byLandlord, String... roles) throws Exception {
        return mockMvc.perform(delete("/api/booking/cancel")
                .param("bookingPublicId", booking.getPublicId().toString())
                .param("listingPublicId", listing.getPublicId().toString())
                .param("byLandlord", String.valueOf(byLandlord))
                .with(loginAs(user, roles))
                .with(csrf()));
    }
}
