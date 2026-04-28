package com.eap09.reservas.provideroffer.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusResponse;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;
import java.time.OffsetDateTime;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para cambio de estado de servicios propios del proveedor.
 * Soporta transiciones ACTIVO ↔ INACTIVO.
 * 
 * Responsabilidades:
 * - Validar que el proveedor es autenticado y tiene rol PROVEEDOR
 * - Validar que el servicio existe y pertenece al proveedor
 * - Validar que el estado objetivo es válido
 * - Validar que el cambio es necesario (no está redundante)
 * - Cambiar el estado en BD
 * - Registrar evento de trazabilidad
 */
@Service
public class ServiceStatusService {

    private static final String PROVIDER_ROLE = "PROVEEDOR";
    private static final String SERVICE_STATE_CATEGORY = "tbl_servicio";
    private static final String ACTIVE_STATE = "ACTIVO";
    private static final String INACTIVE_STATE = "INACTIVO";

    private final UserAccountRepository userAccountRepository;
    private final ServiceRepository serviceRepository;
    private final StateRepository stateRepository;
    private final SystemEventPublisher systemEventPublisher;

    public ServiceStatusService(UserAccountRepository userAccountRepository,
                               ServiceRepository serviceRepository,
                               StateRepository stateRepository,
                               SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.serviceRepository = serviceRepository;
        this.stateRepository = stateRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    /**
     * Cambia el estado de un servicio propio del proveedor.
     * 
     * @param authenticatedUsername email del usuario autenticado (proveedor)
     * @param serviceId ID del servicio a cambiar de estado
     * @param request DTO con targetStatus (ACTIVO | INACTIVO)
     * @return ServiceStatusResponse con detalles del servicio modificado
     * 
     * @throws ProviderRoleRequiredException si no es proveedor
     * @throws ResourceNotFoundException si el servicio no existe
     * @throws AccessDeniedException si el servicio no pertenece al proveedor
     * @throws ApiException si el targetStatus es inválido
     * @throws ServiceAlreadyInStateException si ya está en el estado deseado
     */
    @Transactional
    public ServiceStatusResponse updateServiceStatus(String authenticatedUsername,
                                                      Long serviceId,
                                                      ServiceStatusRequest request) {
        // 1. Resolver y validar proveedor autenticado
        UserAccountEntity provider = resolveAuthenticatedProvider(authenticatedUsername);

        // 2. Validar y normalizar targetStatus
        String normalizedTargetStatus = validateAndNormalizeStatus(request.targetStatus());

        // 3. Resolver estados posibles
        StateEntity targetStatusEntity = stateRepository.findByCategoryAndStateName(
                SERVICE_STATE_CATEGORY, 
                normalizedTargetStatus)
                .orElseThrow(() -> new IllegalStateException(
                        "Required state " + normalizedTargetStatus + " for tbl_servicio was not found"));

        // 4. Resolver servicio y validar propiedad
        ServiceEntity targetService = resolveServiceForProvider(provider.getIdUsuario(), serviceId);

        // 5. Obtener estado actual
        StateEntity currentStatusEntity = stateRepository.findById(targetService.getIdEstadoServicio())
                .orElseThrow(() -> new IllegalStateException(
                        "Current state of service could not be resolved"));

        // 6. Si el estado ya es el deseado, devolver respuesta idempotente (200 OK)
        if (currentStatusEntity.getIdEstado().equals(targetStatusEntity.getIdEstado())) {
            // El servicio ya está en el estado deseado.
            // Esto es una operación idempotente: devolvemos 200 OK sin cambios.
            return new ServiceStatusResponse(
                    targetService.getIdServicio(),
                    targetService.getNombreServicio(),
                    targetStatusEntity.getNombreEstado());
        }

        // 7. Cambiar estado y actualizar fecha
        targetService.setIdEstadoServicio(targetStatusEntity.getIdEstado());
        targetService.setFechaActualizacionServicio(OffsetDateTime.now());

        ServiceEntity saved = serviceRepository.save(targetService);

        // 8. Registrar evento de trazabilidad
        String eventType = normalizedTargetStatus.equals(ACTIVE_STATE) 
                ? "ACTIVACION_SERVICIO" 
                : "INACTIVACION_SERVICIO";
        
        systemEventPublisher.publish(SystemEvent.now(
                eventType,
                "tbl_servicio",
                String.valueOf(provider.getIdUsuario()),
                String.valueOf(saved.getIdServicio()),
                "EXITO",
                "Servicio " + eventType.toLowerCase() + " por proveedor",
                TraceIdUtil.currentTraceId()));

        return new ServiceStatusResponse(
                saved.getIdServicio(),
                saved.getNombreServicio(),
                targetStatusEntity.getNombreEstado());
    }

    /**
     * Valida que el usuario autenticado es proveedor.
     */
    private UserAccountEntity resolveAuthenticatedProvider(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ProviderRoleRequiredException(
                        "Solo un proveedor autenticado puede cambiar el estado de servicios"));

        if (!PROVIDER_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ProviderRoleRequiredException(
                    "Solo un proveedor autenticado puede cambiar el estado de servicios");
        }

        return user;
    }

    /**
     * Valida y normaliza el estado objetivo.
     * Acepta: ACTIVO, INACTIVO (case-insensitive)
     */
    private String validateAndNormalizeStatus(String targetStatus) {
        if (targetStatus == null || targetStatus.trim().isEmpty()) {
            throw new ApiException("INVALID_STATUS", "El estado objetivo es requerido");
        }

        String normalized = targetStatus.trim().toUpperCase();
        
        if (!normalized.equals(ACTIVE_STATE) && !normalized.equals(INACTIVE_STATE)) {
            throw new ApiException("INVALID_STATUS", 
                    "El estado debe ser ACTIVO o INACTIVO");
        }

        return normalized;
    }

    /**
     * Valida que el servicio existe y pertenece al proveedor.
     * 
     * @throws ResourceNotFoundException si no existe
     * @throws AccessDeniedException si no pertenece al proveedor
     */
    private ServiceEntity resolveServiceForProvider(Long providerUserId, Long serviceId) {
        ServiceEntity service = serviceRepository.findByIdServicio(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SERVICE_NOT_FOUND",
                        "El servicio indicado no existe"));

        if (!service.getIdUsuarioProveedor().equals(providerUserId)) {
            throw new AccessDeniedException("No tiene permisos para operar este servicio");
        }

        return service;
    }
}
