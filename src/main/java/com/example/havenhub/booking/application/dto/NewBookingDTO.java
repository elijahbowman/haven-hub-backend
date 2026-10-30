package com.example.havenhub.booking.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record NewBookingDTO(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull UUID listingPublicId,
        // Optional until the booking form has a guest picker; defaults to 1.
        @Positive Integer numberOfTravelers) {
}
