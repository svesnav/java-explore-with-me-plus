package ru.practicum.ewm.request;

import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface RequestService {

	ParticipationRequestDto create(Long userId, Long eventId);

	List<ParticipationRequestDto> getUserRequests(Long userId);

	ParticipationRequestDto cancel(Long userId, Long requestId);

	List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

	EventRequestStatusUpdateResult changeStatus(Long userId, Long eventId,
												EventRequestStatusUpdateRequest update);

	Map<Long, Long> getConfirmedCounts(List<Long> eventIds);
}
