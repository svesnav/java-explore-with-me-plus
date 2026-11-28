package ru.practicum.ewm.event.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.category.Category;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.request.ParticipationRequestRepository;
import ru.practicum.ewm.request.RequestStatus;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final ParticipationRequestRepository requestRepository;

    public Event toEntity(NewEventDto dto, Category category, User initiator) {
        return Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .category(category)
                .initiator(initiator)
                .location(dto.getLocation())
                .eventDate(dto.getEventDate())
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .paid(dto.getPaid())
                .participantLimit(dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration())
                .build();
    }

    public EventShortDto toShortDto(Event event, Long views) {
        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .category(toCategoryDto(event.getCategory()))
                .initiator(toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .views(views)
                .confirmedRequests(confirmedCount(event))
                .build();
    }

    public EventFullDto toFullDto(Event event, Long views) {
        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .category(toCategoryDto(event.getCategory()))
                .initiator(toUserShortDto(event.getInitiator()))
                .location(event.getLocation())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .eventDate(event.getEventDate())
                .views(views)
                .confirmedRequests(confirmedCount(event))
                .build();
    }

    private CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder().id(category.getId()).name(category.getName()).build();
    }

    private UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder().id(user.getId()).name(user.getName()).build();
    }

    private Long confirmedCount(Event event) {
        return requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
    }
}
