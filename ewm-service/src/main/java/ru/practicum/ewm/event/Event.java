package ru.practicum.ewm.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.ewm.user.User;

// ВРЕМЕННАЯ ЗАГЛУШКА: полная сущность события будет в задаче #17 (ветка main_svc_events).
// При мерже #17 этот файл заменяется версией коллеги — поля ниже уже совпадают с ТЗ.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "events")
public class Event {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "initiator_id", nullable = false)
	private User initiator;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private EventState state;

	// 0 = без ограничения по количеству участников
	@Column(name = "participant_limit", nullable = false)
	private Integer participantLimit;

	@Column(name = "request_moderation", nullable = false)
	private Boolean requestModeration;
}
