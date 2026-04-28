package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ProviderReservationQueryFailedException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.customerbooking.api.dto.ProviderBookingResponse;
import com.eap09.reservas.customerbooking.infrastructure.ProviderBookingProjection;
import com.eap09.reservas.customerbooking.infrastructure.ProviderBookingQueryRepository;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderBookingQueryService {

    private static final Logger log = LoggerFactory.getLogger(ProviderBookingQueryService.class);

    private static final String PROVIDER_ROLE = "PROVEEDOR";
    private static final String RESERVATION_STATE_CATEGORY = "tbl_reserva";
    private static final String PROVIDER_BOOKING_QUERY_EVENT = "CONSULTA_RESERVAS_PROVEEDOR";

    private final ProviderBookingQueryRepository providerBookingQueryRepository;
    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final ServiceRepository serviceRepository;
    private final SystemEventPublisher systemEventPublisher;

    public ProviderBookingQueryService(ProviderBookingQueryRepository providerBookingQueryRepository,
                                       UserAccountRepository userAccountRepository,
                                       StateRepository stateRepository,
                                       ServiceRepository serviceRepository,
                                       SystemEventPublisher systemEventPublisher) {
        this.providerBookingQueryRepository = providerBookingQueryRepository;
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.serviceRepository = serviceRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional(readOnly = true, noRollbackFor = {
            ProviderRoleRequiredException.class,
            ResourceNotFoundException.class,
            AccessDeniedException.class,
            ApiException.class
    })
    public ProviderBookingQueryResult getOwnBookings(String authenticatedUsername,
                                                     LocalDate date,
                                                     String status,
                                                     Long serviceId) {
        UserAccountEntity provider = resolveAuthenticatedProvider(authenticatedUsername);
        Long providerUserId = provider.getIdUsuario();

        try {
            validateServiceOwnershipIfPresent(providerUserId, serviceId);
            Long reservationStateId = resolveReservationStateId(status);

            List<ProviderBookingResponse> bookings = providerBookingQueryRepository.findProviderBookings(
                            providerUserId,
                            serviceId,
                            date,
                            reservationStateId)
                    .stream()
                    .map(this::toResponse)
                    .toList();

            boolean hasAnyReservation = providerBookingQueryRepository.existsAnyReservationForProvider(providerUserId);
            String message = resolveMessage(bookings, hasAnyReservation);

            publishEvent(providerUserId, "EXITO", buildSuccessDetail(date, status, serviceId, bookings.size()));

            return new ProviderBookingQueryResult(message, bookings);
        } catch (DataAccessException ex) {
            publishEventSafely(providerUserId, "FALLO", "No fue posible completar la consulta operativa de reservas");
            log.error("Error de datos al consultar reservas operativas del proveedor {}", providerUserId, ex);
            throw new ProviderReservationQueryFailedException(
                    "No fue posible completar la consulta de reservas. Intenta nuevamente mas tarde");
        } catch (RuntimeException ex) {
            publishEventSafely(providerUserId, "FALLO", resolveFailureDetail(ex));
            throw ex;
        }
    }

    private UserAccountEntity resolveAuthenticatedProvider(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ProviderRoleRequiredException(
                        "Solo un proveedor autenticado puede consultar reservas operativas"));

        if (!PROVIDER_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ProviderRoleRequiredException("Solo un proveedor autenticado puede consultar reservas operativas");
        }

        return user;
    }

    private void validateServiceOwnershipIfPresent(Long providerUserId, Long serviceId) {
        if (serviceId == null) {
            return;
        }

        ServiceEntity service = serviceRepository.findByIdServicio(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("SERVICE_NOT_FOUND", "El servicio indicado no existe"));

        if (!providerUserId.equals(service.getIdUsuarioProveedor())) {
            throw new AccessDeniedException("No tiene permisos para consultar reservas de este servicio");
        }
    }

    private Long resolveReservationStateId(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        StateEntity state = stateRepository.findByCategoryAndStateName(RESERVATION_STATE_CATEGORY, normalizedStatus)
                .orElseThrow(() -> new ApiException(
                        "INVALID_RESERVATION_STATUS",
                        "El estado de reserva consultado no es valido"));

        return state.getIdEstado();
    }

    private ProviderBookingResponse toResponse(ProviderBookingProjection projection) {
        return new ProviderBookingResponse(
                projection.getBookingId(),
                projection.getServiceId(),
                projection.getServiceName(),
                projection.getAvailabilityId(),
                projection.getSlotDate(),
                projection.getStartTime(),
                projection.getEndTime(),
                projection.getCustomerId(),
                projection.getCustomerFullName(),
                projection.getCustomerEmail(),
                projection.getBookingStatus(),
                projection.getCreatedAt());
    }

    private String resolveMessage(List<ProviderBookingResponse> bookings, boolean hasAnyReservation) {
        if (!bookings.isEmpty()) {
            return "Consulta operativa de reservas exitosa";
        }

        if (hasAnyReservation) {
            return "No existen reservas que cumplan con los filtros aplicados";
        }

        return "No existen reservas registradas para sus servicios";
    }

    private String buildSuccessDetail(LocalDate date, String status, Long serviceId, int resultSize) {
        return "Consulta reservas proveedor date=" + date
                + " status=" + status
                + " serviceId=" + serviceId
                + " resultados=" + resultSize;
    }

    private String resolveFailureDetail(RuntimeException ex) {
        if (ex instanceof ApiException
                || ex instanceof ProviderRoleRequiredException
                || ex instanceof ResourceNotFoundException) {
            return ex.getMessage();
        }

        if (ex instanceof AccessDeniedException) {
            return "Intento de consulta de reservas sobre servicio ajeno";
        }

        return "No fue posible completar la consulta operativa de reservas";
    }

    private void publishEvent(Long providerUserId, String result, String details) {
        systemEventPublisher.publish(SystemEvent.now(
                PROVIDER_BOOKING_QUERY_EVENT,
                "tbl_reserva",
                String.valueOf(providerUserId),
                String.valueOf(providerUserId),
                result,
                details,
                TraceIdUtil.currentTraceId()));
    }

    private void publishEventSafely(Long providerUserId, String result, String details) {
        try {
            publishEvent(providerUserId, result, details);
        } catch (RuntimeException publishFailure) {
            log.warn("No fue posible registrar evento de consulta de reservas de proveedor {}", providerUserId, publishFailure);
        }
    }
}