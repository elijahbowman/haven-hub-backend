package com.example.havenhub.listing.application.dto;

import com.example.havenhub.listing.application.dto.vo.PriceVO;

import java.util.UUID;

public record ListingCreateBookingDTO(
        UUID listingPublicId, PriceVO price) {
}
