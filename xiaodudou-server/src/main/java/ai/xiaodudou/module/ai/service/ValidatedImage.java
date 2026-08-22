package ai.xiaodudou.module.ai.service;

public record ValidatedImage(byte[] bytes, String mediaType, int width, int height) {}
