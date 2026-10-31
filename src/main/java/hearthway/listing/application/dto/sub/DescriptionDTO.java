package hearthway.listing.application.dto.sub;

import hearthway.listing.application.dto.vo.DescriptionVO;
import hearthway.listing.application.dto.vo.TitleVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record DescriptionDTO(
        @NotNull @Valid TitleVO title,
        @NotNull @Valid DescriptionVO description
        ) {
}
