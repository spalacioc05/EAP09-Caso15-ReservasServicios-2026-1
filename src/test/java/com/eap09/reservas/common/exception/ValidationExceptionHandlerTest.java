package com.eap09.reservas.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class ValidationExceptionHandlerTest {

    private final ValidationExceptionHandler handler = new ValidationExceptionHandler();

    @AfterEach
    void clearTraceId() {
        MDC.clear();
    }

    @Test
    void shouldPrioritizeNotBlankOverPatternAndPreserveTraceId() throws Exception {
        MDC.put("traceId", "trace-validation");

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "correo", "", false,
                new String[]{"Pattern"}, null, "correo tiene formato invalido"));
        bindingResult.addError(new FieldError("request", "correo", "", false,
                new String[]{"NotBlank"}, null, "correo es obligatorio"));
        bindingResult.addError(new FieldError("request", "contrasena", "", false,
                new String[]{"NotBlank"}, null, "contrasena es obligatoria"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(requestParameter(), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().errorCode());
        assertEquals("Validacion de la solicitud fallida", response.getBody().message());
        assertIterableEquals(List.of(
                "correo: correo es obligatorio",
                "contrasena: contrasena es obligatoria"
        ), response.getBody().details());
        assertEquals("trace-validation", response.getBody().traceId());
    }

    @Test
    void shouldFormatConstraintViolationsAsValidationErrors() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(path.toString()).thenReturn("payload.correo");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("correo es obligatorio");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(List.of("payload.correo: correo es obligatorio"), response.getBody().details());
    }

    @Test
    void shouldUseDefaultParameterNameForBlankTypeMismatchNames() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn(" ");

        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(List.of("parametro: valor invalido"), response.getBody().details());
    }

    @Test
    void shouldDescribeBlankLocalDateAsRequiredField() {
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(httpMessageForInvalidFormat(
                LocalDate.class,
                "",
                "fecha"
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(List.of("fecha: fecha es obligatoria"), response.getBody().details());
    }

    @Test
    void shouldDescribeInvalidLocalDateFormat() {
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(httpMessageForInvalidFormat(
                LocalDate.class,
                "2026/05/22",
                "fecha"
        ));

        assertEquals(List.of("fecha: valor invalido, use el formato yyyy-MM-dd"), response.getBody().details());
    }

    @Test
    void shouldDescribeBlankLocalTimeAsRequiredField() {
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(httpMessageForInvalidFormat(
                LocalTime.class,
                "",
                "horaInicio"
        ));

        assertEquals(List.of("horaInicio: horaInicio es obligatoria"), response.getBody().details());
    }

    @Test
    void shouldDescribeInvalidLocalTimeFormat() {
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(httpMessageForInvalidFormat(
                LocalTime.class,
                "25:00:00",
                "horaInicio"
        ));

        assertEquals(List.of("horaInicio: valor invalido, use el formato HH:mm:ss"), response.getBody().details());
    }

    @Test
    void shouldFallbackToBodyWhenInvalidFormatHasNoFieldPath() {
        InvalidFormatException invalidFormatException = InvalidFormatException.from(
                null,
                "valor invalido",
                99,
                Integer.class
        );

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("payload invalido", invalidFormatException, null)
        );

        assertEquals(List.of("cuerpo: valor invalido"), response.getBody().details());
    }

    @Test
    void shouldReturnGenericBodyFormatMessageWhenNoInvalidFormatExceptionExists() {
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("payload invalido", new MockHttpInputMessage(new byte[0]))
        );

        assertEquals(List.of("cuerpo: formato invalido"), response.getBody().details());
    }

    private MethodParameter requestParameter() throws NoSuchMethodException {
        Method method = List.class.getMethod("add", Object.class);
        return new MethodParameter(method, 0);
    }

    private HttpMessageNotReadableException httpMessageForInvalidFormat(
            Class<?> targetType,
            Object rejectedValue,
            String fieldName
    ) {
        InvalidFormatException invalidFormatException = InvalidFormatException.from(
                null,
                "valor invalido",
                rejectedValue,
                targetType
        );
        invalidFormatException.prependPath(new Object(), fieldName);
        return new HttpMessageNotReadableException(
            "payload invalido",
            invalidFormatException,
            new MockHttpInputMessage(new byte[0])
        );
    }
}