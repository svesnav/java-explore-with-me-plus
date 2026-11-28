package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record NewCompilationDto(
        @NotBlank @Size(max = 50) String title,
        Boolean pinned,
        Set<@NotNull @Positive Long> events
) {
}
