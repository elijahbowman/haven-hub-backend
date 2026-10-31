package hearthway.booking.application;

import hearthway.booking.application.dto.BookedDateDTO;
import hearthway.booking.application.dto.BookedListingDTO;
import hearthway.booking.application.dto.NewBookingDTO;
import hearthway.booking.domain.Booking;
import hearthway.booking.mapper.BookingMapper;
import hearthway.booking.repository.BookingRepository;
import hearthway.infrastructure.config.SecurityUtils;
import hearthway.listing.application.LandlordService;
import hearthway.listing.application.dto.DisplayCardListingDTO;
import hearthway.listing.application.dto.ListingCreateBookingDTO;
import hearthway.listing.application.dto.vo.PriceVO;
import hearthway.sharedkernel.service.State;
import hearthway.user.application.UserService;
import hearthway.user.application.dto.ReadUserDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final UserService userService;
    private final LandlordService landlordService;
    private final Clock clock;

    static final int MAX_NIGHTS = 365;

    public BookingService(BookingRepository bookingRepository, BookingMapper bookingMapper,
                          UserService userService, LandlordService landlordService, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.userService = userService;
        this.landlordService = landlordService;
        this.clock = clock;
    }

    /**
     * Books the nights [startDate, endDate) of a listing for the current user.
     * Invalid requests return an error state; a clash with another booking throws
     * {@link BookingConflictException}.
     * <p>
     * The listing row is locked first, so concurrent bookings of the same listing
     * run one at a time and the availability check sees the previous booking.
     * Without the lock, concurrent inserts can deadlock while checking the
     * booking_no_overlap constraint, which remains as a safety net.
     */
    @Transactional
    public State<Void, String> create(NewBookingDTO newBookingDTO) {
        Optional<ListingCreateBookingDTO> listingOpt = landlordService.lockForBooking(newBookingDTO.listingPublicId());

        if (listingOpt.isEmpty()) {
            return State.<Void, String>builder().forError("Listing not found");
        }

        ListingCreateBookingDTO listing = listingOpt.get();
        ReadUserDTO connectedUser = userService.getAuthenticatedUserFromSecurityContext();
        int travelers = newBookingDTO.numberOfTravelers() == null ? 1 : newBookingDTO.numberOfTravelers();

        Optional<String> validationError = validateBooking(newBookingDTO, listing, connectedUser, travelers);
        if (validationError.isPresent()) {
            return State.<Void, String>builder().forError(validationError.get());
        }

        boolean alreadyBooked = bookingRepository.bookingExistsAtInterval(newBookingDTO.startDate(), newBookingDTO.endDate(), newBookingDTO.listingPublicId());
        if (alreadyBooked) {
            throw new BookingConflictException("These dates are no longer available");
        }

        Booking booking = bookingMapper.newBookingToBooking(newBookingDTO);
        booking.setFkListing(listing.listingPublicId());
        booking.setFkTenant(connectedUser.publicId());
        booking.setNumberOfTravelers(travelers);

        long numberOfNights = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
        booking.setTotalPrice(Math.toIntExact(numberOfNights * listing.price().value()));

        // Flush so a constraint violation surfaces here rather than at commit.
        bookingRepository.saveAndFlush(booking);

        return State.<Void, String>builder().forSuccess();
    }

    private Optional<String> validateBooking(NewBookingDTO newBooking, ListingCreateBookingDTO listing,
                                             ReadUserDTO tenant, int travelers) {
        // Accept "yesterday" in UTC so guests west of UTC can still book their today.
        LocalDate earliestCheckIn = LocalDate.now(clock).minusDays(1);

        if (!newBooking.endDate().isAfter(newBooking.startDate())) {
            return Optional.of("Check-out must be after check-in");
        }
        if (newBooking.startDate().isBefore(earliestCheckIn)) {
            return Optional.of("Check-in can't be in the past");
        }
        if (ChronoUnit.DAYS.between(newBooking.startDate(), newBooking.endDate()) > MAX_NIGHTS) {
            return Optional.of("A stay can't exceed " + MAX_NIGHTS + " nights");
        }
        // Listings created before capacity was validated may have 0 guests; skip the check for them.
        if (listing.guests() > 0 && travelers > listing.guests()) {
            return Optional.of("This listing hosts at most " + listing.guests() + " guests");
        }
        if (tenant.publicId().equals(listing.landlordPublicId())) {
            return Optional.of("You can't book your own listing");
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<BookedDateDTO> checkAvailability(UUID publicId) {
        return bookingRepository.findAllByFkListing(publicId)
                .stream().map(bookingMapper::bookingToCheckAvailability).toList();
    }

    @Transactional(readOnly = true)
    public List<BookedListingDTO> getBookedListing() {
        ReadUserDTO connectedUser = userService.getAuthenticatedUserFromSecurityContext();
        List<Booking> allBookings = bookingRepository.findAllByFkTenant(connectedUser.publicId());
        List<UUID> allListingPublicIDs = allBookings.stream().map(Booking::getFkListing).toList();
        List<DisplayCardListingDTO> allListings = landlordService.getCardDisplayByListingPublicId(allListingPublicIDs);
        return mapBookingToBookedListing(allBookings, allListings);
    }

    private List<BookedListingDTO> mapBookingToBookedListing(List<Booking> allBookings, List<DisplayCardListingDTO> allListings) {
        return allBookings.stream().map(booking -> {
            DisplayCardListingDTO displayCardListingDTO = allListings
                    .stream()
                    .filter(listing -> listing.publicId().equals(booking.getFkListing()))
                    .findFirst()
                    .orElseThrow();
            BookedDateDTO dates = bookingMapper.bookingToCheckAvailability(booking);
            return new BookedListingDTO(displayCardListingDTO.cover(),
                    displayCardListingDTO.location(),
                    dates, new PriceVO(booking.getTotalPrice()),
                    booking.getPublicId(), displayCardListingDTO.publicId());
        }).toList();
    }

    @Transactional
    public State<UUID, String> cancel(UUID bookingPublicId, UUID listingPublicId, boolean byLandlord) {
        ReadUserDTO connectedUser = userService.getAuthenticatedUserFromSecurityContext();
        int deleteSuccess = 0;

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(SecurityUtils.ROLE_LANDLORD)
                && byLandlord) {
            deleteSuccess = handleDeletionForLandlord(bookingPublicId, listingPublicId, connectedUser, deleteSuccess);
        } else {
            deleteSuccess = bookingRepository.deleteBookingByFkTenantAndPublicId(connectedUser.publicId(), bookingPublicId);
        }

        if (deleteSuccess >= 1) {
            return State.<UUID, String>builder().forSuccess(bookingPublicId);
        } else {
            return State.<UUID, String>builder().forError("Booking not found");
        }
    }

    private int handleDeletionForLandlord(UUID bookingPublicId, UUID listingPublicId, ReadUserDTO connectedUser, int deleteSuccess) {
        Optional<DisplayCardListingDTO> listingVerificationOpt = landlordService.getByPublicIdAndLandlordPublicId(listingPublicId, connectedUser.publicId());
        if (listingVerificationOpt.isPresent()) {
            deleteSuccess = bookingRepository.deleteBookingByPublicIdAndFkListing(bookingPublicId, listingVerificationOpt.get().publicId());
        }
        return deleteSuccess;
    }

    @Transactional(readOnly = true)
    public List<BookedListingDTO> getBookedListingForLandlord() {
        ReadUserDTO connectedUser = userService.getAuthenticatedUserFromSecurityContext();
        List<DisplayCardListingDTO> allProperties = landlordService.getAllProperties(connectedUser);
        List<UUID> allPropertyPublicIds = allProperties.stream().map(DisplayCardListingDTO::publicId).toList();
        List<Booking> allBookings = bookingRepository.findAllByFkListingIn(allPropertyPublicIds);
        return mapBookingToBookedListing(allBookings, allProperties);
    }
}
