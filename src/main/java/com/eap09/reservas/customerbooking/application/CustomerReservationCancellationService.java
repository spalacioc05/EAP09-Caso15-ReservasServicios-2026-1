package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.ReservationCancellationFailedException;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.customerbooking.api.dto.ReservationCancellationResponse;
import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import com.eap09.reservas.customerbooking.infrastructure.BookingLifecycleProjection;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerReservationCancellationService {

    private static final Logger log = LoggerFactory.getLogger(CustomerReservationCancellationService.class);

    private static final String CLIENT_ROLE = "CLIENTE";
    private static final String RESERVATION_STATE_CATEGORY = "tbl_reserva";
    private static final String CREATED_STATE = "CREADA";
    private static final String CANCELED_STATE = "CANCELADA";
    private static final String FINALIZED_STATE = "FINALIZADA";

    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final ReservationRepository reservationRepository;
    private final SystemEventPublisher systemEventPublisher;

    public CustomerReservationCancellationService(UserAccountRepository userAccountRepository,
                                                  StateRepository stateRepository,
                                                  ReservationRepository reservationRepository,
                                                  SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.reservationRepository = reservationRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional
    public ReservationCancellationResponse cancelOwnBooking(String authenticatedUsername, Long bookingId) {
        UserAccountEntity customer = resolveAuthenticatedClient(authenticatedUsername);

        try {
            BookingLifecycleProjection booking = reservationRepository.findBookingLifecycleById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND", "La reserva indicada no existe"));

            if (!customer.getIdUsuario().equals(booking.getCustomerUserId())) {
                throw new AccessDeniedException("No tiene permisos para cancelar esta reserva");
            }

            StateEntity createdState = resolveReservationState(CREATED_STATE);
            StateEntity canceledState = resolveReservationState(CANCELED_STATE);
            StateEntity finalizedState = resolveReservationState(FINALIZED_STATE);

            if (booking.getReservationStateId().equals(canceledState.getIdEstado())) {
                throw new ReservationConflictException("BOOKING_ALREADY_CANCELED", "La reserva ya se encuentra cancelada");
            }

            if (booking.getReservationStateId().equals(finalizedState.getIdEstado())) {
                throw new ReservationConflictException("BOOKING_FINALIZED_NOT_CANCELABLE", "Una reserva finalizada no puede ser cancelada");
            }

            if (!booking.getReservationStateId().equals(createdState.getIdEstado())) {
                throw new ReservationConflictException("BOOKING_STATE_NOT_CANCELABLE", "La reserva no se encuentra en un estado cancelable");
            }

            OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime slotStartUtc = LocalDateTime.of(booking.getSlotDate(), booking.getSlotStartTime()).atOffset(ZoneOffset.UTC);
            if (!nowUtc.isBefore(slotStartUtc)) {
                throw new ReservationConflictException(
                        "BOOKING_SLOT_ALREADY_STARTED",
                        "La reserva ya no puede ser cancelada porque su franja ha iniciado");
            }

            ReservationEntity entity = reservationRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND", "La reserva indicada no existe"));

            entity.setIdEstadoReserva(canceledState.getIdEstado());
            entity.setFechaCancelacionReserva(nowUtc);
            entity.setFechaActualizacionReserva(nowUtc);
            reservationRepository.save(entity);

            publishCancellationEvent(customer.getIdUsuario(), bookingId, "EXITO", "Reserva cancelada correctamente por cliente");

            return new ReservationCancellationResponse(bookingId, CANCELED_STATE, nowUtc);
        } catch (DataAccessException ex) {
            publishCancellationEventSafely(customer.getIdUsuario(), bookingId, "FALLO", "No fue posible completar la cancelacion de la reserva");
            log.error("Error de datos al cancelar reserva {}", bookingId, ex);
            throw new ReservationCancellationFailedException(
                    "No fue posible completar la cancelacion de la reserva. Intenta nuevamente mas tarde");
        } catch (RuntimeException ex) {
            publishCancellationEventSafely(customer.getIdUsuario(), bookingId, "FALLO", ex.getMessage());
            throw ex;
        }
    }

    private UserAccountEntity resolveAuthenticatedClient(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ClientRoleRequiredException("Solo un cliente autenticado puede cancelar reservas"));

        if (!CLIENT_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ClientRoleRequiredException("Solo un cliente autenticado puede cancelar reservas");
        }

        return user;
    }

    private StateEntity resolveReservationState(String stateName) {
        return stateRepository.findByCategoryAndStateName(RESERVATION_STATE_CATEGORY, stateName)
                .orElseThrow(() -> new IllegalStateException(
                        "Required state " + stateName + " for tbl_reserva was not found"));
    }

    private void publishCancellationEvent(Long customerUserId, Long bookingId, String result, String detail) {
        systemEventPublisher.publish(SystemEvent.now(
                "CANCELACION_RESERVA",
                "tbl_reserva",
                String.valueOf(customerUserId),
                String.valueOf(bookingId),
                result,
                detail,
                TraceIdUtil.currentTraceId()));
    }

    private void publishCancellationEventSafely(Long customerUserId, Long bookingId, String result, String detail) {
        try {
            publishCancellationEvent(customerUserId, bookingId, result, detail);
        } catch (RuntimeException publishEx) {
            log.warn("No fue posible registrar evento de cancelacion para reserva {}", bookingId, publishEx);
        }
    }
}