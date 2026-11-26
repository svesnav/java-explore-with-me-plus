package ru.practicum.ewm.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.stats.StatsFacade;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final StatsFacade statsFacade;

    @Transactional
    public CompilationDto create(NewCompilationDto request) {
        Compilation compilation = new Compilation();
        compilation.setTitle(request.title());
        compilation.setPinned(Boolean.TRUE.equals(request.pinned()));
        compilation.getEvents().addAll(findEvents(request.events()));
        return toDtos(List.of(compilationRepository.save(compilation))).getFirst();
    }

    @Transactional
    public CompilationDto update(Long id, UpdateCompilationRequest request) {
        Compilation compilation = findCompilation(id);
        if (request.title() != null) {
            compilation.setTitle(request.title());
        }
        if (request.pinned() != null) {
            compilation.setPinned(request.pinned());
        }
        if (request.events() != null) {
            List<Event> events = findEvents(request.events());
            compilation.getEvents().clear();
            compilation.getEvents().addAll(events);
        }
        return toDtos(List.of(compilation)).getFirst();
    }

    @Transactional
    public void delete(Long id) {
        compilationRepository.delete(findCompilation(id));
    }

    public CompilationDto getById(Long id) {
        return toDtos(List.of(findCompilation(id))).getFirst();
    }

    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        List<Long> ids = compilationRepository.findPageIds(pinned, from, size);
        if (ids.isEmpty()) {
            return List.of();
        }
        return toDtos(compilationRepository.findAllByIdInOrderByIdAsc(ids));
    }

    private Compilation findCompilation(Long id) {
        return compilationRepository.findWithEventsById(id)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + id + " was not found"));
    }

    private List<Event> findEvents(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Event> events = eventRepository.findAllByIdInOrderByIdAsc(ids);
        if (events.size() != ids.size()) {
            throw new NotFoundException("Some compilation events were not found: " + ids);
        }
        return events;
    }

    private List<CompilationDto> toDtos(List<Compilation> compilations) {
        List<String> uris = compilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream())
                .map(event -> "/events/" + event.getId())
                .distinct()
                .toList();
        Map<String, Long> views = statsFacade.getViews(uris);
        return compilations.stream()
                .map(compilation -> new CompilationDto(
                        compilation.getId(), compilation.getTitle(), compilation.isPinned(),
                        compilation.getEvents().stream()
                                .map(event -> eventMapper.toShortDto(
                                        event, views.getOrDefault("/events/" + event.getId(), 0L)))
                                .toList()))
                .toList();
    }
}
