package hearthway.listing.application.dto.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TitleVO(@NotBlank(message = "Title must be present")
                      @Size(max = 256, message = "Title can't exceed 256 characters") String value) {
}
