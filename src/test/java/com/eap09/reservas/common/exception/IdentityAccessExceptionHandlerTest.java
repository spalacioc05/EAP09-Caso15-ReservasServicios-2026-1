package com.eap09.reservas.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eap09.reservas.common.response.ErrorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;

class IdentityAccessExceptionHandlerTest {

    private final IdentityAccessExceptionHandler handler = new IdentityAccessExceptionHandler();

    @AfterEach
    void clearTraceId() {
        MDC.clear();
    }

    @Test
    void shouldUseDefaultMessageWhenAuthenticationMessageIsBlank() {
        MDC.put("traceId", "trace-auth");
        AuthenticationException ex = new AuthenticationException(" ") { };

        ResponseEntity<ErrorResponse> response = handler.handleAuthentication(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("UNAUTHORIZED", response.getBody().errorCode());
        assertEquals("Autenticacion requerida", response.getBody().message());
        assertEquals("trace-auth", response.getBody().traceId());
    }
}