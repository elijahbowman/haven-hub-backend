package com.example.havenhub.listing.application.dto;

import com.example.havenhub.booking.application.dto.BookedDateDTO;
import com.example.havenhub.listing.application.dto.sub.ListingInfoDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SearchDTO(@Valid BookedDateDTO dates,
                        @Valid ListingInfoDTO infos,
                        @NotEmpty String location) {
}
