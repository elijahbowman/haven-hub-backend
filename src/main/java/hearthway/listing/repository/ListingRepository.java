package hearthway.listing.repository;

import hearthway.listing.domain.BookingCategory;
import hearthway.listing.domain.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    @Query("SELECT listing FROM Listing listing LEFT JOIN FETCH listing.pictures picture" +
            " WHERE listing.landlordPublicId = :landlordPublicId AND picture.isCover = true" +
            " ORDER BY listing.id ASC")
    List<Listing> findAllByLandlordPublicIdFetchCoverPicture(UUID landlordPublicId);

    long deleteByPublicIdAndLandlordPublicId(UUID publicId, UUID landlordPublicId);

    @Query("SELECT listing from Listing listing LEFT JOIN FETCH listing.pictures picture" +
            " WHERE picture.isCover = true AND listing.bookingCategory = :bookingCategory" +
            " ORDER BY listing.id ASC")
    Page<Listing> findAllByBookingCategoryWithCoverOnly(Pageable pageable, BookingCategory bookingCategory);

    @Query("SELECT listing from Listing listing LEFT JOIN FETCH listing.pictures picture" +
            " WHERE picture.isCover = true" +
            " ORDER BY listing.id ASC")
    Page<Listing> findAllWithCoverOnly(Pageable pageable);

    Optional<Listing> findByPublicId(UUID publicId);

    /**
     * Loads a listing and locks its row (SELECT ... FOR UPDATE) until the current
     * transaction ends.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT listing FROM Listing listing WHERE listing.publicId = :publicId")
    Optional<Listing> findByPublicIdForUpdate(UUID publicId);

    List<Listing> findAllByPublicIdIn(List<UUID> allListingPublicIDs);

    Optional<Listing> findOneByPublicIdAndLandlordPublicId(UUID listingPublicId, UUID landlordPublicId);

    /**
     * Listings in a location that fit at least the requested capacity and have no
     * booking overlapping [startDate, endDate). Bookings are half-open intervals, so
     * a stay ending on startDate does not conflict. The availability check runs in
     * SQL so that page contents and totals are correct.
     */
    @Query("SELECT listing FROM Listing listing" +
            " WHERE listing.location = :location" +
            " AND listing.bathrooms >= :bathrooms" +
            " AND listing.bedrooms >= :bedrooms" +
            " AND listing.guests >= :guests" +
            " AND listing.beds >= :beds" +
            " AND NOT EXISTS (SELECT booking.id FROM Booking booking" +
            " WHERE booking.fkListing = listing.publicId" +
            " AND booking.startDate < :endDate AND booking.endDate > :startDate)" +
            " ORDER BY listing.id ASC")
    Page<Listing> searchAvailable(Pageable pageable, String location,
                                  int bathrooms, int bedrooms, int guests, int beds,
                                  LocalDate startDate, LocalDate endDate);
}
