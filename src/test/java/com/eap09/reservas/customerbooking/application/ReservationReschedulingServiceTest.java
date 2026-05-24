package com.eap09.reservas.customerbooking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.common.exception.ReservationReschedulingFailedException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.customerbooking.api.dto.ReservationReschedulingRequest;
import com.eap09.reservas.customerbooking.api.dto.ReservationReschedulingResponse;
import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import com.eap09.reservas.customerbooking.infrastructure.BookingLifecycleProjection;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.customerbooking.infrastructure.ServicesAvailabilityRepository;
import com.eap09.reservas.customerbooking.infrastructure.ServicesRepository;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ReservationReschedulingServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ServicesAvailabilityRepository servicesAvailabilityRepository;

    @Mock
    private ServicesRepository servicesRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @Test
    void shouldRescheduleOwnCreatedBookingAndPreserveOtherFields() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        ReservationEntity reservation = reservationEntity(101L, 700L, 50L, 7L);

        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 3), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(701L))
                .thenReturn(Optional.of(availability(701L, 300L, 20L,
                        LocalDate.of(2026, 5, 5), LocalTime.of(11, 0), LocalTime.of(12, 0))));
        when(servicesRepository.findByIdServicio(300L)).thenReturn(Optional.of(serviceEntity(300L, 2)));
        when(reservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(701L, 7L)).thenReturn(1L);
        when(reservationRepository.save(any(ReservationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationReschedulingResponse result = service.rescheduleOwnBooking(
                "customer@test.local",
                101L,
                new ReservationReschedulingRequest(701L));

        assertEquals(101L, result.bookingId());
        assertEquals(700L, result.previousAvailabilityId());
        assertEquals(701L, result.newAvailabilityId());
        assertEquals(LocalDate.of(2026, 5, 3), result.previousDate());
        assertEquals(LocalDate.of(2026, 5, 5), result.newDate());
        assertEquals("CREADA", result.bookingStatus());
        assertNotNull(result.updatedAt());
        assertEquals(701L, reservation.getIdDisponibilidadServicio());
        assertEquals(50L, reservation.getIdUsuarioCliente());
        assertEquals(7L, reservation.getIdEstadoReserva());
        assertEquals(OffsetDateTime.parse("2026-05-01T10:00:00Z"), reservation.getFechaActualizacionReserva());
        verify(reservationRepository).save(reservation);
        verify(systemEventPublisher).publish(argThat(event ->
                "REPROGRAMACION_RESERVA".equals(event.type())
                        && "EXITO".equals(event.result())
                        && event.details().contains("\"previousAvailabilityId\":700")
                        && event.details().contains("\"newAvailabilityId\":701")));
    }

    @Test
    void shouldAllowReschedulingWhenCurrentBookingStartsInExactlyTwentyFourHours() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        ReservationEntity reservation = reservationEntity(101L, 700L, 50L, 7L);

        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 2), LocalTime.of(10, 0), LocalTime.of(11, 0))));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(701L))
                .thenReturn(Optional.of(availability(701L, 300L, 20L,
                        LocalDate.of(2026, 5, 4), LocalTime.of(11, 0), LocalTime.of(12, 0))));
        when(servicesRepository.findByIdServicio(300L)).thenReturn(Optional.of(serviceEntity(300L, 1)));
        when(reservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(701L, 7L)).thenReturn(0L);
        when(reservationRepository.save(any(ReservationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationReschedulingResponse result = service.rescheduleOwnBooking(
                "customer@test.local",
                101L,
                new ReservationReschedulingRequest(701L));

        assertEquals(701L, result.newAvailabilityId());
        verify(systemEventPublisher).publish(argThat(event -> "EXITO".equals(event.result())));
    }

    @Test
    void shouldRejectWhenBookingDoesNotExist() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        stubClient("customer@test.local", 50L, "CLIENTE");
        when(reservationRepository.findBookingLifecycleById(101L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        assertEquals("BOOKING_NOT_FOUND", ex.getErrorCode());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void shouldRejectWhenBookingDoesNotBelongToAuthenticatedClient() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        stubClient("customer@test.local", 50L, "CLIENTE");
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 99L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 3), LocalTime.of(9, 0), LocalTime.of(10, 0))));

        assertThrows(AccessDeniedException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotClient() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        stubClient("provider@test.local", 50L, "PROVEEDOR");

        assertThrows(ClientRoleRequiredException.class,
                () -> service.rescheduleOwnBooking("provider@test.local", 101L, new ReservationReschedulingRequest(701L)));

        verifyNoInteractions(systemEventPublisher);
    }

    @Test
    void shouldRejectWhenBookingIsCanceled() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 8L, "CANCELADA",
                        LocalDate.of(2026, 5, 3), LocalTime.of(9, 0), LocalTime.of(10, 0))));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        assertEquals("RESERVATION_RESCHEDULING_NOT_ALLOWED", ex.getErrorCode());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
    }

    @Test
    void shouldRejectWhenBookingIsFinalized() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 9L, "FINALIZADA",
                        LocalDate.of(2026, 5, 3), LocalTime.of(9, 0), LocalTime.of(10, 0))));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        assertEquals("RESERVATION_RESCHEDULING_NOT_ALLOWED", ex.getErrorCode());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
    }

    @Test
    void shouldRejectWhenThereAreLessThanTwentyFourHoursBeforeCurrentSlot() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 2), LocalTime.of(9, 59), LocalTime.of(10, 59))));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        assertEquals("RESERVATION_RESCHEDULING_TOO_LATE", ex.getErrorCode());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
    }

    @Test
    void shouldRejectWhenTargetAvailabilityDoesNotExist() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        ReservationEntity reservation = reservationEntity(101L, 700L, 50L, 7L);

        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 3), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(701L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        assertEquals("AVAILABILITY_NOT_FOUND", ex.getErrorCode());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
    }

    @Test
    void shouldRejectWhenTargetAvailabilityIsBlocked() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        ReservationEntity reservation = reservationEntity(101L, 700L, 50L, 7L);

        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 3), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(701L))
                .thenReturn(Optional.of(availability(701L, 300L, 21L,
                        LocalDate.of(2026, 5, 5), LocalTime.of(11, 0), LocalTime.of(12, 0))));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        assertEquals("RESERVATION_RESCHEDULING_SLOT_UNAVAILABLE", ex.getErrorCode());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
    }

    @Test
    void shouldRejectWhenTargetAvailabilityBelongsToDifferentService() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        ReservationEntity reservation = reservationEntity(101L, 700L, 50L, 7L);

        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 3), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(701L))
                .thenReturn(Optional.of(availability(701L, 301L, 20L,
                        LocalDate.of(2026, 5, 5), LocalTime.of(11, 0), LocalTime.of(12, 0))));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        assertEquals("RESERVATION_RESCHEDULING_DIFFERENT_SERVICE", ex.getErrorCode());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
    }

    @Test
    void shouldRejectWhenTargetAvailabilityMatchesCurrentAvailability() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        ReservationEntity reservation = reservationEntity(101L, 700L, 50L, 7L);

        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 3), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(700L)));

        assertEquals("RESERVATION_RESCHEDULING_SAME_AVAILABILITY", ex.getErrorCode());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
        verify(systemEventPublisher, never()).publish(argThat(event -> "EXITO".equals(event.result())));
    }

    @Test
    void shouldRejectWhenTargetAvailabilityHasNoCapacity() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        ReservationEntity reservation = reservationEntity(101L, 700L, 50L, 7L);

        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 3), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(701L))
                .thenReturn(Optional.of(availability(701L, 300L, 20L,
                        LocalDate.of(2026, 5, 5), LocalTime.of(11, 0), LocalTime.of(12, 0))));
        when(servicesRepository.findByIdServicio(300L)).thenReturn(Optional.of(serviceEntity(300L, 1)));
        when(reservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(701L, 7L)).thenReturn(1L);

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        assertEquals("RESERVATION_RESCHEDULING_NO_CAPACITY", ex.getErrorCode());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void shouldRejectWhenTargetAvailabilityStartsInThePast() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        ReservationEntity reservation = reservationEntity(101L, 700L, 50L, 7L);

        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 4), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(701L))
                .thenReturn(Optional.of(availability(701L, 300L, 20L,
                        LocalDate.of(2026, 5, 1), LocalTime.of(9, 0), LocalTime.of(10, 0))));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        assertEquals("RESERVATION_RESCHEDULING_SLOT_UNAVAILABLE", ex.getErrorCode());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
    }

    @Test
    void shouldTranslatePersistenceErrorToControlledException() {
        ReservationReschedulingService service = serviceAt("2026-05-01T10:00:00Z");
        ReservationEntity reservation = reservationEntity(101L, 700L, 50L, 7L);

        stubClient("customer@test.local", 50L, "CLIENTE");
        stubCreatedReservationStates();
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 300L, 90L, 50L, 7L, "CREADA",
                        LocalDate.of(2026, 5, 3), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservation));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(701L))
                .thenReturn(Optional.of(availability(701L, 300L, 20L,
                        LocalDate.of(2026, 5, 5), LocalTime.of(11, 0), LocalTime.of(12, 0))));
        when(servicesRepository.findByIdServicio(300L)).thenReturn(Optional.of(serviceEntity(300L, 2)));
        when(reservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(701L, 7L)).thenReturn(0L);
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenThrow(new DataAccessResourceFailureException("db unavailable"));

        ReservationReschedulingFailedException ex = assertThrows(ReservationReschedulingFailedException.class,
                () -> service.rescheduleOwnBooking("customer@test.local", 101L, new ReservationReschedulingRequest(701L)));

        assertEquals("No fue posible completar la reprogramacion de la reserva. Intenta nuevamente mas tarde", ex.getMessage());
        verify(systemEventPublisher).publish(argThat(event -> "FALLO".equals(event.result())));
    }

    private ReservationReschedulingService serviceAt(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new ReservationReschedulingService(
                userAccountRepository,
                stateRepository,
                reservationRepository,
                servicesAvailabilityRepository,
                servicesRepository,
                systemEventPublisher,
                clock);
    }

    private void stubClient(String email, Long userId, String roleName) {
        RoleEntity role = new RoleEntity();
        role.setNombreRol(roleName);

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(userId);
        user.setCorreoUsuario(email);
        user.setRol(role);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(email)).thenReturn(Optional.of(user));
    }

    private void stubCreatedReservationStates() {
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(state(7L, "CREADA")));
        when(stateRepository.findByCategoryAndStateName("tbl_disponibilidad_servicio", "HABILITADA"))
                .thenReturn(Optional.of(state(20L, "HABILITADA")));
    }

    private StateEntity state(Long id, String name) {
        StateEntity state = new StateEntity();
        state.setIdEstado(id);
        state.setNombreEstado(name);
        return state;
    }

    private ReservationEntity reservationEntity(Long bookingId, Long availabilityId, Long customerId, Long stateId) {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setIdReserva(bookingId);
        reservation.setIdDisponibilidadServicio(availabilityId);
        reservation.setIdUsuarioCliente(customerId);
        reservation.setIdEstadoReserva(stateId);
        reservation.setFechaCreacionReserva(OffsetDateTime.parse("2026-04-01T09:00:00Z"));
        reservation.setFechaCancelacionReserva(null);
        reservation.setFechaFinalizacionReserva(null);
        return reservation;
    }

    private ServiceAvailabilityEntity availability(Long availabilityId,
                                                   Long serviceId,
                                                   Long stateId,
                                                   LocalDate date,
                                                   LocalTime start,
                                                   LocalTime end) {
        ServiceAvailabilityEntity availability = new ServiceAvailabilityEntity();
        availability.setIdDisponibilidadServicio(availabilityId);
        availability.setIdServicio(serviceId);
        availability.setIdEstadoDisponibilidad(stateId);
        availability.setFechaDisponibilidad(date);
        availability.setHoraInicio(start);
        availability.setHoraFin(end);
        return availability;
    }

    private ServiceEntity serviceEntity(Long serviceId, int capacity) {
        ServiceEntity service = new ServiceEntity();
        service.setIdServicio(serviceId);
        service.setCapacidadMaximaConcurrente(capacity);
        return service;
    }

    private BookingLifecycleProjection bookingProjection(Long bookingId,
                                                         Long serviceId,
                                                         Long providerUserId,
                                                         Long customerUserId,
                                                         Long stateId,
                                                         String stateName,
                                                         LocalDate date,
                                                         LocalTime start,
                                                         LocalTime end) {
        return new BookingLifecycleProjection() {
            @Override
            public Long getBookingId() {
                return bookingId;
            }

            @Override
            public Long getServiceId() {
                return serviceId;
            }

            @Override
            public Long getProviderUserId() {
                return providerUserId;
            }

            @Override
            public Long getCustomerUserId() {
                return customerUserId;
            }

            @Override
            public Long getReservationStateId() {
                return stateId;
            }

            @Override
            public String getReservationStateName() {
                return stateName;
            }

            @Override
            public LocalDate getSlotDate() {
                return date;
            }

            @Override
            public LocalTime getSlotStartTime() {
                return start;
            }

            @Override
            public LocalTime getSlotEndTime() {
                return end;
            }
        };
    }
}