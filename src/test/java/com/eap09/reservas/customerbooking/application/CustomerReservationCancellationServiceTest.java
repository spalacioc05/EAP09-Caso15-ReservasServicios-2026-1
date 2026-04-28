package com.eap09.reservas.customerbooking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.customerbooking.api.dto.ReservationCancellationResponse;
import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import com.eap09.reservas.customerbooking.infrastructure.BookingLifecycleProjection;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerReservationCancellationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private CustomerReservationCancellationService service;

    @Test
    void shouldCancelOwnCreatedBookingBeforeSlotStarts() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("customer@test.local"))
                .thenReturn(Optional.of(clientUser()));
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 10L, 50L, 7L, LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(state(7L, "CREADA")));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CANCELADA"))
                .thenReturn(Optional.of(state(8L, "CANCELADA")));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "FINALIZADA"))
                .thenReturn(Optional.of(state(9L, "FINALIZADA")));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(reservationEntity(7L)));

        ReservationCancellationResponse result = service.cancelOwnBooking("customer@test.local", 101L);

        assertEquals(101L, result.bookingId());
        assertEquals("CANCELADA", result.bookingStatus());
        assertNotNull(result.canceledAt());
        verify(reservationRepository).save(any(ReservationEntity.class));
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectWhenSlotAlreadyStarted() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("customer@test.local"))
                .thenReturn(Optional.of(clientUser()));
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(bookingProjection(101L, 10L, 50L, 7L, LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(state(7L, "CREADA")));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CANCELADA"))
                .thenReturn(Optional.of(state(8L, "CANCELADA")));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "FINALIZADA"))
                .thenReturn(Optional.of(state(9L, "FINALIZADA")));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.cancelOwnBooking("customer@test.local", 101L));

        assertEquals("BOOKING_SLOT_ALREADY_STARTED", ex.getErrorCode());
    }

    @Test
    void shouldRejectWhenUserIsNotClient() {
        UserAccountEntity provider = clientUser();
        provider.getRol().setNombreRol("PROVEEDOR");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("customer@test.local"))
                .thenReturn(Optional.of(provider));

        assertThrows(ClientRoleRequiredException.class,
                () -> service.cancelOwnBooking("customer@test.local", 101L));
    }

    private UserAccountEntity clientUser() {
        RoleEntity role = new RoleEntity();
        role.setNombreRol("CLIENTE");

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(50L);
        user.setCorreoUsuario("customer@test.local");
        user.setRol(role);
        return user;
    }

    private StateEntity state(Long id, String name) {
        StateEntity state = new StateEntity();
        state.setIdEstado(id);
        state.setNombreEstado(name);
        return state;
    }

    private ReservationEntity reservationEntity(Long stateId) {
        ReservationEntity entity = new ReservationEntity();
        entity.setIdEstadoReserva(stateId);
        entity.setFechaCreacionReserva(OffsetDateTime.now());
        return entity;
    }

    private BookingLifecycleProjection bookingProjection(Long bookingId,
                                                         Long providerId,
                                                         Long customerId,
                                                         Long stateId,
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
                return 300L;
            }

            @Override
            public Long getProviderUserId() {
                return providerId;
            }

            @Override
            public Long getCustomerUserId() {
                return customerId;
            }

            @Override
            public Long getReservationStateId() {
                return stateId;
            }

            @Override
            public String getReservationStateName() {
                return "CREADA";
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