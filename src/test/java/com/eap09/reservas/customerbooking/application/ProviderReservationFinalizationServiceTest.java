package com.eap09.reservas.customerbooking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.customerbooking.api.dto.ReservationFinalizationResponse;
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
class ProviderReservationFinalizationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private ProviderReservationFinalizationService service;

    @Test
    void shouldFinalizeOwnCreatedBookingWhenSlotAlreadyEnded() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(providerUser()));
        when(reservationRepository.findBookingLifecycleById(100L))
                .thenReturn(Optional.of(bookingProjection(100L, 10L, 50L, 7L, LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(state(7L, "CREADA")));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CANCELADA"))
                .thenReturn(Optional.of(state(8L, "CANCELADA")));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "FINALIZADA"))
                .thenReturn(Optional.of(state(9L, "FINALIZADA")));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservationEntity(100L, 7L)));

        ReservationFinalizationResponse result = service.finalizeOwnBooking("provider@test.local", 100L);

        assertEquals(100L, result.bookingId());
        assertEquals("FINALIZADA", result.bookingStatus());
        assertNotNull(result.finalizedAt());
        verify(reservationRepository).save(any(ReservationEntity.class));
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectWhenSlotHasNotFinishedYet() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(providerUser()));
        when(reservationRepository.findBookingLifecycleById(100L))
                .thenReturn(Optional.of(bookingProjection(100L, 10L, 50L, 7L, LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(state(7L, "CREADA")));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CANCELADA"))
                .thenReturn(Optional.of(state(8L, "CANCELADA")));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "FINALIZADA"))
                .thenReturn(Optional.of(state(9L, "FINALIZADA")));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.finalizeOwnBooking("provider@test.local", 100L));

        assertEquals("BOOKING_SLOT_NOT_FINISHED", ex.getErrorCode());
    }

    @Test
    void shouldRejectWhenUserIsNotProvider() {
        UserAccountEntity client = providerUser();
        client.getRol().setNombreRol("CLIENTE");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(client));

        assertThrows(ProviderRoleRequiredException.class,
                () -> service.finalizeOwnBooking("provider@test.local", 100L));
    }

    @Test
    void shouldRejectWhenBookingNotFound() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(providerUser()));
        when(reservationRepository.findBookingLifecycleById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.finalizeOwnBooking("provider@test.local", 100L));
    }

    private UserAccountEntity providerUser() {
        RoleEntity role = new RoleEntity();
        role.setNombreRol("PROVEEDOR");

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(10L);
        user.setCorreoUsuario("provider@test.local");
        user.setRol(role);
        return user;
    }

    private StateEntity state(Long id, String name) {
        StateEntity state = new StateEntity();
        state.setIdEstado(id);
        state.setNombreEstado(name);
        return state;
    }

    private ReservationEntity reservationEntity(Long id, Long stateId) {
        ReservationEntity entity = new ReservationEntity();
        entity.setIdEstadoReserva(stateId);
        entity.setFechaCreacionReserva(OffsetDateTime.now());
        try {
            java.lang.reflect.Field idField = ReservationEntity.class.getDeclaredField("idReserva");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
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