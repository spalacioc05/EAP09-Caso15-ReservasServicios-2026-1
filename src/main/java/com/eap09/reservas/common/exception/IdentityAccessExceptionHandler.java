package com.eap09.reservas.common.exception;

import com.eap09.reservas.common.response.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityAccessExceptionHandler extends AbstractErrorResponseHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailConflict(EmailAlreadyRegisteredException ex) {
        return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountInactive(AccountInactiveException ex) {
        return response(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", ex.getMessage());
    }

    @ExceptionHandler(TemporaryAccessRestrictedException.class)
    public ResponseEntity<ErrorResponse> handleTemporaryRestriction(TemporaryAccessRestrictedException ex) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_TEMPORARILY_RESTRICTED", ex.getMessage());
    }

    @ExceptionHandler(ProviderRoleRequiredException.class)
    public ResponseEntity<ErrorResponse> handleProviderRoleRequired(ProviderRoleRequiredException ex) {
        return response(HttpStatus.FORBIDDEN, "PROVIDER_ROLE_REQUIRED", ex.getMessage());
    }

    @ExceptionHandler(ClientRoleRequiredException.class)
    public ResponseEntity<ErrorResponse> handleClientRoleRequired(ClientRoleRequiredException ex) {
        return response(HttpStatus.FORBIDDEN, "CLIENT_ROLE_REQUIRED", ex.getMessage());
    }

    @ExceptionHandler(SessionNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotActive(SessionNotActiveException ex) {
        return response(HttpStatus.CONFLICT, "SESSION_NOT_ACTIVE", ex.getMessage());
    }

    @ExceptionHandler(ProfileNoChangesException.class)
    public ResponseEntity<ErrorResponse> handleProfileNoChanges(ProfileNoChangesException ex) {
        return response(HttpStatus.CONFLICT, "PROFILE_NO_CHANGES", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        String message = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "Autenticacion requerida"
                : ex.getMessage();
        return response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}