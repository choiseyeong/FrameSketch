package com.framesketch.common;

/** 요청한 자원이 없을 때. {@link ApiExceptionHandler} 가 404 로 바꾼다. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
