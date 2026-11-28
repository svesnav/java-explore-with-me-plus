package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.Category;
import ru.practicum.ewm.category.CategoryRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.stats.StatsFacade;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final int STATS_BATCH_SIZE = 100;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper mapper;
    private final StatsFacade statsFacade;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found: " + dto.getCategory()));
        Event event = mapper.toEntity(dto, category, user);
        return mapper.toFullDto(eventRepository.save(event), 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        Page<Event> page = eventRepository.findAllByInitiatorId(userId, pageable);
        Map<String, Long> views = statsFacade.getViews(
                page.getContent().stream().map(e -> "/events/" + e.getId()).toList()
        );
        return page.getContent().stream()
                .map(e -> mapper.toShortDto(e, views.getOrDefault("/events/" + e.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        Long views = statsFacade.getViews(List.of("/events/" + eventId))
                .getOrDefault("/events/" + eventId, 0L);
        return mapper.toFullDto(event, views);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Published event cannot be edited");
        }
        if (request.getEventDate() != null && request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }
        applyUserUpdate(event, request);
        return mapper.toFullDto(eventRepository.save(event), 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            Boolean onlyAvailable, String sort, Pageable pageable) {
        validateRange(rangeStart, rangeEnd);
        var filters = EventRepository.publishedEvents(text, categories, paid, rangeStart, rangeEnd,
                Boolean.TRUE.equals(onlyAvailable));
        if (!"VIEWS".equals(sort)) {
            Page<Event> page = eventRepository.findAll(filters, pageable);
            Map<String, Long> views = statsFacade.getViews(
                    page.getContent().stream().map(e -> "/events/" + e.getId()).toList()
            );
            return page.getContent().stream()
                    .map(e -> mapper.toShortDto(e, views.getOrDefault("/events/" + e.getId(), 0L)))
                    .toList();
        }

        List<Long> matchingIds = eventRepository.findBy(filters, query -> query
                .as(EventRepository.EventId.class).project("id").all()).stream()
                .map(EventRepository.EventId::getId)
                .toList();
        Map<String, Long> views = getPublicViews(matchingIds);
        List<Long> ids = matchingIds.stream()
                .sorted(Comparator.<Long>comparingLong(id -> views.getOrDefault("/events/" + id, 0L))
                        .reversed().thenComparingLong(Long::longValue))
                .skip(pageable.getOffset()).limit(pageable.getPageSize()).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Event> events = eventRepository.findAllByIdInOrderByIdAsc(new HashSet<>(ids)).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));
        return ids.stream().filter(events::containsKey)
                .map(id -> mapper.toShortDto(events.get(id), views.getOrDefault("/events/" + id, 0L)))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto getPublicEvent(Long eventId, String ip, String uri) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> e.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Published event not found: " + eventId));
        statsFacade.hit(uri, ip);
        Long views = statsFacade.getViews(List.of(uri)).getOrDefault(uri, 0L);
        return mapper.toFullDto(event, views);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> searchAdmin(List<Long> users, List<EventState> states,
                                          List<Long> categories, LocalDateTime rangeStart,
                                          LocalDateTime rangeEnd, Pageable pageable) {
        validateRange(rangeStart, rangeEnd);
        Page<Event> page = eventRepository.findForAdmin(users, states, categories,
                rangeStart, rangeEnd, pageable);
        Map<String, Long> views = statsFacade.getViews(
                page.getContent().stream().map(e -> "/events/" + e.getId()).toList()
        );
        return page.getContent().stream()
                .map(e -> mapper.toFullDto(e, views.getOrDefault("/events/" + e.getId(), 0L)))
                .toList();
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("rangeStart must not be after rangeEnd");
        }
    }

    private Map<String, Long> getPublicViews(List<Long> ids) {
        Map<String, Long> views = new HashMap<>();
        for (int from = 0; from < ids.size(); from += STATS_BATCH_SIZE) {
            List<String> uris = ids.subList(from, Math.min(from + STATS_BATCH_SIZE, ids.size())).stream()
                    .map(id -> "/events/" + id)
                    .toList();
            views.putAll(statsFacade.getViews(uris));
        }
        return views;
    }

    @Override
    @Transactional
    public EventFullDto moderate(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
        applyAdminUpdate(event, request);
        return mapper.toFullDto(eventRepository.save(event), 0L);
    }

    private void applyUserUpdate(Event event, UpdateEventUserRequest r) {
        if (r.getTitle() != null) event.setTitle(r.getTitle());
        if (r.getAnnotation() != null) event.setAnnotation(r.getAnnotation());
        if (r.getDescription() != null) event.setDescription(r.getDescription());
        if (r.getCategoryId() != null) {
            Category category = categoryRepository.findById(r.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (r.getEventDate() != null) event.setEventDate(r.getEventDate());
        if (r.getLocation() != null) event.setLocation(r.getLocation());
        if (r.getPaid() != null) event.setPaid(r.getPaid());
        if (r.getParticipantLimit() != null) event.setParticipantLimit(r.getParticipantLimit());
        if (r.getRequestModeration() != null) event.setRequestModeration(r.getRequestModeration());
        if (r.getStateAction() != null) {
            if (r.getStateAction() == UpdateEventUserRequest.StateAction.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else {
                event.setState(EventState.CANCELED);
            }
        }
    }

    private void applyAdminUpdate(Event event, UpdateEventAdminRequest r) {
        if (r.getTitle() != null) event.setTitle(r.getTitle());
        if (r.getAnnotation() != null) event.setAnnotation(r.getAnnotation());
        if (r.getDescription() != null) event.setDescription(r.getDescription());
        if (r.getCategoryId() != null) {
            Category category = categoryRepository.findById(r.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (r.getEventDate() != null) event.setEventDate(r.getEventDate());
        if (r.getLocation() != null) event.setLocation(r.getLocation());
        if (r.getPaid() != null) event.setPaid(r.getPaid());
        if (r.getParticipantLimit() != null) event.setParticipantLimit(r.getParticipantLimit());
        if (r.getRequestModeration() != null) event.setRequestModeration(r.getRequestModeration());
        if (r.getStateAction() != null) {
            if (r.getStateAction() == UpdateEventAdminRequest.StateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Only PENDING events can be published");
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Event date must be at least 1 hour from publication");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Published event cannot be rejected");
                }
                event.setState(EventState.CANCELED);
            }
        }
    }
}
