package com.eap09.reservas.unittests.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationRequest;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationResponse;
import com.eap09.reservas.customerbooking.api.dto.ReservationCancellationResponse;
import com.eap09.reservas.customerbooking.application.CustomerReservationCancellationService;
import com.eap09.reservas.customerbooking.application.ReservationService;
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

public class CustomerReservationTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ServicesAvailabilityRepository availabilityRepository;
    @Mock
    private ServicesRepository servicesRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private StateRepository stateRepository;
    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private ReservationService reservationService;

    @InjectMocks
    private CustomerReservationCancellationService cancellationService;

    private UserAccountEntity client;
    private UserAccountEntity provider;
    private ServiceEntity service;
    private ServiceAvailabilityEntity availability;
    private StateEntity activeState;
    private StateEntity enabledState;
    private StateEntity createdState;
    private StateEntity canceledState;
    private StateEntity finalizedState;
    private ReservationEntity savedReservation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        RoleEntity clientRole = new RoleEntity();
        clientRole.setNombreRol("CLIENTE");

        RoleEntity providerRole = new RoleEntity();
        providerRole.setNombreRol("PROVEEDOR");

        client = new UserAccountEntity();
        client.setIdUsuario(1L);
        client.setCorreoUsuario("juan.cliente@gmail.com");
        client.setRol(clientRole);

        provider = new UserAccountEntity();
        provider.setIdUsuario(10L);
        provider.setIdEstado(1L);
        provider.setRol(providerRole);

        service = new ServiceEntity();
        service.setIdServicio(200L);
        service.setNombreServicio("Peluquería");
        service.setIdUsuarioProveedor(10L);
        service.setIdEstadoServicio(1L);
        service.setCapacidadMaximaConcurrente(5);

        availability = new ServiceAvailabilityEntity();
        availability.setIdDisponibilidadServicio(500L);
        availability.setIdServicio(200L);
        availability.setIdEstadoDisponibilidad(1L);

        activeState = new StateEntity();
        activeState.setIdEstado(1L);
        activeState.setNombreEstado("ACTIVA");

        enabledState = new StateEntity();
        enabledState.setIdEstado(1L);
        enabledState.setNombreEstado("HABILITADA");

        savedReservation = new ReservationEntity();
        savedReservation.setIdEstadoReserva(7L);
        savedReservation.setIdUsuarioCliente(1L);
        savedReservation.setFechaCreacionReserva(OffsetDateTime.now());

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
    void CreateReservation_Success() {
        CreateReservationRequest request = new CreateReservationRequest(10L, 200L, 500L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.cliente@gmail.com"))
                .thenReturn(Optional.of(client));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")).thenReturn(Optional.of(activeState));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO")).thenReturn(Optional.of(activeState));
        when(stateRepository.findByCategoryAndStateName("tbl_disponibilidad_servicio", "HABILITADA"))
                .thenReturn(Optional.of(enabledState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA")).thenReturn(Optional.of(createdState));

        when(userAccountRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(servicesRepository.findByIdServicio(200L)).thenReturn(Optional.of(service));
        when(availabilityRepository.findByIdDisponibilidadServicioForUpdate(500L))
                .thenReturn(Optional.of(availability));
        when(reservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(500L, 2L)).thenReturn(0L);
        when(reservationRepository.save(any(ReservationEntity.class))).thenReturn(savedReservation);

        CreateReservationResponse response = reservationService.createReservation(request, client.getCorreoUsuario());

        assertEquals(1L, response.customerId());
        assertEquals(200L, response.serviceId());
        assertEquals(500L, response.availabilityId());
        assertEquals(2L, createdState.getIdEstado());
    }

    @Test
    void CreateReservation_InactiveService_Exception() {
        CreateReservationRequest request = new CreateReservationRequest(10L, 200L, 500L);
        service.setIdEstadoServicio(0L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase(anyString())).thenReturn(Optional.of(client));
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")).thenReturn(Optional.of(activeState));
        when(stateRepository.findByCategoryAndStateName("tbl_disponibilidad_servicio", "HABILITADA"))
                .thenReturn(Optional.of(enabledState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA")).thenReturn(Optional.of(createdState));

        when(userAccountRepository.findById(10L)).thenReturn(Optional.of(provider));
        when(servicesRepository.findByIdServicio(200L)).thenReturn(Optional.of(service));

        assertThrows(IllegalStateException.class,
                () -> reservationService.createReservation(request, client.getCorreoUsuario()));
    }

    @Test
    void CancelOwnCreatedBooking_Success() {
        BookingLifecycleProjection projection = mock(BookingLifecycleProjection.class);
        when(projection.getBookingId()).thenReturn(101L);
        when(projection.getProviderUserId()).thenReturn(10L);
        when(projection.getCustomerUserId()).thenReturn(1L);
        when(projection.getReservationStateId()).thenReturn(2L);
        when(projection.getSlotDate()).thenReturn(LocalDate.now().plusDays(2));
        when(projection.getSlotStartTime()).thenReturn(LocalTime.of(9, 0));
        when(projection.getSlotEndTime()).thenReturn(LocalTime.of(10, 0));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.cliente@gmail.com"))
                .thenReturn(Optional.of(client));
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(projection));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(createdState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CANCELADA"))
                .thenReturn(Optional.of(canceledState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "FINALIZADA"))
                .thenReturn(Optional.of(finalizedState));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(savedReservation));

        ReservationCancellationResponse result = cancellationService.cancelOwnBooking("juan.cliente@gmail.com", 101L);

        assertEquals(101L, result.bookingId());
        assertEquals("CANCELADA", result.bookingStatus());
        assertNotNull(result.canceledAt());
    }

    @Test
    void shouldRejectWhenSlotAlreadyStarted() {
        BookingLifecycleProjection projection = mock(BookingLifecycleProjection.class);
        when(projection.getBookingId()).thenReturn(101L);
        when(projection.getProviderUserId()).thenReturn(10L);
        when(projection.getCustomerUserId()).thenReturn(1L);
        when(projection.getReservationStateId()).thenReturn(2L);
        when(projection.getSlotDate()).thenReturn(LocalDate.now());
        when(projection.getSlotStartTime()).thenReturn(LocalTime.now().minusHours(2));
        when(projection.getSlotEndTime()).thenReturn(LocalTime.now().plusHours(2));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("juan.cliente@gmail.com"))
                .thenReturn(Optional.of(client));
        when(reservationRepository.findBookingLifecycleById(101L))
                .thenReturn(Optional.of(projection));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(createdState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CANCELADA"))
                .thenReturn(Optional.of(canceledState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "FINALIZADA"))
                .thenReturn(Optional.of(finalizedState));
        when(reservationRepository.findById(101L)).thenReturn(Optional.of(savedReservation));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> cancellationService.cancelOwnBooking("juan.cliente@gmail.com", 101L));

        assertEquals("BOOKING_SLOT_ALREADY_STARTED", ex.getErrorCode());
    }
}
