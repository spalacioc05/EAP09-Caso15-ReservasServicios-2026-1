package com.eap09.reservas.administration.application;

import com.eap09.reservas.administration.api.dto.UserRoleUpdateRequest;
import com.eap09.reservas.administration.api.dto.UserRoleUpdateResponse;
import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.exception.TargetUserInactiveException;
import com.eap09.reservas.common.exception.UserRoleAlreadyAssignedException;
import com.eap09.reservas.common.exception.UserRoleUpdateFailedException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.RoleRepository;
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
public class UserRoleAdministrationService {

    private static final Logger log = LoggerFactory.getLogger(UserRoleAdministrationService.class);

    private static final String ADMIN_ROLE = "ADMINISTRADOR";
    private static final String USER_STATE_CATEGORY = "tbl_usuario";
    private static final String ACTIVE_USER_STATE = "ACTIVA";
    private static final String USER_ENTITY_TYPE = "tbl_usuario";
    private static final String ROLE_UPDATE_EVENT = "ACTUALIZACION_ROL_USUARIO";
    private static final String ADMIN_REQUIRED_MESSAGE =
            "Solo un administrador autenticado puede actualizar roles de usuario";

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final StateRepository stateRepository;
    private final SystemEventPublisher systemEventPublisher;

    public UserRoleAdministrationService(UserAccountRepository userAccountRepository,
                                         RoleRepository roleRepository,
                                         StateRepository stateRepository,
                                         SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.stateRepository = stateRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional
    public UserRoleUpdateResponse updateUserRole(String authenticatedUsername,
                                                 Long targetUserId,
                                                 UserRoleUpdateRequest request) {
        if (authenticatedUsername == null || authenticatedUsername.isBlank()) {
            throw new InsufficientAuthenticationException("Autenticacion requerida");
        }

        UserAccountEntity adminUser = resolveAuthenticatedAdmin(authenticatedUsername);
        String normalizedRoleName = normalizeRoleName(request.roleName());

        try {
            RoleEntity targetRole = roleRepository.findByNombreRol(normalizedRoleName)
                    .orElseThrow(() -> new ApiException("ROLE_NAME_INVALID", "El rol indicado no existe"));

            UserAccountEntity targetUser = userAccountRepository.findById(targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "USER_NOT_FOUND",
                            "El usuario indicado no existe"));

            if (!isActiveUser(targetUser)) {
                throw new TargetUserInactiveException("El usuario se encuentra desactivado");
            }

            String currentRoleName = targetUser.getRol().getNombreRol();
            if (currentRoleName.equalsIgnoreCase(targetRole.getNombreRol())) {
                throw new UserRoleAlreadyAssignedException("El usuario ya tiene asignado ese rol");
            }

            targetUser.setRol(targetRole);
            UserAccountEntity updatedUser = userAccountRepository.save(targetUser);

            publishEvent(
                    adminUser.getIdUsuario(),
                    updatedUser.getIdUsuario(),
                    "EXITO",
                    "Rol actualizado de " + currentRoleName + " a " + targetRole.getNombreRol());

            return new UserRoleUpdateResponse(
                    updatedUser.getIdUsuario(),
                    updatedUser.getCorreoUsuario(),
                    currentRoleName,
                    updatedUser.getRol().getNombreRol());
        } catch (DataAccessException ex) {
            publishEventSafely(
                    adminUser.getIdUsuario(),
                    targetUserId,
                    "FALLO",
                    "No fue posible completar la actualizacion del rol del usuario");
            log.error("Error de datos al actualizar rol del usuario {}", targetUserId, ex);
            throw new UserRoleUpdateFailedException(
                "No fue posible completar la actualizacion del rol del usuario. Intenta nuevamente mas tarde");
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

    private String normalizeRoleName(String roleName) {
        String normalizedRoleName = roleName == null ? null : roleName.trim().toUpperCase(Locale.ROOT);
        if (normalizedRoleName == null || normalizedRoleName.isBlank()) {
            throw new ApiException("ROLE_NAME_REQUIRED", "roleName es obligatorio");
        }
        return normalizedRoleName;
    }

    private boolean isActiveUser(UserAccountEntity targetUser) {
        Long activeUserStateId = stateRepository.findByCategoryAndStateName(USER_STATE_CATEGORY, ACTIVE_USER_STATE)
                .map(StateEntity::getIdEstado)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontro el estado requerido: " + USER_STATE_CATEGORY + " / " + ACTIVE_USER_STATE));

        return activeUserStateId.equals(targetUser.getIdEstado());
    }

    private String resolveFailureDetail(RuntimeException ex) {
        if (ex instanceof ApiException
                || ex instanceof ResourceNotFoundException
                || ex instanceof UserRoleAlreadyAssignedException
                || ex instanceof TargetUserInactiveException
                || ex instanceof AdminRoleRequiredException) {
            return ex.getMessage();
        }

        return "No fue posible completar la actualizacion del rol del usuario";
    }

    private void publishEvent(Long responsibleUserId,
                              Long targetUserId,
                              String result,
                              String detail) {
        systemEventPublisher.publish(SystemEvent.now(
                ROLE_UPDATE_EVENT,
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
                    ROLE_UPDATE_EVENT,
                    targetUserId,
                    publishFailure);
        }
    }
}