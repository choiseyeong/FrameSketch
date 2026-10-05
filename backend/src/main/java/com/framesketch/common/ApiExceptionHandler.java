package com.framesketch.common;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 예외를 일정한 형태의 JSON 으로 바꾼다.
 *
 * <p>프론트엔드가 어떤 오류든 같은 모양으로 읽을 수 있도록 {@code message} 하나는 항상
 * 채워 보낸다. 입력값 검증 실패일 때만 필드별 사유를 {@code fieldErrors} 에 덧붙인다.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** 오류 응답 본문. */
    public record ErrorResponse(String message, List<FieldErrorDetail> fieldErrors, String timestamp) {

        public record FieldErrorDetail(String field, String message) {}

        static ErrorResponse of(String message) {
            return new ErrorResponse(message, List.of(), Instant.now().toString());
        }

        static ErrorResponse of(String message, List<FieldErrorDetail> fieldErrors) {
            return new ErrorResponse(message, fieldErrors, Instant.now().toString());
        }
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(e.getMessage()));
    }

    /** {@code @Valid} 검증 실패. 어떤 필드가 왜 틀렸는지 함께 돌려준다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();

        String message = details.isEmpty()
                ? "입력값이 올바르지 않습니다."
                : details.getFirst().message();

        return ResponseEntity.badRequest().body(ErrorResponse.of(message, details));
    }

    /** 요청 본문이 비었거나 JSON 형식이 깨진 경우 등. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
    }

    /**
     * 예상하지 못한 예외. 내부 사정이 그대로 드러나지 않게 일반적인 문구만 내보내고,
     * 원인은 서버 로그에 남긴다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("서버에서 오류가 발생했습니다."));
    }

    private ErrorResponse.FieldErrorDetail toDetail(FieldError error) {
        String message = error.getDefaultMessage() == null
                ? "올바르지 않은 값입니다."
                : error.getDefaultMessage();
        return new ErrorResponse.FieldErrorDetail(error.getField(), message);
    }
}
