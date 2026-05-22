package com.eap09.reservas.common.exception;

import com.eap09.reservas.common.response.ErrorResponse;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

abstract class AbstractErrorResponseHandler {

    protected ResponseEntity<ErrorResponse> response(HttpStatus status, String errorCode, String message) {
        return response(status, errorCode, message, List.of());
    }

    protected ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String errorCode,
            String message,
            List<String> details
    ) {
        return ResponseEntity.status(status).body(build(errorCode, message, details));
    }

    protected ResponseEntity<ErrorResponse> badRequest(String errorCode, String message, List<String> details) {
        return ResponseEntity.badRequest().body(build(errorCode, message, details));
    }

    protected ErrorResponse build(String errorCode, String message, List<String> details) {
        return new ErrorResponse(errorCode, message, details, MDC.get("traceId"));
    }
}