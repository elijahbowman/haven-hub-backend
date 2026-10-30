package com.example.havenhub.listing.repository;

import com.example.havenhub.listing.domain.BookingCategory;
import com.example.havenhub.listing.domain.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
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
                                  OffsetDateTime startDate, OffsetDateTime endDate);
}
