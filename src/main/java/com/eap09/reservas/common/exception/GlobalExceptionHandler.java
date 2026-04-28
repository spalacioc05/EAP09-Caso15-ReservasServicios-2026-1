package com.eap09.reservas.common.exception;

import com.eap09.reservas.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        return ResponseEntity.badRequest().body(build("VALIDATION_ERROR", "Validacion de la solicitud fallida", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return ResponseEntity.badRequest().body(build("VALIDATION_ERROR", "Validacion de la solicitud fallida", details));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String parameterName = ex.getName() == null ? "parametro" : ex.getName();
        String detail = parameterName + ": valor invalido";
        return ResponseEntity.badRequest()
                .body(build("VALIDATION_ERROR", "Validacion de la solicitud fallida", List.of(detail)));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.badRequest().body(build(ex.getErrorCode(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailConflict(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build("EMAIL_ALREADY_REGISTERED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build("INVALID_CREDENTIALS", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountInactive(AccountInactiveException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build("ACCOUNT_INACTIVE", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(TemporaryAccessRestrictedException.class)
    public ResponseEntity<ErrorResponse> handleTemporaryRestriction(TemporaryAccessRestrictedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build("ACCESS_TEMPORARILY_RESTRICTED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ProviderRoleRequiredException.class)
    public ResponseEntity<ErrorResponse> handleProviderRoleRequired(ProviderRoleRequiredException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build("PROVIDER_ROLE_REQUIRED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ClientRoleRequiredException.class)
    public ResponseEntity<ErrorResponse> handleClientRoleRequired(ClientRoleRequiredException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build("CLIENT_ROLE_REQUIRED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ServiceNameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleServiceNameAlreadyExists(ServiceNameAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build("SERVICE_NAME_ALREADY_EXISTS", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ServiceStatusAlreadySetException.class)
    public ResponseEntity<ErrorResponse> handleServiceStatusAlreadySet(ServiceStatusAlreadySetException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build("SERVICE_STATUS_ALREADY_SET", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(AvailabilityOverlapException.class)
    public ResponseEntity<ErrorResponse> handleAvailabilityOverlap(AvailabilityOverlapException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build("AVAILABILITY_OVERLAP", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(ex.getErrorCode(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(OfferQueryFailedException.class)
    public ResponseEntity<ErrorResponse> handleOfferQueryFailed(OfferQueryFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("OFFER_QUERY_UNAVAILABLE", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(AvailabilityQueryFailedException.class)
    public ResponseEntity<ErrorResponse> handleAvailabilityQueryFailed(AvailabilityQueryFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("AVAILABILITY_QUERY_UNAVAILABLE", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ErrorResponse> handleReservationConflict(ReservationConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(ex.getErrorCode(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ReservationCreationFailedException.class)
    public ResponseEntity<ErrorResponse> handleReservationCreationFailed(ReservationCreationFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("RESERVATION_CREATION_FAILED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ServiceStatusChangeFailedException.class)
    public ResponseEntity<ErrorResponse> handleServiceStatusChangeFailed(ServiceStatusChangeFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("SERVICE_STATUS_CHANGE_FAILED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ProviderReservationQueryFailedException.class)
    public ResponseEntity<ErrorResponse> handleProviderReservationQueryFailed(ProviderReservationQueryFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("PROVIDER_BOOKING_QUERY_FAILED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(CustomerReservationQueryFailedException.class)
    public ResponseEntity<ErrorResponse> handleCustomerReservationQueryFailed(CustomerReservationQueryFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("CUSTOMER_BOOKING_QUERY_FAILED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ReservationFinalizationFailedException.class)
    public ResponseEntity<ErrorResponse> handleReservationFinalizationFailed(ReservationFinalizationFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("RESERVATION_FINALIZATION_FAILED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ReservationCancellationFailedException.class)
    public ResponseEntity<ErrorResponse> handleReservationCancellationFailed(ReservationCancellationFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("RESERVATION_CANCELLATION_FAILED", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(SessionNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotActive(SessionNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build("SESSION_NOT_ACTIVE", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ProfileNoChangesException.class)
    public ResponseEntity<ErrorResponse> handleProfileNoChanges(ProfileNoChangesException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build("PROFILE_NO_CHANGES", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        String message = (ex.getMessage() == null || ex.getMessage().isBlank())
            ? "Autenticacion requerida"
            : ex.getMessage();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(build("UNAUTHORIZED", message, List.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build("FORBIDDEN", "Permisos insuficientes", List.of(ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build("INTERNAL_ERROR", "No fue posible completar la solicitud", List.of()));
    }

    private ErrorResponse build(String errorCode, String message, List<String> details) {
        return new ErrorResponse(errorCode, message, details, MDC.get("traceId"));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
