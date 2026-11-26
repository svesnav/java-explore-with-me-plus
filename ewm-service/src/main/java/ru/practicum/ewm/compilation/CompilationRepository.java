package ru.practicum.ewm.compilation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query(value = """
            SELECT id FROM compilations
            WHERE pinned = COALESCE(CAST(:pinned AS boolean), pinned)
            ORDER BY id
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Long> findPageIds(@Param("pinned") Boolean pinned, @Param("offset") int offset,
                           @Param("size") int size);

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    List<Compilation> findAllByIdInOrderByIdAsc(List<Long> ids);

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    Optional<Compilation> findWithEventsById(Long id);
}
