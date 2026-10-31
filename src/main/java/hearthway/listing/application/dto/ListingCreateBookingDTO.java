package hearthway.listing.application.dto;

import hearthway.listing.application.dto.vo.PriceVO;

import java.util.UUID;

public record ListingCreateBookingDTO(
        UUID listingPublicId, PriceVO price, int guests, UUID landlordPublicId) {
}
