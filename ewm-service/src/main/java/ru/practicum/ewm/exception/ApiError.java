package ru.practicum.ewm.exception;

// ошибка как в спецификации
public record ApiError(String status, String reason, String message, String timestamp) {
}
