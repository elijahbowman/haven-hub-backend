package hearthway.listing.application.dto.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DescriptionVO(@NotBlank(message = "Description must be present")
                            @Size(max = 9999, message = "Description can't exceed 9999 characters") String value) {
}
