package com.eap09.reservas.administration.application;

import com.eap09.reservas.administration.api.dto.UserAccountStatusUpdateRequest;
import com.eap09.reservas.administration.api.dto.UserAccountStatusUpdateResponse;
import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.exception.UserAccountStatusAlreadySetException;
import com.eap09.reservas.common.exception.UserAccountStatusUpdateFailedException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountStatusAdministrationService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountStatusAdministrationService.class);

    private static final String ADMIN_ROLE = "ADMINISTRADOR";
    private static final String USER_STATE_CATEGORY = "tbl_usuario";
    private static final String ACTIVE_STATE = "ACTIVA";
    private static final String INACTIVE_STATE = "INACTIVA";
    private static final String USER_ENTITY_TYPE = "tbl_usuario";
    private static final String STATUS_UPDATE_EVENT = "ACTUALIZACION_ESTADO_USUARIO";
    private static final String ADMIN_REQUIRED_MESSAGE =
            "Solo un administrador autenticado puede actualizar el estado de cuentas de usuario";
    private static final String INVALID_STATUS_MESSAGE = "El estado solicitado no es valido para usuarios";
    private static final String UPDATE_FAILED_MESSAGE =
            "No fue posible completar la actualizacion del estado de la cuenta de usuario. Intenta nuevamente mas tarde";

    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final SystemEventPublisher systemEventPublisher;

    public UserAccountStatusAdministrationService(UserAccountRepository userAccountRepository,
                                                  StateRepository stateRepository,
                                                  SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional
    public UserAccountStatusUpdateResponse updateUserAccountStatus(String authenticatedUsername,
                                                                   Long targetUserId,
                                                                   UserAccountStatusUpdateRequest request) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }

        UserAccountEntity adminUser = resolveAuthenticatedAdmin(authenticatedUsername);
        String normalizedTargetStatus = normalizeTargetStatus(request.targetStatus());

        try {
            StateEntity activeUserState = resolveRequiredUserState(ACTIVE_STATE);
            StateEntity inactiveUserState = resolveRequiredUserState(INACTIVE_STATE);
            StateEntity targetState = resolveTargetState(normalizedTargetStatus, activeUserState, inactiveUserState);

            UserAccountEntity targetUser = userAccountRepository.findById(targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));

            StateEntity currentState = resolveCurrentState(targetUser, activeUserState, inactiveUserState);
            ensureStatusChangeIsNeeded(currentState.getNombreEstado(), normalizedTargetStatus);

            targetUser.setIdEstado(targetState.getIdEstado());
            UserAccountEntity updatedUser = userAccountRepository.save(targetUser);

            publishEvent(
                    adminUser.getIdUsuario(),
                    updatedUser.getIdUsuario(),
                    "EXITO",
                    buildSuccessDetail(currentState.getNombreEstado(), targetState.getNombreEstado()));

            return new UserAccountStatusUpdateResponse(
                    updatedUser.getIdUsuario(),
                    updatedUser.getCorreoUsuario(),
                    currentState.getNombreEstado(),
                    targetState.getNombreEstado());
        } catch (DataAccessException ex) {
            publishEventSafely(
                    adminUser.getIdUsuario(),
                    targetUserId,
                    "FALLO",
                    "No fue posible completar la actualizacion del estado de la cuenta de usuario");
            log.error("Error de datos al actualizar el estado del usuario {}", targetUserId, ex);
            throw new UserAccountStatusUpdateFailedException(UPDATE_FAILED_MESSAGE);
        } catch (RuntimeException ex) {
            publishEventSafely(
                    adminUser.getIdUsuario(),
                    targetUserId,
                    "FALLO",
                    resolveFailureDetail(ex));
            throw ex;
        }
    }

    private UserAccountEntity resolveAuthenticatedAdmin(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new AdminRoleRequiredException(ADMIN_REQUIRED_MESSAGE));

        if (!ADMIN_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new AdminRoleRequiredException(ADMIN_REQUIRED_MESSAGE);
        }

        return user;
    }

    private String normalizeTargetStatus(String targetStatus) {
        String normalizedStatus = targetStatus == null ? null : targetStatus.trim().toUpperCase(Locale.ROOT);
        if (!ACTIVE_STATE.equals(normalizedStatus) && !INACTIVE_STATE.equals(normalizedStatus)) {
            throw new ApiException("INVALID_USER_ACCOUNT_STATUS", INVALID_STATUS_MESSAGE);
        }
        return normalizedStatus;
    }

    private StateEntity resolveRequiredUserState(String stateName) {
        return stateRepository.findByCategoryAndStateName(USER_STATE_CATEGORY, stateName)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontro el estado requerido: " + USER_STATE_CATEGORY + " / " + stateName));
    }

    private StateEntity resolveTargetState(String normalizedTargetStatus,
                                           StateEntity activeUserState,
                                           StateEntity inactiveUserState) {
        return ACTIVE_STATE.equals(normalizedTargetStatus) ? activeUserState : inactiveUserState;
    }

    private StateEntity resolveCurrentState(UserAccountEntity targetUser,
                                            StateEntity activeUserState,
                                            StateEntity inactiveUserState) {
        if (activeUserState.getIdEstado().equals(targetUser.getIdEstado())) {
            return activeUserState;
        }

        if (inactiveUserState.getIdEstado().equals(targetUser.getIdEstado())) {
            return inactiveUserState;
        }

        throw new IllegalStateException("El usuario objetivo tiene un estado no soportado para administracion");
    }

    private void ensureStatusChangeIsNeeded(String currentStatus, String targetStatus) {
        if (currentStatus.equalsIgnoreCase(targetStatus)) {
            throw buildAlreadySetException(targetStatus);
        }
    }

    private UserAccountStatusAlreadySetException buildAlreadySetException(String targetStatus) {
        if (ACTIVE_STATE.equals(targetStatus)) {
            return new UserAccountStatusAlreadySetException("La cuenta ya se encontraba activada");
        }

        return new UserAccountStatusAlreadySetException("La cuenta ya se encontraba desactivada");
    }

    private String buildSuccessDetail(String previousStatus, String newStatus) {
        return "Estado de cuenta actualizado de " + previousStatus + " a " + newStatus;
    }

    private String resolveFailureDetail(RuntimeException ex) {
        if (ex instanceof ApiException
                || ex instanceof ResourceNotFoundException
                || ex instanceof UserAccountStatusAlreadySetException
                || ex instanceof AdminRoleRequiredException) {
            return ex.getMessage();
        }

        return "No fue posible completar la actualizacion del estado de la cuenta de usuario";
    }

    private void publishEvent(Long responsibleUserId,
                              Long targetUserId,
                              String result,
                              String detail) {
        systemEventPublisher.publish(SystemEvent.now(
                STATUS_UPDATE_EVENT,
                USER_ENTITY_TYPE,
                responsibleUserId == null ? null : String.valueOf(responsibleUserId),
                targetUserId == null ? null : String.valueOf(targetUserId),
                result,
                detail,
                TraceIdUtil.currentTraceId()));
    }

    private void publishEventSafely(Long responsibleUserId,
                                    Long targetUserId,
                                    String result,
                                    String detail) {
        try {
            publishEvent(responsibleUserId, targetUserId, result, detail);
        } catch (RuntimeException publishFailure) {
            log.warn(
                    "No fue posible registrar evento de auditoria {} para el usuario {}",
                    STATUS_UPDATE_EVENT,
                    targetUserId,
                    publishFailure);
        }
    }
}