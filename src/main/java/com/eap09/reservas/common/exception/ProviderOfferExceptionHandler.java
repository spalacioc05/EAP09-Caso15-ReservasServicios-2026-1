package com.eap09.reservas.common.exception;

import com.eap09.reservas.common.response.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProviderOfferExceptionHandler extends AbstractErrorResponseHandler {

    @ExceptionHandler(ServiceNameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleServiceNameAlreadyExists(ServiceNameAlreadyExistsException ex) {
        return response(HttpStatus.CONFLICT, "SERVICE_NAME_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(ServiceStatusAlreadySetException.class)
    public ResponseEntity<ErrorResponse> handleServiceStatusAlreadySet(ServiceStatusAlreadySetException ex) {
        return response(HttpStatus.CONFLICT, "SERVICE_STATUS_ALREADY_SET", ex.getMessage());
    }

    @ExceptionHandler(ServiceInactivationBlockedException.class)
    public ResponseEntity<ErrorResponse> handleServiceInactivationBlocked(ServiceInactivationBlockedException ex) {
        return response(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(AvailabilityOverlapException.class)
    public ResponseEntity<ErrorResponse> handleAvailabilityOverlap(AvailabilityOverlapException ex) {
        return response(HttpStatus.CONFLICT, "AVAILABILITY_OVERLAP", ex.getMessage());
    }

    @ExceptionHandler(ServiceStatusChangeFailedException.class)
    public ResponseEntity<ErrorResponse> handleServiceStatusChangeFailed(ServiceStatusChangeFailedException ex) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "SERVICE_STATUS_CHANGE_FAILED", ex.getMessage());
    }
}