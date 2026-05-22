package com.eap09.reservas.common.exception;

import com.eap09.reservas.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler extends AbstractErrorResponseHandler {

    private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
    private static final String VALIDATION_FAILED_MESSAGE = "Validacion de la solicitud fallida";
    private static final String DEFAULT_PARAMETER_NAME = "parametro";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = collectFieldValidationDetails(ex.getBindingResult().getFieldErrors());
        return buildValidationError(details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return buildValidationError(details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String parameterName = ex.getName();
        if (parameterName.isBlank()) {
            parameterName = DEFAULT_PARAMETER_NAME;
        }
        return buildValidationError(List.of(parameterName + ": valor invalido"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildValidationError(extractReadableMessageDetails(ex));
    }

    private ResponseEntity<ErrorResponse> buildValidationError(List<String> details) {
        return badRequest(VALIDATION_ERROR_CODE, VALIDATION_FAILED_MESSAGE, details);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private List<String> collectFieldValidationDetails(List<FieldError> fieldErrors) {
        Map<String, FieldError> selectedErrorsByField = new LinkedHashMap<>();

        for (FieldError fieldError : fieldErrors) {
            selectedErrorsByField.merge(fieldError.getField(), fieldError, this::preferHigherPriorityError);
        }

        return selectedErrorsByField.values().stream()
                .map(this::formatFieldError)
                .toList();
    }

    private FieldError preferHigherPriorityError(FieldError currentError, FieldError candidateError) {
        return validationPriority(candidateError) < validationPriority(currentError)
                ? candidateError
                : currentError;
    }

    private int validationPriority(FieldError fieldError) {
        String validationCode = fieldError.getCode();
        if (validationCode == null) {
            return Integer.MAX_VALUE;
        }

        return switch (validationCode) {
            case "NotBlank", "NotNull" -> 0;
            case "Email", "Pattern" -> 1;
            case "Size" -> 2;
            default -> 10;
        };
    }

    private List<String> extractReadableMessageDetails(HttpMessageNotReadableException ex) {
        InvalidFormatException invalidFormatException = findInvalidFormatException(ex);
        if (invalidFormatException != null) {
            String fieldName = extractFieldName(invalidFormatException);
            if (fieldName == null || fieldName.isBlank()) {
                fieldName = "cuerpo";
            }

            Class<?> targetType = invalidFormatException.getTargetType();
            Object invalidValue = invalidFormatException.getValue();
            if (LocalDate.class.equals(targetType)) {
                if (invalidValue instanceof String stringValue && stringValue.isBlank()) {
                    return List.of(fieldName + ": " + fieldName + " es obligatoria");
                }
                return List.of(fieldName + ": valor invalido, use el formato yyyy-MM-dd");
            }

            if (LocalTime.class.equals(targetType)) {
                if (invalidValue instanceof String stringValue && stringValue.isBlank()) {
                    return List.of(fieldName + ": " + fieldName + " es obligatoria");
                }
                return List.of(fieldName + ": valor invalido, use el formato HH:mm:ss");
            }

            return List.of(fieldName + ": valor invalido");
        }

        return List.of("cuerpo: formato invalido");
    }

    private InvalidFormatException findInvalidFormatException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormatException) {
                return invalidFormatException;
            }
            current = current.getCause();
        }
        return null;
    }

    private String extractFieldName(InvalidFormatException invalidFormatException) {
        if (invalidFormatException.getPath() == null || invalidFormatException.getPath().isEmpty()) {
            return null;
        }

        return invalidFormatException.getPath().get(invalidFormatException.getPath().size() - 1).getFieldName();
    }
}