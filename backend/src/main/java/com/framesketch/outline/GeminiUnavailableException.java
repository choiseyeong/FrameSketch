package com.framesketch.outline;

/**
 * Gemini 를 쓸 수 없을 때. 키가 비어 있거나 호출이 실패한 경우를 통틀어 쓴다.
 * {@link com.framesketch.common.ApiExceptionHandler} 가 503 으로 바꾼다.
 */
public class GeminiUnavailableException extends RuntimeException {
    public GeminiUnavailableException(String message) { super(message); }
}
