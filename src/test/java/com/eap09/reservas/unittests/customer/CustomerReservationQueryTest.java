package com.eap09.reservas.unittests.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.customerbooking.application.CustomerReservationQueryResult;
import com.eap09.reservas.customerbooking.application.CustomerReservationQueryService;
import com.eap09.reservas.customerbooking.infrastructure.CustomerReservationProjection;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;

public class CustomerReservationQueryTest {
    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private CustomerReservationQueryService service;

    private RoleEntity role;
    private UserAccountEntity user;
    private ServiceEntity mockService;
    private StateEntity createdState;
    private StateEntity canceledState;
    private StateEntity finalizedState;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        role = new RoleEntity();
        role.setIdRol(1L);
        role.setNombreRol("CLIENTE");

        user = new UserAccountEntity();
        user.setIdUsuario(1L);
        user.setCorreoUsuario("juan.empresa@gmail.com");
        user.setRol(role);
        user.setIdEstado(1L);

        mockService = new ServiceEntity();
        mockService.setIdServicio(200L);
        mockService.setIdUsuarioProveedor(10L);

        createdState = new StateEntity();
        createdState.setIdEstado(2L);
        createdState.setNombreEstado("CREADA");

        canceledState = new StateEntity();
        canceledState.setIdEstado(8L);
        canceledState.setNombreEstado("CANCELADA");

        finalizedState = new StateEntity();
        finalizedState.setIdEstado(9L);
        finalizedState.setNombreEstado("FINALIZADA");
    }

    @Test
    void shouldReturnOwnBookings() {
        CustomerReservationProjection reservation1 = mock(CustomerReservationProjection.class);
        when(reservation1.getBookingId()).thenReturn(101L);
        when(reservation1.getProviderId()).thenReturn(10L);
        when(reservation1.getBookingStatus()).thenReturn(createdState.getNombreEstado());
        when(reservation1.getSlotDate()).thenReturn(LocalDate.of(2026, 5, 15));
        when(reservation1.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(reservation1.getEndTime()).thenReturn(LocalTime.of(10, 0));

        CustomerReservationProjection reservation2 = mock(CustomerReservationProjection.class);
        when(reservation2.getBookingId()).thenReturn(202L);
        when(reservation2.getProviderId()).thenReturn(11L);
        when(reservation2.getBookingStatus()).thenReturn(finalizedState.getNombreEstado());
        when(reservation2.getSlotDate()).thenReturn(LocalDate.of(2026, 5, 6));
        when(reservation2.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(reservation2.getEndTime()).thenReturn(LocalTime.of(10, 0));

        CustomerReservationProjection reservation3 = mock(CustomerReservationProjection.class);
        when(reservation3.getBookingId()).thenReturn(303L);
        when(reservation3.getProviderId()).thenReturn(12L);
        when(reservation3.getBookingStatus()).thenReturn(canceledState.getNombreEstado());
        when(reservation3.getSlotDate()).thenReturn(LocalDate.of(2026, 5, 21));
        when(reservation3.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(reservation3.getEndTime()).thenReturn(LocalTime.of(10, 0));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.empresa@gmail.com"))
                .thenReturn(Optional.of(user));
        when(reservationRepository.findByCustomerUserId(1L))
                .thenReturn(List.of(reservation1, reservation2, reservation3));

        CustomerReservationQueryResult result = service.getOwnBookings("juan.empresa@gmail.com");

        assertEquals("Consulta de reservas del cliente exitosa", result.message());
        assertEquals(3, result.bookings().size());
    }

    @Test
    void ReturnControlledMessageWhenNoBookings() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.empresa@gmail.com"))
                .thenReturn(Optional.of(user));
        when(reservationRepository.findByCustomerUserId(50L)).thenReturn(List.of());

        CustomerReservationQueryResult result = service.getOwnBookings("juan.empresa@gmail.com");

        assertEquals("No existen reservas asociadas a tu cuenta", result.message());
        assertEquals(0, result.bookings().size());
    }

}
