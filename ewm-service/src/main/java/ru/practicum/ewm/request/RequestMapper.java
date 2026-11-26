package ru.practicum.ewm.request;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.user.User;

import java.time.LocalDateTime;

@UtilityClass
public class RequestMapper {

	public ParticipationRequest toEntity(Event event, User requester, RequestStatus status) {
		return ParticipationRequest.builder()
				.event(event)
				.requester(requester)
				.created(LocalDateTime.now())
				.status(status)
				.build();
	}

	public ParticipationRequestDto toDto(ParticipationRequest request) {
		return ParticipationRequestDto.builder()
				.id(request.getId())
				.event(request.getEvent().getId())
				.requester(request.getRequester().getId())
				.created(request.getCreated())
				.status(request.getStatus())
				.build();
	}
}