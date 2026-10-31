package hearthway.listing;

import hearthway.listing.domain.Listing;
import hearthway.support.AbstractIntegrationTest;
import hearthway.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchIntegrationTest extends AbstractIntegrationTest {

    private final LocalDate checkIn = LocalDate.now().plusDays(20);
    private final LocalDate checkOut = checkIn.plusDays(3);

    @Test
    void treatsCapacityAsAMinimum() throws Exception {
        User landlord = createUser("landlord@test.dev");
        Listing sleepsFour = createListing(landlord, 4);
        createListing(landlord, 1);

        search(2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].publicId").value(sleepsFour.getPublicId().toString()));
    }

    @Test
    void excludesBookedListingsAndCountsOnlyAvailableOnes() throws Exception {
        User landlord = createUser("landlord@test.dev");
        User tenant = createUser("tenant@test.dev");
        Listing booked = createListing(landlord, 2);
        Listing free = createListing(landlord, 2);
        Listing freeFromCheckIn = createListing(landlord, 2);

        createBooking(tenant, booked, checkIn.plusDays(1), checkIn.plusDays(2));
        // Checks out on the requested check-in day, so it doesn't conflict.
        createBooking(tenant, freeFromCheckIn, checkIn.minusDays(2), checkIn);

        search(1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.publicId == '%s')]", booked.getPublicId()).isEmpty())
                .andExpect(jsonPath("$.content[?(@.publicId == '%s')]", free.getPublicId()).isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.publicId == '%s')]", freeFromCheckIn.getPublicId()).isNotEmpty());
    }

    private ResultActions search(int guests) throws Exception {
        String body = """
                {
                  "location": "FRA",
                  "dates": {"startDate": "%s", "endDate": "%s"},
                  "infos": {
                    "guests": {"value": %d},
                    "bedrooms": {"value": 0},
                    "beds": {"value": 0},
                    "baths": {"value": 0}
                  }
                }
                """.formatted(checkIn, checkOut, guests);

        return mockMvc.perform(post("/api/tenant-listing/search")
                .param("page", "0")
                .param("size", "10")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
