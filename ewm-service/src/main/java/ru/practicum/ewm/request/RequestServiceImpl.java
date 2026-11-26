package ru.practicum.ewm.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
	private final ParticipationRequestRepository requestRepository;
	private final EventRepository eventRepository;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public ParticipationRequestDto create(Long userId, Long eventId) {
		User requester = findUser(userId);
		Event event = findEvent(eventId);

		if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
			throw new ConflictException("Request from user with id=" + userId
					+ " for event with id=" + eventId + " already exists");
		}
		if (event.getInitiator().getId().equals(userId)) {
			throw new ConflictException("Event initiator cannot participate in own event");
		}
		if (event.getState() != EventState.PUBLISHED) {
			throw new ConflictException("Cannot participate in unpublished event");
		}
		int limit = event.getParticipantLimit();
		if (limit > 0 && requestRepository.countByEventIdAndStatus(eventId,
				RequestStatus.CONFIRMED) >= limit) {
			throw new ConflictException("The participant limit has been reached");
		}

		RequestStatus status = RequestStatus.PENDING;
		if (limit == 0 || !event.getRequestModeration()) {
			status = RequestStatus.CONFIRMED;
		}

		ParticipationRequest saved = requestRepository.save(
				RequestMapper.toEntity(event, requester, status));
		return RequestMapper.toDto(saved);
	}

	@Override
	public List<ParticipationRequestDto> getUserRequests(Long userId) {
		findUser(userId);
		return requestRepository.findAllByRequesterId(userId).stream()
				.map(RequestMapper::toDto)
				.toList();
	}

	@Override
	@Transactional
	public ParticipationRequestDto cancel(Long userId, Long requestId) {
		ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
				.orElseThrow(() -> new NotFoundException(
						"Request with id=" + requestId + " was not found"));
		request.setStatus(RequestStatus.CANCELED);
		return RequestMapper.toDto(requestRepository.save(request));
	}

	@Override
	public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
		findEventByInitiator(userId, eventId);
		return requestRepository.findAllByEventId(eventId).stream()
				.map(RequestMapper::toDto)
				.toList();
	}

	@Override
	@Transactional
	public EventRequestStatusUpdateResult changeStatus(Long userId, Long eventId,
													   EventRequestStatusUpdateRequest update) {
		Event event = findEventByInitiator(userId, eventId);

		if (update.getStatus() != RequestStatus.CONFIRMED
				&& update.getStatus() != RequestStatus.REJECTED) {
			throw new ConflictException("Status must be CONFIRMED or REJECTED");
		}

		List<ParticipationRequest> requests =
				requestRepository.findAllByIdInAndEventId(update.getRequestIds(), eventId);
		if (requests.size() != update.getRequestIds().stream().distinct().count()) {
			throw new NotFoundException("Some requests were not found for event with id=" + eventId);
		}

		for (ParticipationRequest request : requests) {
			if (request.getStatus() != RequestStatus.PENDING) {
				throw new ConflictException("Request with id=" + request.getId()
						+ " must have status PENDING");
			}
		}

		List<ParticipationRequestDto> confirmed = new ArrayList<>();
		List<ParticipationRequestDto> rejected = new ArrayList<>();

		if (update.getStatus() == RequestStatus.REJECTED) {
			for (ParticipationRequest request : requests) {
				request.setStatus(RequestStatus.REJECTED);
				rejected.add(RequestMapper.toDto(request));
			}
			requestRepository.saveAll(requests);
			return EventRequestStatusUpdateResult.builder()
					.confirmedRequests(confirmed)
					.rejectedRequests(rejected)
					.build();
		}

		int limit = event.getParticipantLimit();
		long confirmedCount = requestRepository.countByEventIdAndStatus(eventId,
				RequestStatus.CONFIRMED);
		if (limit > 0 && confirmedCount >= limit) {
			throw new ConflictException("The participant limit has been reached");
		}

		for (ParticipationRequest request : requests) {
			if (limit > 0 && confirmedCount >= limit) {
				break;
			}
			request.setStatus(RequestStatus.CONFIRMED);
			confirmedCount++;
			confirmed.add(RequestMapper.toDto(request));
		}
		requestRepository.saveAll(requests);

		if (limit > 0 && confirmedCount >= limit) {
			List<ParticipationRequest> pending =
					requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);
			for (ParticipationRequest request : pending) {
				request.setStatus(RequestStatus.REJECTED);
				rejected.add(RequestMapper.toDto(request));
			}
			requestRepository.saveAll(pending);
		}

		return EventRequestStatusUpdateResult.builder()
				.confirmedRequests(confirmed)
				.rejectedRequests(rejected)
				.build();
	}

	@Override
	public Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
		if (eventIds == null || eventIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED)
				.stream()
				.collect(Collectors.toMap(EventConfirmedCount::getEventId,
						EventConfirmedCount::getTotal));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException(
						"User with id=" + userId + " was not found"));
	}

	private Event findEvent(Long eventId) {
		return eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException(
						"Event with id=" + eventId + " was not found"));
	}

	private Event findEventByInitiator(Long userId, Long eventId) {
		Event event = findEvent(eventId);
		if (!event.getInitiator().getId().equals(userId)) {
			throw new NotFoundException("Event with id=" + eventId + " was not found");
		}
		return event;
	}
}
