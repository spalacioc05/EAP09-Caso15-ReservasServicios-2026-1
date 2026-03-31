package com.eap09.reservas.provideroffer.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ServiceNameAlreadyExistsException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.api.dto.ServiceRegistrationRequest;
import com.eap09.reservas.provideroffer.api.dto.ServiceRegistrationResponse;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceRegistrationService {

    private static final String PROVIDER_ROLE = "PROVEEDOR";
    private static final String SERVICE_STATE_CATEGORY = "tbl_servicio";
    private static final String ACTIVE_STATE = "ACTIVO";

    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final ServiceRepository serviceRepository;
    private final SystemEventPublisher systemEventPublisher;

    public ServiceRegistrationService(UserAccountRepository userAccountRepository,
                                      StateRepository stateRepository,
                                      ServiceRepository serviceRepository,
                                      SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.serviceRepository = serviceRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional
    public ServiceRegistrationResponse registerService(String authenticatedUsername,
                                                       ServiceRegistrationRequest request) {
        UserAccountEntity provider = resolveAuthenticatedProvider(authenticatedUsername);
        validateBusinessValues(request);

        String normalizedServiceName = request.nombre().trim();
        if (serviceRepository.existsByProviderAndServiceName(provider.getIdUsuario(), normalizedServiceName)) {
            throw new ServiceNameAlreadyExistsException(
                    "No es posible crear un servicio con nombre repetido para el mismo proveedor");
        }

        StateEntity activeServiceState = stateRepository.findByCategoryAndStateName(SERVICE_STATE_CATEGORY, ACTIVE_STATE)
                .orElseThrow(() -> new IllegalStateException("Required state ACTIVO for tbl_servicio was not found"));

        OffsetDateTime now = OffsetDateTime.now();

        ServiceEntity entity = new ServiceEntity();
        entity.setIdUsuarioProveedor(provider.getIdUsuario());
        entity.setIdEstadoServicio(activeServiceState.getIdEstado());
        entity.setNombreServicio(normalizedServiceName);
        entity.setDescripcionServicio(request.descripcion().trim());
        entity.setDuracionMinutos(request.duracionMinutos());
        entity.setCapacidadMaximaConcurrente(request.capacidadMaximaConcurrente());
        entity.setFechaCreacionServicio(now);
        entity.setFechaActualizacionServicio(now);

        ServiceEntity saved = serviceRepository.save(entity);

        systemEventPublisher.publish(SystemEvent.now(
                "REGISTRO_SERVICIO",
                "tbl_servicio",
                String.valueOf(provider.getIdUsuario()),
                "EXITO",
                "Servicio registrado por proveedor",
                TraceIdUtil.currentTraceId()));

        return new ServiceRegistrationResponse(
                saved.getIdServicio(),
                saved.getNombreServicio(),
                saved.getDescripcionServicio(),
                saved.getDuracionMinutos(),
                saved.getCapacidadMaximaConcurrente(),
                activeServiceState.getNombreEstado());
    }

    private UserAccountEntity resolveAuthenticatedProvider(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ProviderRoleRequiredException("Solo un proveedor autenticado puede registrar servicios"));

        if (!PROVIDER_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ProviderRoleRequiredException("Solo un proveedor autenticado puede registrar servicios");
        }

        return user;
    }

    private void validateBusinessValues(ServiceRegistrationRequest request) {
        if (request.duracionMinutos() == null || request.duracionMinutos() <= 0) {
            throw new ApiException("INVALID_SERVICE_DURATION", "La duracion del servicio debe ser mayor a cero");
        }

        if (request.capacidadMaximaConcurrente() == null || request.capacidadMaximaConcurrente() <= 0) {
            throw new ApiException("INVALID_SERVICE_CAPACITY", "La capacidad maxima concurrente debe ser mayor a cero");
        }
    }
}