package hearthway.booking;

import hearthway.booking.domain.Booking;
import hearthway.listing.domain.Listing;
import hearthway.support.AbstractIntegrationTest;
import hearthway.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingCreationIntegrationTest extends AbstractIntegrationTest {

    private User landlord;
    private User tenant;
    private Listing listing;
    private LocalDate checkIn;

    @BeforeEach
    void setUp() {
        landlord = createUser("landlord@test.dev");
        tenant = createUser("tenant@test.dev");
        listing = createListing(landlord, 2);
        checkIn = LocalDate.now().plusDays(10);
    }

    @Test
    void createsABookingPricedPerNight() throws Exception {
        requestBooking(tenant, checkIn, checkIn.plusDays(3), null)
                .andExpect(status().isOk());

        List<Booking> bookings = bookingRepository.findAllByFkListing(listing.getPublicId());
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getTotalPrice()).isEqualTo(3 * NIGHTLY_PRICE);
        assertThat(bookings.get(0).getNumberOfTravelers()).isEqualTo(1);
        assertThat(bookings.get(0).getFkTenant()).isEqualTo(tenant.getPublicId());
    }

    @Test
    void returnsBookedDatesAsCalendarDays() throws Exception {
        createBooking(tenant, listing, checkIn, checkIn.plusDays(2));

        mockMvc.perform(get("/api/booking/check-availability")
                        .param("listingPublicId", listing.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startDate").value(checkIn.toString()))
                .andExpect(jsonPath("$[0].endDate").value(checkIn.plusDays(2).toString()));
    }

    @Test
    void rejectsOverlappingBookingWithConflict() throws Exception {
        createBooking(createUser("other@test.dev"), listing, checkIn, checkIn.plusDays(3));

        requestBooking(tenant, checkIn.plusDays(2), checkIn.plusDays(5), null)
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findAllByFkListing(listing.getPublicId())).hasSize(1);
    }

    @Test
    void allowsCheckInOnThePreviousGuestsCheckOutDay() throws Exception {
        createBooking(createUser("other@test.dev"), listing, checkIn, checkIn.plusDays(3));

        requestBooking(tenant, checkIn.plusDays(3), checkIn.plusDays(5), null)
                .andExpect(status().isOk());

        assertThat(bookingRepository.findAllByFkListing(listing.getPublicId())).hasSize(2);
    }

    @Test
    void rejectsAZeroNightStay() throws Exception {
        requestBooking(tenant, checkIn, checkIn, null).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCheckOutBeforeCheckIn() throws Exception {
        requestBooking(tenant, checkIn, checkIn.minusDays(2), null).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCheckInInThePast() throws Exception {
        LocalDate lastWeek = LocalDate.now().minusDays(7);
        requestBooking(tenant, lastWeek, lastWeek.plusDays(2), null).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMoreTravelersThanTheListingHosts() throws Exception {
        requestBooking(tenant, checkIn, checkIn.plusDays(2), 3).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsALandlordBookingTheirOwnListing() throws Exception {
        requestBooking(landlord, checkIn, checkIn.plusDays(2), null).andExpect(status().isBadRequest());
    }

    @Test
    void databaseRejectsOverlapsEvenWhenTheApplicationIsBypassed() {
        createBooking(tenant, listing, checkIn, checkIn.plusDays(3));
        User other = createUser("other@test.dev");

        assertThatThrownBy(() -> createBooking(other, listing, checkIn.plusDays(1), checkIn.plusDays(2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ResultActions requestBooking(User user, LocalDate start, LocalDate end, Integer travelers) throws Exception {
        String travelersJson = travelers == null ? "" : ", \"numberOfTravelers\": " + travelers;
        String body = """
                {"startDate": "%s", "endDate": "%s", "listingPublicId": "%s"%s}
                """.formatted(start, end, listing.getPublicId(), travelersJson);

        return mockMvc.perform(post("/api/booking/create")
                .with(loginAs(user, ROLE_TENANT))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
