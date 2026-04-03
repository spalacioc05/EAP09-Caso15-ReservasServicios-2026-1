package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.common.exception.ReservationCreationFailedException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationRequest;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationResponse;
import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.customerbooking.infrastructure.ServicesAvailabilityRepository;
import com.eap09.reservas.customerbooking.infrastructure.ServicesRepository;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private static final String CLIENT_ROLE = "CLIENTE";
    private static final String PROVIDER_ROLE = "PROVEEDOR";

    private static final String USER_STATE_CATEGORY = "tbl_usuario";
    private static final String SERVICE_STATE_CATEGORY = "tbl_servicio";
    private static final String AVAILABILITY_STATE_CATEGORY = "tbl_disponibilidad_servicio";
    private static final String RESERVATION_STATE_CATEGORY = "tbl_reserva";

    private static final String ACTIVE_USER_STATE = "ACTIVA";
    private static final String ACTIVE_SERVICE_STATE = "ACTIVO";
    private static final String ENABLED_AVAILABILITY_STATE = "HABILITADA";
    private static final String CREATED_RESERVATION_STATE = "CREADA";

    private final ReservationRepository reservationRepository;
    private final ServicesAvailabilityRepository availabilityRepository;
    private final ServicesRepository servicesRepository;
    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final SystemEventPublisher systemEventPublisher;

    public ReservationService(
            ReservationRepository reservationRepository,
            ServicesAvailabilityRepository availabilityRepository,
            ServicesRepository servicesRepository,
            UserAccountRepository userAccountRepository,
            StateRepository stateRepository,
            SystemEventPublisher systemEventPublisher
    ) {
        this.reservationRepository = reservationRepository;
        this.availabilityRepository = availabilityRepository;
        this.servicesRepository = servicesRepository;
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional
    public CreateReservationResponse createReservation(CreateReservationRequest request, String authenticatedUsername) {
        validateRequiredFields(request);

        UserAccountEntity customer = resolveAuthenticatedClient(authenticatedUsername);
        StateEntity activeUserState = resolveState(USER_STATE_CATEGORY, ACTIVE_USER_STATE);
        StateEntity activeServiceState = resolveState(SERVICE_STATE_CATEGORY, ACTIVE_SERVICE_STATE);
        StateEntity enabledAvailabilityState = resolveState(AVAILABILITY_STATE_CATEGORY, ENABLED_AVAILABILITY_STATE);
        StateEntity createdReservationState = resolveState(RESERVATION_STATE_CATEGORY, CREATED_RESERVATION_STATE);

        try {
            UserAccountEntity provider = userAccountRepository.findById(request.providerId())
                    .orElse(null);

            if (!isActiveProvider(provider, activeUserState.getIdEstado())) {
                throw new ReservationConflictException("PROVIDER_NOT_AVAILABLE", "El proveedor no esta disponible");
            }

            ServiceEntity service = servicesRepository.findByIdServicio(request.serviceId())
                    .orElse(null);

            if (!isActiveServiceForProvider(service, provider.getIdUsuario(), activeServiceState.getIdEstado())) {
                throw new ReservationConflictException("SERVICE_NOT_AVAILABLE", "El servicio no esta disponible");
            }

            ServiceAvailabilityEntity availability = availabilityRepository
                    .findByIdDisponibilidadServicioForUpdate(request.availabilityId())
                    .orElse(null);

            if (!isReservableAvailability(availability, service.getIdServicio(), enabledAvailabilityState.getIdEstado())) {
                throw new ReservationConflictException(
                        "AVAILABILITY_NOT_RESERVABLE",
                        "La franja seleccionada ya no puede reservarse");
            }

            long currentReservations = reservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(
                    availability.getIdDisponibilidadServicio(),
                    createdReservationState.getIdEstado());

            if (currentReservations >= service.getCapacidadMaximaConcurrente()) {
                throw new ReservationConflictException(
                        "AVAILABILITY_CAPACITY_EXHAUSTED",
                        "La franja seleccionada no tiene cupos disponibles");
            }

            ReservationEntity booking = new ReservationEntity();
            booking.setIdDisponibilidadServicio(availability.getIdDisponibilidadServicio());
            booking.setIdUsuarioCliente(customer.getIdUsuario());
            booking.setIdEstadoReserva(createdReservationState.getIdEstado());
            booking.setFechaCreacionReserva(OffsetDateTime.now());

            ReservationEntity savedBooking = reservationRepository.save(booking);

            systemEventPublisher.publish(SystemEvent.now(
                    "CREACION_RESERVA",
                    "tbl_reserva",
                    String.valueOf(customer.getIdUsuario()),
                    "EXITO",
                    "Reserva creada con id " + savedBooking.getIdReserva(),
                    TraceIdUtil.currentTraceId()));

            return new CreateReservationResponse(
                    savedBooking.getIdReserva(),
                    provider.getIdUsuario(),
                    service.getIdServicio(),
                    availability.getIdDisponibilidadServicio(),
                    customer.getIdUsuario(),
                    availability.getFechaDisponibilidad(),
                    createdReservationState.getNombreEstado(),
                    savedBooking.getFechaCreacionReserva());
        } catch (DataAccessException ex) {
            log.error("Error de datos al crear reserva", ex);
            throw new ReservationCreationFailedException("No fue posible completar la reserva. Intenta nuevamente mas tarde");
        }
    }

    private void validateRequiredFields(CreateReservationRequest request) {
        if (request == null
                || request.providerId() == null
                || request.serviceId() == null
                || request.availabilityId() == null) {
            throw new ApiException("REQUIRED_FIELDS_MISSING", "Proveedor, servicio y franja son requeridos");
        }
    }

    private UserAccountEntity resolveAuthenticatedClient(String authenticatedUsername) {
        UserAccountEntity customer = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ClientRoleRequiredException("Solo un cliente autenticado puede crear reservas"));

        if (!CLIENT_ROLE.equalsIgnoreCase(customer.getRol().getNombreRol())) {
            throw new ClientRoleRequiredException("Solo un cliente autenticado puede crear reservas");
        }

        return customer;
    }

    private StateEntity resolveState(String categoryName, String stateName) {
        return stateRepository.findByCategoryAndStateName(categoryName, stateName)
                .orElseThrow(() -> new IllegalStateException(
                        "Required state " + stateName + " for " + categoryName + " was not found"));
    }

    private boolean isActiveProvider(UserAccountEntity provider, Long activeProviderStateId) {
        return provider != null
                && provider.getIdEstado().equals(activeProviderStateId)
                && provider.getRol() != null
                && PROVIDER_ROLE.equalsIgnoreCase(provider.getRol().getNombreRol());
    }

    private boolean isActiveServiceForProvider(ServiceEntity service, Long providerId, Long activeServiceStateId) {
        return service != null
                && service.getIdUsuarioProveedor().equals(providerId)
                && service.getIdEstadoServicio().equals(activeServiceStateId);
    }

    private boolean isReservableAvailability(ServiceAvailabilityEntity availability,
                                             Long serviceId,
                                             Long enabledAvailabilityStateId) {
        return availability != null
                && availability.getIdServicio().equals(serviceId)
                && availability.getIdEstadoDisponibilidad().equals(enabledAvailabilityStateId);
    }
}