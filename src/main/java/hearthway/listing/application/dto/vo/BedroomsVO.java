package hearthway.listing.application.dto.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Shared by listings and search; in a search 0 means "any". Listing-specific
// minimums are checked in SaveListingDTO.
public record BedroomsVO(@Min(value = 0, message = "Bedrooms can't be negative")
                         @Max(value = 50, message = "Bedrooms can't exceed 50") int value) {
}
