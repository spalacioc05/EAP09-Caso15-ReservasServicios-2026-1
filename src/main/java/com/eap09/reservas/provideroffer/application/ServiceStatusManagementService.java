package com.eap09.reservas.provideroffer.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.exception.ServiceStatusAlreadySetException;
import com.eap09.reservas.common.exception.ServiceStatusChangeFailedException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusUpdateRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusUpdateResponse;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceStatusManagementService {

    private static final Logger log = LoggerFactory.getLogger(ServiceStatusManagementService.class);

    private static final String PROVIDER_ROLE = "PROVEEDOR";
    private static final String SERVICE_STATE_CATEGORY = "tbl_servicio";
    private static final String ACTIVE_STATE = "ACTIVO";
    private static final String INACTIVE_STATE = "INACTIVO";
    private static final String SERVICE_ENTITY_TYPE = "tbl_servicio";
    private static final String ACTIVATE_EVENT = "ACTIVACION_SERVICIO";
    private static final String INACTIVATE_EVENT = "INACTIVACION_SERVICIO";

    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final ServiceRepository serviceRepository;
    private final SystemEventPublisher systemEventPublisher;

    public ServiceStatusManagementService(UserAccountRepository userAccountRepository,
                                          StateRepository stateRepository,
                                          ServiceRepository serviceRepository,
                                          SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.serviceRepository = serviceRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional(noRollbackFor = {
            ProviderRoleRequiredException.class,
            ResourceNotFoundException.class,
            AccessDeniedException.class,
            ServiceStatusAlreadySetException.class,
            ApiException.class
    })
    public ServiceStatusUpdateResponse updateOwnServiceStatus(String authenticatedUsername,
                                                              Long serviceId,
                                                              ServiceStatusUpdateRequest request) {
        String targetStatus = normalizeTargetStatus(request.targetStatus());
        String eventType = resolveEventType(targetStatus);
        UserAccountEntity provider = resolveAuthenticatedProvider(authenticatedUsername);

        try {
            StateEntity targetState = resolveServiceState(targetStatus);
            ServiceEntity service = serviceRepository.findByIdServicio(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "SERVICE_NOT_FOUND",
                            "El servicio indicado no existe"));

            if (!service.getIdUsuarioProveedor().equals(provider.getIdUsuario())) {
                throw new AccessDeniedException("No tiene permisos para cambiar el estado de este servicio");
            }

            if (targetState.getIdEstado().equals(service.getIdEstadoServicio())) {
                throw buildRedundantStateException(targetStatus);
            }

            service.setIdEstadoServicio(targetState.getIdEstado());
            service.setFechaActualizacionServicio(OffsetDateTime.now());

            ServiceEntity updatedService = serviceRepository.save(service);

            publishEvent(
                    eventType,
                    provider.getIdUsuario(),
                    updatedService.getIdServicio(),
                    "EXITO",
                    buildSuccessDetail(updatedService.getNombreServicio(), targetStatus));

            return new ServiceStatusUpdateResponse(
                    updatedService.getIdServicio(),
                    updatedService.getNombreServicio(),
                    targetState.getNombreEstado());
        } catch (DataAccessException ex) {
            publishEventSafely(
                    eventType,
                    provider.getIdUsuario(),
                    serviceId,
                    "FALLO",
                    "No fue posible completar el cambio de estado del servicio");
            log.error("Error de datos al actualizar el estado del servicio {}", serviceId, ex);
            throw new ServiceStatusChangeFailedException(
                    "No fue posible completar el cambio de estado del servicio. Intenta nuevamente mas tarde");
        } catch (RuntimeException ex) {
            publishEventSafely(
                    eventType,
                    provider.getIdUsuario(),
                    serviceId,
                    "FALLO",
                    resolveFailureDetail(targetStatus, ex));
            throw ex;
        }
    }

    private UserAccountEntity resolveAuthenticatedProvider(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ProviderRoleRequiredException(
                        "Solo un proveedor autenticado puede cambiar el estado de sus servicios"));

        if (!PROVIDER_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ProviderRoleRequiredException("Solo un proveedor autenticado puede cambiar el estado de sus servicios");
        }

        return user;
    }

    private StateEntity resolveServiceState(String targetStatus) {
        return stateRepository.findByCategoryAndStateName(SERVICE_STATE_CATEGORY, targetStatus)
                .orElseThrow(() -> new IllegalStateException(
                        "Required state " + targetStatus + " for tbl_servicio was not found"));
    }

    private String normalizeTargetStatus(String targetStatus) {
        String normalizedStatus = targetStatus == null ? null : targetStatus.trim().toUpperCase(Locale.ROOT);

        if (!ACTIVE_STATE.equals(normalizedStatus) && !INACTIVE_STATE.equals(normalizedStatus)) {
            throw new ApiException(
                    "INVALID_SERVICE_STATUS",
                    "targetStatus debe ser ACTIVO o INACTIVO");
        }

        return normalizedStatus;
    }

    private String resolveEventType(String targetStatus) {
        return ACTIVE_STATE.equals(targetStatus) ? ACTIVATE_EVENT : INACTIVATE_EVENT;
    }

    private ServiceStatusAlreadySetException buildRedundantStateException(String targetStatus) {
        if (ACTIVE_STATE.equals(targetStatus)) {
            return new ServiceStatusAlreadySetException("El servicio ya se encuentra activo");
        }

        return new ServiceStatusAlreadySetException("El servicio ya se encuentra inactivo");
    }

    private String buildSuccessDetail(String serviceName, String targetStatus) {
        if (ACTIVE_STATE.equals(targetStatus)) {
            return "Servicio '" + serviceName + "' activado correctamente";
        }

        return "Servicio '" + serviceName + "' inactivado correctamente";
    }

    private String resolveFailureDetail(String targetStatus, RuntimeException ex) {
        if (ex instanceof ServiceStatusAlreadySetException
                || ex instanceof ResourceNotFoundException
                || ex instanceof ProviderRoleRequiredException
                || ex instanceof ApiException) {
            return ex.getMessage();
        }

        if (ex instanceof AccessDeniedException) {
            return "Intento de cambio de estado sobre un servicio que no pertenece al proveedor autenticado";
        }

        return ACTIVE_STATE.equals(targetStatus)
                ? "Fallo al activar el servicio"
                : "Fallo al inactivar el servicio";
    }

    private void publishEvent(String eventType,
                              Long providerUserId,
                              Long serviceId,
                              String result,
                              String details) {
        systemEventPublisher.publish(SystemEvent.now(
                eventType,
                SERVICE_ENTITY_TYPE,
                String.valueOf(providerUserId),
                String.valueOf(serviceId),
                result,
                details,
                TraceIdUtil.currentTraceId()));
    }

    private void publishEventSafely(String eventType,
                                    Long providerUserId,
                                    Long serviceId,
                                    String result,
                                    String details) {
        try {
            publishEvent(eventType, providerUserId, serviceId, result, details);
        } catch (RuntimeException publishFailure) {
            log.warn("No fue posible registrar evento de auditoria {} para el servicio {}", eventType, serviceId, publishFailure);
        }
    }
}