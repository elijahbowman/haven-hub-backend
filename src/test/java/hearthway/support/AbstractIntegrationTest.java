package hearthway.support;

import hearthway.booking.domain.Booking;
import hearthway.booking.repository.BookingRepository;
import hearthway.listing.domain.BookingCategory;
import hearthway.listing.domain.Listing;
import hearthway.listing.domain.ListingPicture;
import hearthway.listing.repository.ListingPictureRepository;
import hearthway.listing.repository.ListingRepository;
import hearthway.user.application.Auth0Service;
import hearthway.user.domain.User;
import hearthway.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

/**
 * Boots the full application against a Testcontainers PostgreSQL. Subclasses share
 * one Spring context (and one database container); each test cleans up after itself.
 */
@SpringBootTest(properties = {
        "AUTH0_CLIENT_ID=test-client",
        "AUTH0_CLIENT_SECRET=test-secret"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    protected static final String ROLE_LANDLORD = "ROLE_LANDLORD";
    protected static final String ROLE_TENANT = "ROLE_TENANT";
    protected static final int NIGHTLY_PRICE = 100;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ListingRepository listingRepository;

    @Autowired
    protected ListingPictureRepository listingPictureRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    // Never call the real Auth0 Management API from tests.
    @MockBean
    protected Auth0Service auth0Service;

    @AfterEach
    void cleanDatabase() {
        bookingRepository.deleteAllInBatch();
        listingPictureRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        userRepository.deleteAll();
    }

    protected User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        return userRepository.saveAndFlush(user);
    }

    protected Listing createListing(User landlord, int guests) {
        Listing listing = new Listing();
        listing.setTitle("Test listing");
        listing.setDescription("A place to stay");
        listing.setGuests(guests);
        listing.setBedrooms(1);
        listing.setBeds(1);
        listing.setBathrooms(1);
        listing.setPrice(NIGHTLY_PRICE);
        listing.setBookingCategory(BookingCategory.ROOMS);
        listing.setLocation("FRA");
        listing.setLandlordPublicId(landlord.getPublicId());
        Listing saved = listingRepository.saveAndFlush(listing);

        // Card queries expect a cover picture.
        ListingPicture cover = new ListingPicture();
        cover.setListing(saved);
        cover.setFile(new byte[]{1, 2, 3});
        cover.setFileContentType("image/png");
        cover.setCover(true);
        listingPictureRepository.saveAndFlush(cover);
        return saved;
    }

    protected Booking createBooking(User tenant, Listing listing, LocalDate checkIn, LocalDate checkOut) {
        Booking booking = new Booking();
        booking.setFkTenant(tenant.getPublicId());
        booking.setFkListing(listing.getPublicId());
        booking.setStartDate(checkIn);
        booking.setEndDate(checkOut);
        booking.setNumberOfTravelers(1);
        booking.setTotalPrice(NIGHTLY_PRICE);
        return bookingRepository.saveAndFlush(booking);
    }

    /** A logged-in browser session (oauth2Login) for MockMvc requests. */
    protected static RequestPostProcessor loginAs(User user, String... roles) {
        return oidcLogin()
                .idToken(token -> token.claim("email", user.getEmail()))
                .authorities(toAuthorities(roles).toArray(GrantedAuthority[]::new));
    }

    /** The same kind of authentication, for calling services directly. */
    protected static Authentication authenticationFor(User user, String... roles) {
        List<GrantedAuthority> authorities = toAuthorities(roles);
        DefaultOAuth2User principal = new DefaultOAuth2User(authorities,
                Map.of("sub", "test|" + user.getEmail(), "email", user.getEmail()), "sub");
        return new OAuth2AuthenticationToken(principal, authorities, "okta");
    }

    private static List<GrantedAuthority> toAuthorities(String... roles) {
        return Arrays.stream(roles)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                .toList();
    }
}
