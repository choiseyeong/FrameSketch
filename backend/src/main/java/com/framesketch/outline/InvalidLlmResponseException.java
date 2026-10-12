package com.framesketch.outline;

/**
 * LLM 이 돌려준 응답이 JSON 형식이 아니거나 필수 필드가 비어 있을 때.
 * {@link com.framesketch.common.ApiExceptionHandler} 가 502 로 바꾼다 (상류 응답 불량).
 */
public class InvalidLlmResponseException extends RuntimeException {
    public InvalidLlmResponseException(String message) { super(message); }
}
