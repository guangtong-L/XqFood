package ai.xiaodudou.module.action.dto;

public record CheckinResponse(Long actionId, boolean created, boolean alreadyExists) {}
