package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// запрос к эндпоинту, который сохраняем через POST /hit
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {

    // название сервиса
    @NotBlank
    private String app;

    // uri
    @NotBlank
    private String uri;

    // ip пользователя
    @NotBlank
    private String ip;

    // когда был запрос
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
