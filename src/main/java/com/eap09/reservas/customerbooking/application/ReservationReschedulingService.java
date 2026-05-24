package com.eap09.reservas.customerbooking.application;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.common.exception.ReservationReschedulingFailedException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.util.TraceIdUtil;
import com.eap09.reservas.customerbooking.api.dto.ReservationReschedulingRequest;
import com.eap09.reservas.customerbooking.api.dto.ReservationReschedulingResponse;
import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import com.eap09.reservas.customerbooking.infrastructure.BookingLifecycleProjection;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.customerbooking.infrastructure.ServicesAvailabilityRepository;
import com.eap09.reservas.customerbooking.infrastructure.ServicesRepository;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationReschedulingService {

    private static final Logger log = LoggerFactory.getLogger(ReservationReschedulingService.class);

    private static final String CLIENT_ROLE = "CLIENTE";
    private static final String RESERVATION_STATE_CATEGORY = "tbl_reserva";
    private static final String AVAILABILITY_STATE_CATEGORY = "tbl_disponibilidad_servicio";
    private static final String CREATED_STATE = "CREADA";
    private static final String ENABLED_AVAILABILITY_STATE = "HABILITADA";
    private static final String RESCHEDULING_EVENT = "REPROGRAMACION_RESERVA";
    private static final String TOO_LATE_MESSAGE =
            "No es posible reprogramar una reserva con menos de 24 horas de antelacion";
    private static final String SLOT_UNAVAILABLE_MESSAGE =
            "No es posible reprogramar la reserva porque la fecha elegida no esta disponible";
    private static final String SAME_AVAILABILITY_MESSAGE =
            "La reserva ya se encuentra asociada a la disponibilidad seleccionada";
    private static final String NO_CAPACITY_MESSAGE =
            "No es posible reprogramar la reserva porque la disponibilidad seleccionada no tiene cupos";
    private static final String DIFFERENT_SERVICE_MESSAGE =
            "No es posible reprogramar la reserva porque la disponibilidad seleccionada no corresponde al servicio reservado";
    private static final String NOT_ACTIVE_MESSAGE = "Solo es posible reprogramar reservas activas";
    private static final String FAILED_MESSAGE =
            "No fue posible completar la reprogramacion de la reserva. Intenta nuevamente mas tarde";

    private final UserAccountRepository userAccountRepository;
    private final StateRepository stateRepository;
    private final ReservationRepository reservationRepository;
    private final ServicesAvailabilityRepository servicesAvailabilityRepository;
    private final ServicesRepository servicesRepository;
    private final SystemEventPublisher systemEventPublisher;
    private final Clock clock;

    public ReservationReschedulingService(UserAccountRepository userAccountRepository,
                                          StateRepository stateRepository,
                                          ReservationRepository reservationRepository,
                                          ServicesAvailabilityRepository servicesAvailabilityRepository,
                                          ServicesRepository servicesRepository,
                                          SystemEventPublisher systemEventPublisher,
                                          Clock clock) {
        this.userAccountRepository = userAccountRepository;
        this.stateRepository = stateRepository;
        this.reservationRepository = reservationRepository;
        this.servicesAvailabilityRepository = servicesAvailabilityRepository;
        this.servicesRepository = servicesRepository;
        this.systemEventPublisher = systemEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ReservationReschedulingResponse rescheduleOwnBooking(String authenticatedUsername,
                                                                Long bookingId,
                                                                ReservationReschedulingRequest request) {
        UserAccountEntity customer = resolveAuthenticatedClient(authenticatedUsername);

        try {
            BookingLifecycleProjection booking = findReservationLifecycle(bookingId);
            ensureReservationBelongsToClient(customer.getIdUsuario(), booking);

            StateEntity createdState = resolveState(RESERVATION_STATE_CATEGORY, CREATED_STATE);
            StateEntity enabledAvailabilityState = resolveState(AVAILABILITY_STATE_CATEGORY, ENABLED_AVAILABILITY_STATE);

            ensureReservationIsActive(booking, createdState.getIdEstado());

            OffsetDateTime now = OffsetDateTime.now(clock);
            ensureReschedulingWindow(booking, now);

            ReservationEntity reservation = findReservationEntity(bookingId);
            Long previousAvailabilityId = reservation.getIdDisponibilidadServicio();
            ensureTargetAvailabilityIsDifferent(previousAvailabilityId, request.availabilityId());

            ServiceAvailabilityEntity targetAvailability = findTargetAvailability(request.availabilityId());
            ensureTargetAvailabilityIsEnabled(targetAvailability, enabledAvailabilityState.getIdEstado());
            ensureSameService(booking.getServiceId(), targetAvailability.getIdServicio());
            ensureTargetAvailabilityIsFuture(targetAvailability, now);

            ServiceEntity reservedService = findReservedService(booking.getServiceId());
            ensureCapacityAvailable(targetAvailability, reservedService, createdState.getIdEstado());

            applyRescheduling(reservation, targetAvailability, now);
            ReservationEntity updatedReservation = reservationRepository.save(reservation);

            publishResultEvent(
                    customer.getIdUsuario(),
                    bookingId,
                    "EXITO",
                    buildSuccessDetail(
                        customer.getIdUsuario(),
                        booking,
                        previousAvailabilityId,
                        updatedReservation.getIdDisponibilidadServicio(),
                        targetAvailability));

                return buildResponse(booking, previousAvailabilityId, targetAvailability, updatedReservation);
        } catch (DataAccessException ex) {
            publishResultEventSafely(
                    customer.getIdUsuario(),
                    bookingId,
                    "FALLO",
                    "No fue posible completar la reprogramacion de la reserva");
            log.error("Error de datos al reprogramar reserva {}", bookingId, ex);
            throw new ReservationReschedulingFailedException(FAILED_MESSAGE);
        } catch (RuntimeException ex) {
            publishResultEventSafely(customer.getIdUsuario(), bookingId, "FALLO", resolveFailureDetail(ex));
            throw ex;
        }
    }

    private UserAccountEntity resolveAuthenticatedClient(String authenticatedUsername) {
        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(authenticatedUsername)
                .orElseThrow(() -> new ClientRoleRequiredException("Solo un cliente autenticado puede reprogramar reservas"));

        if (!CLIENT_ROLE.equalsIgnoreCase(user.getRol().getNombreRol())) {
            throw new ClientRoleRequiredException("Solo un cliente autenticado puede reprogramar reservas");
        }

        return user;
    }

    private BookingLifecycleProjection findReservationLifecycle(Long bookingId) {
        return reservationRepository.findBookingLifecycleById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND", "La reserva indicada no existe"));
    }

    private ReservationEntity findReservationEntity(Long bookingId) {
        return reservationRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND", "La reserva indicada no existe"));
    }

    private ServiceAvailabilityEntity findTargetAvailability(Long availabilityId) {
        return servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("AVAILABILITY_NOT_FOUND", "La disponibilidad indicada no existe"));
    }

    private ServiceEntity findReservedService(Long serviceId) {
        return servicesRepository.findByIdServicio(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("SERVICE_NOT_FOUND", "El servicio asociado a la reserva no existe"));
    }

    private StateEntity resolveState(String categoryName, String stateName) {
        return stateRepository.findByCategoryAndStateName(categoryName, stateName)
                .orElseThrow(() -> new IllegalStateException(
                        "Required state " + stateName + " for " + categoryName + " was not found"));
    }

    private void ensureReservationBelongsToClient(Long customerUserId, BookingLifecycleProjection booking) {
        if (!customerUserId.equals(booking.getCustomerUserId())) {
            throw new AccessDeniedException("No tienes permisos para reprogramar esta reserva");
        }
    }

    private void ensureReservationIsActive(BookingLifecycleProjection booking, Long createdStateId) {
        if (!createdStateId.equals(booking.getReservationStateId())) {
            throw new ReservationConflictException("RESERVATION_RESCHEDULING_NOT_ALLOWED", NOT_ACTIVE_MESSAGE);
        }
    }

    private void ensureReschedulingWindow(BookingLifecycleProjection booking, OffsetDateTime now) {
        OffsetDateTime currentSlotStart = buildSlotStart(booking.getSlotDate(), booking.getSlotStartTime());
        Duration timeUntilCurrentSlot = Duration.between(now, currentSlotStart);
        if (timeUntilCurrentSlot.compareTo(Duration.ofHours(24)) < 0) {
            throw new ReservationConflictException("RESERVATION_RESCHEDULING_TOO_LATE", TOO_LATE_MESSAGE);
        }
    }

    private void ensureTargetAvailabilityIsDifferent(Long currentAvailabilityId, Long targetAvailabilityId) {
        if (currentAvailabilityId.equals(targetAvailabilityId)) {
            throw new ReservationConflictException("RESERVATION_RESCHEDULING_SAME_AVAILABILITY", SAME_AVAILABILITY_MESSAGE);
        }
    }

    private void ensureTargetAvailabilityIsEnabled(ServiceAvailabilityEntity targetAvailability, Long enabledStateId) {
        if (!enabledStateId.equals(targetAvailability.getIdEstadoDisponibilidad())) {
            throw new ReservationConflictException("RESERVATION_RESCHEDULING_SLOT_UNAVAILABLE", SLOT_UNAVAILABLE_MESSAGE);
        }
    }

    private void ensureSameService(Long expectedServiceId, Long targetServiceId) {
        if (!expectedServiceId.equals(targetServiceId)) {
            throw new ReservationConflictException("RESERVATION_RESCHEDULING_DIFFERENT_SERVICE", DIFFERENT_SERVICE_MESSAGE);
        }
    }

    private void ensureTargetAvailabilityIsFuture(ServiceAvailabilityEntity targetAvailability, OffsetDateTime now) {
        OffsetDateTime targetSlotStart = buildSlotStart(targetAvailability.getFechaDisponibilidad(), targetAvailability.getHoraInicio());
        if (!targetSlotStart.isAfter(now)) {
            throw new ReservationConflictException("RESERVATION_RESCHEDULING_SLOT_UNAVAILABLE", SLOT_UNAVAILABLE_MESSAGE);
        }
    }

    private void ensureCapacityAvailable(ServiceAvailabilityEntity targetAvailability,
                                         ServiceEntity reservedService,
                                         Long createdStateId) {
        long activeBookings = reservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(
                targetAvailability.getIdDisponibilidadServicio(),
                createdStateId);

        if (activeBookings >= reservedService.getCapacidadMaximaConcurrente()) {
            throw new ReservationConflictException("RESERVATION_RESCHEDULING_NO_CAPACITY", NO_CAPACITY_MESSAGE);
        }
    }

    private void applyRescheduling(ReservationEntity reservation,
                                   ServiceAvailabilityEntity targetAvailability,
                                   OffsetDateTime now) {
        reservation.setIdDisponibilidadServicio(targetAvailability.getIdDisponibilidadServicio());
        reservation.setFechaActualizacionReserva(now);
    }

    private ReservationReschedulingResponse buildResponse(BookingLifecycleProjection booking,
                                                          Long previousAvailabilityId,
                                                          ServiceAvailabilityEntity targetAvailability,
                                                          ReservationEntity updatedReservation) {
        return new ReservationReschedulingResponse(
                updatedReservation.getIdReserva(),
                previousAvailabilityId,
                targetAvailability.getIdDisponibilidadServicio(),
                booking.getSlotDate(),
                booking.getSlotStartTime(),
                booking.getSlotEndTime(),
                targetAvailability.getFechaDisponibilidad(),
                targetAvailability.getHoraInicio(),
                targetAvailability.getHoraFin(),
                booking.getReservationStateName(),
                updatedReservation.getFechaActualizacionReserva());
    }

    private OffsetDateTime buildSlotStart(java.time.LocalDate date, java.time.LocalTime startTime) {
        return LocalDateTime.of(date, startTime).atZone(clock.getZone()).toOffsetDateTime();
    }

    private String buildSuccessDetail(Long customerUserId,
                                      BookingLifecycleProjection booking,
                                      Long previousAvailabilityId,
                                      Long newAvailabilityId,
                                      ServiceAvailabilityEntity targetAvailability) {
        return "{"
                + "\"bookingId\":" + booking.getBookingId()
                + ",\"customerUserId\":" + customerUserId
                + ",\"previousAvailabilityId\":" + previousAvailabilityId
                + ",\"newAvailabilityId\":" + newAvailabilityId
                + ",\"previousDate\":\"" + booking.getSlotDate() + "\""
                + ",\"previousStartTime\":\"" + booking.getSlotStartTime() + "\""
                + ",\"previousEndTime\":\"" + booking.getSlotEndTime() + "\""
                + ",\"newDate\":\"" + targetAvailability.getFechaDisponibilidad() + "\""
                + ",\"newStartTime\":\"" + targetAvailability.getHoraInicio() + "\""
                + ",\"newEndTime\":\"" + targetAvailability.getHoraFin() + "\""
                + "}";
    }

    private String resolveFailureDetail(RuntimeException ex) {
        if (ex instanceof ReservationConflictException
                || ex instanceof ResourceNotFoundException
                || ex instanceof ClientRoleRequiredException
                || ex instanceof AccessDeniedException) {
            return ex.getMessage();
        }

        return "No fue posible completar la reprogramacion de la reserva";
    }

    private void publishResultEvent(Long customerUserId, Long bookingId, String result, String detail) {
        systemEventPublisher.publish(SystemEvent.now(
                RESCHEDULING_EVENT,
                RESERVATION_STATE_CATEGORY,
                String.valueOf(customerUserId),
                String.valueOf(bookingId),
                result,
                detail,
                TraceIdUtil.currentTraceId()));
    }

    private void publishResultEventSafely(Long customerUserId, Long bookingId, String result, String detail) {
        try {
            publishResultEvent(customerUserId, bookingId, result, detail);
        } catch (RuntimeException publishEx) {
            log.warn("No fue posible registrar evento de reprogramacion para reserva {}", bookingId, publishEx);
        }
    }
}