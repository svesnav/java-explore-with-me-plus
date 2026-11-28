package ru.practicum.ewm.request;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.util.List;

@Validated
@RestController
@RequestMapping(path = "/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class PrivateEventRequestController {
	private final RequestService requestService;

	@GetMapping
	public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
														  @PathVariable Long eventId) {
		return requestService.getEventRequests(userId, eventId);
	}

	@PatchMapping
	public EventRequestStatusUpdateResult changeStatus(
			@PathVariable Long userId,
			@PathVariable Long eventId,
			@Valid @RequestBody EventRequestStatusUpdateRequest update) {
		return requestService.changeStatus(userId, eventId, update);
	}
}