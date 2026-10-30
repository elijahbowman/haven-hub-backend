package com.example.havenhub.booking.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BookedDateDTO(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
