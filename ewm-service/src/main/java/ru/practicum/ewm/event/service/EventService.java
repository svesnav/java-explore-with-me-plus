package ru.practicum.ewm.event.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventFullDto create(Long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(Long userId, Pageable pageable);

    EventFullDto getUserEvent(Long userId, Long eventId);

    EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                     LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                     Boolean onlyAvailable, String sort, Pageable pageable);

    EventFullDto getPublicEvent(Long eventId, String ip, String uri);

    List<EventFullDto> searchAdmin(List<Long> users, List<EventState> states,
                                   List<Long> categories, LocalDateTime rangeStart,
                                   LocalDateTime rangeEnd, Pageable pageable);

    EventFullDto moderate(Long eventId, UpdateEventAdminRequest request);
}
