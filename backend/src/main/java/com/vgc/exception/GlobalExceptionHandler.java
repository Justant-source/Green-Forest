package com.vgc.exception;

import com.vgc.service.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger SYSTEM = LoggerFactory.getLogger("SYSTEM");

    private final ActivityLogService activityLogService;

    public GlobalExceptionHandler(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @ExceptionHandler(ProfileIncompleteException.class)
    public ResponseEntity<Map<String, String>> handleProfileIncomplete(ProfileIncompleteException e) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", "PROFILE_INCOMPLETE");
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status != null && status.is5xxServerError()) {
            activityLogService.logSystemError("HTTP_" + status.value(), e.getReason(), e);
        } else if (status != null && status.is4xxClientError()
                && status != HttpStatus.NOT_FOUND && status != HttpStatus.UNAUTHORIZED && status != HttpStatus.FORBIDDEN) {
            SYSTEM.warn("[HTTP_{}] {}", status.value(), e.getReason());
        }
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("message", e.getReason() != null ? e.getReason() : "오류가 발생했습니다"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        activityLogService.logSystemError("RUNTIME_ERROR", e.getMessage(), e);
        String message = e.getMessage() != null ? e.getMessage() : "서버 오류가 발생했습니다";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception e) {
        activityLogService.logSystemError("UNHANDLED_ERROR", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "서버 오류가 발생했습니다"));
    }
}
