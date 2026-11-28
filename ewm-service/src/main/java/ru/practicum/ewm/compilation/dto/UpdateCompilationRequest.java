package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateCompilationRequest(
        @Size(max = 50) @Pattern(regexp = "(?s).*\\P{javaWhitespace}.*") String title,
        Boolean pinned,
        Set<@NotNull @Positive Long> events
) {
}
