package hearthway.listing.application.dto.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record PriceVO(@Positive(message = "Price must be positive")
                      @Max(value = 1_000_000, message = "Price can't exceed 1,000,000") int value) {
}
