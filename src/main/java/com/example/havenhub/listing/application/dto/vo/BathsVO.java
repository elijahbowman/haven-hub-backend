package com.example.havenhub.listing.application.dto.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Shared by listings and search; in a search 0 means "any". Listing-specific
// minimums are checked in SaveListingDTO.
public record BathsVO(@Min(value = 0, message = "Baths can't be negative")
                      @Max(value = 50, message = "Baths can't exceed 50") int value) {
}
