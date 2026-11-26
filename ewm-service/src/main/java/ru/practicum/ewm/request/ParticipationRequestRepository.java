package ru.practicum.ewm.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

	boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

	List<ParticipationRequest> findAllByRequesterId(Long requesterId);

	Optional<ParticipationRequest> findByIdAndRequesterId(Long id, Long requesterId);

	List<ParticipationRequest> findAllByEventId(Long eventId);

	List<ParticipationRequest> findAllByIdInAndEventId(List<Long> ids, Long eventId);

	List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

	long countByEventIdAndStatus(Long eventId, RequestStatus status);

	@Query("select r.event.id as eventId, count(r) as total " +
			"from ParticipationRequest r " +
			"where r.event.id in :eventIds and r.status = :status " +
			"group by r.event.id")
	List<EventConfirmedCount> countByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds,
													   @Param("status") RequestStatus status);
}