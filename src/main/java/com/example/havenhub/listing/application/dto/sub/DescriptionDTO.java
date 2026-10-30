package com.example.havenhub.listing.application.dto.sub;

import com.example.havenhub.listing.application.dto.vo.DescriptionVO;
import com.example.havenhub.listing.application.dto.vo.TitleVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record DescriptionDTO(
        @NotNull @Valid TitleVO title,
        @NotNull @Valid DescriptionVO description
        ) {
}
