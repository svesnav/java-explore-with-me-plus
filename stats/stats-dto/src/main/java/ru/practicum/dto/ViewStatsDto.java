package ru.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// статистика просмотров для ответа GET /stats
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewStatsDto {

    private String app;

    private String uri;

    // сколько раз смотрели
    private Long hits;
}
