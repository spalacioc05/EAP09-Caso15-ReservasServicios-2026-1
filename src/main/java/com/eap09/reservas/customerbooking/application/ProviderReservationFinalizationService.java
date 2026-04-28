package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.common.exception.ReservationFinalizationFailedException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.customerbooking.api.dto.ReservationFinalizationResponse;
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
public class ProviderReservationFinalizationService {

    private static final Logger log = LoggerFactory.getLogger(ProviderReservationFinalizationService.class);

    private static final String PROVIDER_ROLE = "PROVEEDOR";
    private static final String RESERVATION_STATE_CATEGORY = "tbl_reserva";
    private static final String CREATED_STATE = "CREADA";
    private static final String CANCELED_STATE = "CANCELADA";
    private static final String FINALIZED_STATE = "FINALIZADA";

    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final ReservationRepository reservationRepository;
    private final SystemEventPublisher systemEventPublisher;

    public ProviderReservationFinalizationService(UserAccountRepository userAccountRepository,
                                                  StateRepository stateRepository,
                                                  ReservationRepository reservationRepository,
                                                  SystemEventPublisher systemEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.reservationRepository = reservationRepository;
        this.systemEventPublisher = systemEventPublisher;
    }

    @Transactional
    public ReservationFinalizationResponse finalizeOwnBooking(String authenticatedUsername, Long bookingId) {
        UserAccountEntity provider = resolveAuthenticatedProvider(authenticatedUsername);

        try {
            BookingLifecycleProjection booking = reservationRepository.findBookingLifecycleById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND", "La reserva indicada no existe"));

            if (!provider.getIdUsuario().equals(booking.getProviderUserId())) {
                throw new AccessDeniedException("No tiene permisos para finalizar esta reserva");
            }

            StateEntity createdState = resolveReservationState(CREATED_STATE);
            StateEntity canceledState = resolveReservationState(CANCELED_STATE);
            StateEntity finalizedState = resolveReservationState(FINALIZED_STATE);

            if (booking.getReservationStateId().equals(finalizedState.getIdEstado())) {
                throw new ReservationConflictException("BOOKING_ALREADY_FINALIZED", "La reserva ya se encuentra finalizada");
            }

            if (booking.getReservationStateId().equals(canceledState.getIdEstado())) {
                throw new ReservationConflictException("BOOKING_CANCELED_NOT_FINALIZABLE", "Una reserva cancelada no puede ser finalizada");
            }

            if (!booking.getReservationStateId().equals(createdState.getIdEstado())) {
                throw new ReservationConflictException("BOOKING_STATE_NOT_FINALIZABLE", "La reserva no se encuentra en un estado finalizable");
            }

            OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime slotEndUtc = LocalDateTime.of(booking.getSlotDate(), booking.getSlotEndTime()).atOffset(ZoneOffset.UTC);
            if (nowUtc.isBefore(slotEndUtc)) {
                throw new ReservationConflictException(
                        "BOOKING_SLOT_NOT_FINISHED",
                        "La reserva solo puede finalizarse una vez concluida su franja");
            }

            ReservationEntity entity = reservationRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND", "La reserva indicada no existe"));

            entity.setIdEstadoReserva(finalizedState.getIdEstado());
            entity.setFechaFinalizacionReserva(nowUtc);
            entity.setFechaActualizacionReserva(nowUtc);
            reservationRepository.save(entity);

            publishFinalizationEvent(provider.getIdUsuario(), bookingId, "EXITO", "Reserva finalizada correctamente por proveedor");

            return new ReservationFinalizationResponse(bookingId, FINALIZED_STATE, nowUtc);
        } catch (DataAccessException ex) {
            publishFinalizationEventSafely(provider.getIdUsuario(), bookingId, "FALLO", "No fue posible completar la finalizacion de la reserva");
            log.error("Error de datos al finalizar reserva {}", bookingId, ex);
            throw new ReservationFinalizationFailedException(
                    "No fue posible completar la finalizacion de la reserva. Intenta nuevamente mas tarde");
        } catch (RuntimeException ex) {
            publishFinalizationEventSafely(provider.getIdUsuario(), bookingId, "FALLO", ex.getMessage());
            throw ex;
        }
    }

    private UserAccountEntity resolveAuthenticatedProvider(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ProviderRoleRequiredException("Solo un proveedor autenticado puede finalizar reservas"));

        if (!PROVIDER_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ProviderRoleRequiredException("Solo un proveedor autenticado puede finalizar reservas");
        }

        return user;
    }

    private StateEntity resolveReservationState(String stateName) {
        return stateRepository.findByCategoryAndStateName(RESERVATION_STATE_CATEGORY, stateName)
                .orElseThrow(() -> new IllegalStateException(
                        "Required state " + stateName + " for tbl_reserva was not found"));
    }

    private void publishFinalizationEvent(Long providerUserId, Long bookingId, String result, String detail) {
        systemEventPublisher.publish(SystemEvent.now(
                "FINALIZACION_RESERVA",
                "tbl_reserva",
                String.valueOf(providerUserId),
                String.valueOf(bookingId),
                result,
                detail,
                TraceIdUtil.currentTraceId()));
    }

    private void publishFinalizationEventSafely(Long providerUserId, Long bookingId, String result, String detail) {
        try {
            publishFinalizationEvent(providerUserId, bookingId, result, detail);
        } catch (RuntimeException publishEx) {
            log.warn("No fue posible registrar evento de finalizacion para reserva {}", bookingId, publishEx);
        }
    }
}