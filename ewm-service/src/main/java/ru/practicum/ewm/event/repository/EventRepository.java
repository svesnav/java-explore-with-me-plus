package ru.practicum.ewm.event.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @EntityGraph(attributePaths = {"category", "initiator"})
    List<Event> findAllByIdInOrderByIdAsc(Set<Long> ids);

    @EntityGraph(attributePaths = {"category", "initiator"})
    Page<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "initiator"})
    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    @Override
    @EntityGraph(attributePaths = {"category", "initiator"})
    Page<Event> findAll(Specification<Event> specification, Pageable pageable);

    default Page<Event> findPublished(EventState state, String text, List<Long> categories,
                                     Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                     Pageable pageable) {
        return findAll((event, query, builder) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(builder.equal(event.get("state"), state));
            if (categories != null && !categories.isEmpty()) {
                filters.add(event.get("category").get("id").in(categories));
            }
            if (paid != null) {
                filters.add(builder.equal(event.get("paid"), paid));
            }
            if (rangeStart != null) {
                filters.add(builder.greaterThanOrEqualTo(event.get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                filters.add(builder.lessThanOrEqualTo(event.get("eventDate"), rangeEnd));
            }
            if (text != null && !text.isEmpty()) {
                String escaped = text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
                var pattern = builder.lower(builder.literal("%" + escaped + "%"));
                filters.add(builder.or(
                        builder.like(builder.lower(event.get("annotation")), pattern, '!'),
                        builder.like(builder.lower(event.get("description")), pattern, '!')));
            }
            return builder.and(filters.toArray(Predicate[]::new));
        }, pageable);
    }

    @EntityGraph(attributePaths = {"category", "initiator"})
    @Query("""
            SELECT e FROM Event e
            WHERE (:users IS NULL OR e.initiator.id IN :users)
              AND (:states IS NULL OR e.state IN :states)
              AND (:categories IS NULL OR e.category.id IN :categories)
              AND (CAST(:rangeStart AS timestamp) IS NULL OR e.eventDate >= :rangeStart)
              AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)
            """)
    Page<Event> findForAdmin(@Param("users") List<Long> users,
                             @Param("states") List<EventState> states,
                             @Param("categories") List<Long> categories,
                             @Param("rangeStart") LocalDateTime rangeStart,
                             @Param("rangeEnd") LocalDateTime rangeEnd,
                             Pageable pageable);
}
