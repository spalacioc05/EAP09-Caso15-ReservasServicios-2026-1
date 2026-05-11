package com.eap09.reservas.unittests.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.customerbooking.api.dto.ReservationFinalizationResponse;
import com.eap09.reservas.customerbooking.application.ProviderReservationFinalizationService;
import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import com.eap09.reservas.customerbooking.infrastructure.BookingLifecycleProjection;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;

public class ProviderReservationFinalizationServiceTest {
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

    private StateEntity createdState;
    private StateEntity canceledState;
    private StateEntity finalizedState;
    private UserAccountEntity user;
    private ReservationEntity savedReservation;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        RoleEntity role = new RoleEntity();
        role.setNombreRol("PROVEEDOR");

        createdState = new StateEntity();
        createdState.setIdEstado(7L);
        createdState.setNombreEstado("CREADA");

        canceledState = new StateEntity();
        canceledState.setIdEstado(8L);
        canceledState.setNombreEstado("CANCELADA");

        finalizedState = new StateEntity();
        finalizedState.setIdEstado(9L);
        finalizedState.setNombreEstado("FINALIZADA");

        user = new UserAccountEntity();
        user.setIdUsuario(10L);
        user.setCorreoUsuario("juan.medina@empresa.com");
        user.setRol(role);

        savedReservation = new ReservationEntity();
        savedReservation.setIdEstadoReserva(7L);
        savedReservation.setIdUsuarioCliente(50L);
        savedReservation.setFechaCreacionReserva(OffsetDateTime.now());
    }

    @Test
    void FinalizeOwnCreatedBooking_Success() {
        BookingLifecycleProjection projection = mock(BookingLifecycleProjection.class);
        when(projection.getBookingId()).thenReturn(100L);
        when(projection.getProviderUserId()).thenReturn(10L);
        when(projection.getReservationStateId()).thenReturn(7L);
        when(projection.getSlotDate()).thenReturn(LocalDate.now().minusDays(1));
        when(projection.getSlotStartTime()).thenReturn(LocalTime.of(9, 0));
        when(projection.getSlotEndTime()).thenReturn(LocalTime.of(10, 0));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.medina@empresa.com"))
                .thenReturn(Optional.of(user));
        when(reservationRepository.findBookingLifecycleById(100L)).thenReturn(Optional.of(projection));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA")).thenReturn(Optional.of(createdState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CANCELADA"))
                .thenReturn(Optional.of(canceledState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "FINALIZADA"))
                .thenReturn(Optional.of(finalizedState));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(savedReservation));
        when(reservationRepository.save(any())).thenReturn(savedReservation);

        ReservationFinalizationResponse result = service.finalizeOwnBooking("juan.medina@empresa.com", 100L);

        assertEquals(100L, result.bookingId());
        assertEquals("FINALIZADA", result.bookingStatus());
        assertNotNull(result.finalizedAt());
    }

    @Test
    void shouldRejectWhenSlotHasNotFinishedYet() {
        BookingLifecycleProjection projection = mock(BookingLifecycleProjection.class);
        when(projection.getBookingId()).thenReturn(100L);
        when(projection.getProviderUserId()).thenReturn(10L);
        when(projection.getReservationStateId()).thenReturn(7L);
        when(projection.getSlotDate()).thenReturn(LocalDate.now().plusDays(1));
        when(projection.getSlotStartTime()).thenReturn(LocalTime.of(9, 0));
        when(projection.getSlotEndTime()).thenReturn(LocalTime.of(10, 0));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.medina@empresa.com"))
                .thenReturn(Optional.of(user));
        when(reservationRepository.findBookingLifecycleById(100L)).thenReturn(Optional.of(projection));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA")).thenReturn(Optional.of(createdState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CANCELADA"))
                .thenReturn(Optional.of(canceledState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "FINALIZADA"))
                .thenReturn(Optional.of(finalizedState));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> service.finalizeOwnBooking("juan.medina@empresa.com", 100L));

        assertEquals("BOOKING_SLOT_NOT_FINISHED", ex.getErrorCode());
    }
}
