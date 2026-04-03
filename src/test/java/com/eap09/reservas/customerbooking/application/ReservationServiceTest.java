package com.eap09.reservas.customerbooking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.common.exception.ReservationCreationFailedException;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationRequest;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationResponse;
import com.eap09.reservas.customerbooking.domain.ReservationEntity;
import com.eap09.reservas.customerbooking.infrastructure.ReservationRepository;
import com.eap09.reservas.customerbooking.infrastructure.ServicesAvailabilityRepository;
import com.eap09.reservas.customerbooking.infrastructure.ServicesRepository;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateCategoryEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.ServiceAvailabilityEntity;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ServicesAvailabilityRepository servicesAvailabilityRepository;

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

    private StateEntity activeUserState;
    private StateEntity activeServiceState;
    private StateEntity enabledAvailabilityState;
    private StateEntity createdReservationState;

    @BeforeEach
    void setUp() {
        activeUserState = state("tbl_usuario", "ACTIVA", 11L);
        activeServiceState = state("tbl_servicio", "ACTIVO", 22L);
        enabledAvailabilityState = state("tbl_disponibilidad_servicio", "HABILITADA", 33L);
        createdReservationState = state("tbl_reserva", "CREADA", 44L);
    }

    @Test
    void shouldCreateBookingSuccessfully() {
                stubStateCatalogs();

        CreateReservationRequest request = new CreateReservationRequest(200L, 300L, 400L);
        UserAccountEntity client = user(100L, "client@test.local", "CLIENTE", activeUserState.getIdEstado());
        UserAccountEntity provider = user(200L, "provider@test.local", "PROVEEDOR", activeUserState.getIdEstado());
        ServiceEntity service = service(300L, 200L, activeServiceState.getIdEstado(), 2);
        ServiceAvailabilityEntity availability = availability(400L, 300L, enabledAvailabilityState.getIdEstado(), LocalDate.of(2026, 4, 20));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(client));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(provider));
        when(servicesRepository.findByIdServicio(300L)).thenReturn(Optional.of(service));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(400L))
                .thenReturn(Optional.of(availability));
        when(reservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(400L, createdReservationState.getIdEstado()))
                .thenReturn(1L);
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenAnswer(invocation -> {
                    ReservationEntity saved = invocation.getArgument(0, ReservationEntity.class);
                    setReservationId(saved, 900L);
                    return saved;
                });

        CreateReservationResponse result = reservationService.createReservation(request, "client@test.local");

        assertEquals(900L, result.bookingId());
        assertEquals(200L, result.providerId());
        assertEquals(300L, result.serviceId());
        assertEquals(400L, result.availabilityId());
        assertEquals(100L, result.customerId());
        assertEquals(LocalDate.of(2026, 4, 20), result.slotDate());
        assertEquals("CREADA", result.bookingStatus());
        assertNotNull(result.createdAt());
        verify(systemEventPublisher).publish(any());
    }

    @Test
    void shouldRejectWhenServiceIsInactive() {
                stubStateCatalogs();

        CreateReservationRequest request = new CreateReservationRequest(200L, 300L, 400L);
        UserAccountEntity client = user(100L, "client@test.local", "CLIENTE", activeUserState.getIdEstado());
        UserAccountEntity provider = user(200L, "provider@test.local", "PROVEEDOR", activeUserState.getIdEstado());
        ServiceEntity inactiveService = service(300L, 200L, 999L, 2);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(client));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(provider));
        when(servicesRepository.findByIdServicio(300L)).thenReturn(Optional.of(inactiveService));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(request, "client@test.local"));

        assertEquals("SERVICE_NOT_AVAILABLE", ex.getErrorCode());
        assertEquals("El servicio no esta disponible", ex.getMessage());
    }

    @Test
    void shouldRejectWhenProviderIsInactive() {
                stubStateCatalogs();

        CreateReservationRequest request = new CreateReservationRequest(200L, 300L, 400L);
        UserAccountEntity client = user(100L, "client@test.local", "CLIENTE", activeUserState.getIdEstado());
        UserAccountEntity inactiveProvider = user(200L, "provider@test.local", "PROVEEDOR", 999L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(client));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(inactiveProvider));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(request, "client@test.local"));

        assertEquals("PROVIDER_NOT_AVAILABLE", ex.getErrorCode());
        assertEquals("El proveedor no esta disponible", ex.getMessage());
    }

    @Test
    void shouldRejectWhenRequiredFieldsAreMissing() {
        ApiException ex = assertThrows(ApiException.class,
                () -> reservationService.createReservation(new CreateReservationRequest(null, 300L, 400L), "client@test.local"));

        assertEquals("REQUIRED_FIELDS_MISSING", ex.getErrorCode());
        assertEquals("Proveedor, servicio y franja son requeridos", ex.getMessage());
    }

    @Test
    void shouldRejectWhenAvailabilityIsNotReservable() {
                stubStateCatalogs();

        CreateReservationRequest request = new CreateReservationRequest(200L, 300L, 400L);
        UserAccountEntity client = user(100L, "client@test.local", "CLIENTE", activeUserState.getIdEstado());
        UserAccountEntity provider = user(200L, "provider@test.local", "PROVEEDOR", activeUserState.getIdEstado());
        ServiceEntity service = service(300L, 200L, activeServiceState.getIdEstado(), 2);
        ServiceAvailabilityEntity blockedAvailability = availability(400L, 300L, 999L, LocalDate.of(2026, 4, 20));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(client));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(provider));
        when(servicesRepository.findByIdServicio(300L)).thenReturn(Optional.of(service));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(400L))
                .thenReturn(Optional.of(blockedAvailability));

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(request, "client@test.local"));

        assertEquals("AVAILABILITY_NOT_RESERVABLE", ex.getErrorCode());
        assertEquals("La franja seleccionada ya no puede reservarse", ex.getMessage());
    }

    @Test
    void shouldRejectWhenCapacityIsExhausted() {
                stubStateCatalogs();

        CreateReservationRequest request = new CreateReservationRequest(200L, 300L, 400L);
        UserAccountEntity client = user(100L, "client@test.local", "CLIENTE", activeUserState.getIdEstado());
        UserAccountEntity provider = user(200L, "provider@test.local", "PROVEEDOR", activeUserState.getIdEstado());
        ServiceEntity service = service(300L, 200L, activeServiceState.getIdEstado(), 1);
        ServiceAvailabilityEntity availability = availability(400L, 300L, enabledAvailabilityState.getIdEstado(), LocalDate.of(2026, 4, 20));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(client));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(provider));
        when(servicesRepository.findByIdServicio(300L)).thenReturn(Optional.of(service));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(400L))
                .thenReturn(Optional.of(availability));
        when(reservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(400L, createdReservationState.getIdEstado()))
                .thenReturn(1L);

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(request, "client@test.local"));

        assertEquals("AVAILABILITY_CAPACITY_EXHAUSTED", ex.getErrorCode());
        assertEquals("La franja seleccionada no tiene cupos disponibles", ex.getMessage());
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotClient() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(user(100L, "provider@test.local", "PROVEEDOR", activeUserState.getIdEstado())));

        ClientRoleRequiredException ex = assertThrows(ClientRoleRequiredException.class,
                () -> reservationService.createReservation(new CreateReservationRequest(200L, 300L, 400L), "provider@test.local"));

        assertNotNull(ex);
        verify(reservationRepository, never()).save(any(ReservationEntity.class));
    }

    @Test
    void shouldTranslateRepositoryErrorToControlledInternalError() {
                stubStateCatalogs();

        CreateReservationRequest request = new CreateReservationRequest(200L, 300L, 400L);
        UserAccountEntity client = user(100L, "client@test.local", "CLIENTE", activeUserState.getIdEstado());
        UserAccountEntity provider = user(200L, "provider@test.local", "PROVEEDOR", activeUserState.getIdEstado());
        ServiceEntity service = service(300L, 200L, activeServiceState.getIdEstado(), 2);
        ServiceAvailabilityEntity availability = availability(400L, 300L, enabledAvailabilityState.getIdEstado(), LocalDate.of(2026, 4, 20));

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("client@test.local"))
                .thenReturn(Optional.of(client));
        when(userAccountRepository.findById(200L)).thenReturn(Optional.of(provider));
        when(servicesRepository.findByIdServicio(300L)).thenReturn(Optional.of(service));
        when(servicesAvailabilityRepository.findByIdDisponibilidadServicioForUpdate(400L))
                .thenReturn(Optional.of(availability));
        when(reservationRepository.countByIdDisponibilidadServicioAndIdEstadoReserva(400L, createdReservationState.getIdEstado()))
                .thenReturn(0L);
        when(reservationRepository.save(any(ReservationEntity.class)))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        ReservationCreationFailedException ex = assertThrows(ReservationCreationFailedException.class,
                () -> reservationService.createReservation(request, "client@test.local"));

        assertEquals("No fue posible completar la reserva. Intenta nuevamente mas tarde", ex.getMessage());
    }

    private StateEntity state(String categoryName, String stateName, Long stateId) {
        StateCategoryEntity category = new StateCategoryEntity();
        category.setNombreCategoriaEstado(categoryName);

        StateEntity state = new StateEntity();
        state.setIdEstado(stateId);
        state.setNombreEstado(stateName);
        state.setCategoriaEstado(category);
        return state;
    }

    private UserAccountEntity user(Long id, String email, String roleName, Long stateId) {
        RoleEntity role = new RoleEntity();
        role.setNombreRol(roleName);

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(id);
        user.setCorreoUsuario(email);
        user.setRol(role);
        user.setIdEstado(stateId);
        return user;
    }

    private ServiceEntity service(Long id, Long providerId, Long stateId, int capacity) {
        ServiceEntity service = new ServiceEntity();
        service.setIdServicio(id);
        service.setIdUsuarioProveedor(providerId);
        service.setIdEstadoServicio(stateId);
        service.setCapacidadMaximaConcurrente(capacity);
        return service;
    }

    private ServiceAvailabilityEntity availability(Long id, Long serviceId, Long stateId, LocalDate date) {
        ServiceAvailabilityEntity availability = new ServiceAvailabilityEntity();
        availability.setIdDisponibilidadServicio(id);
        availability.setIdServicio(serviceId);
        availability.setIdEstadoDisponibilidad(stateId);
        availability.setFechaDisponibilidad(date);
        return availability;
    }

    private void setReservationId(ReservationEntity reservation, Long id) {
        try {
            var field = ReservationEntity.class.getDeclaredField("idReserva");
            field.setAccessible(true);
            field.set(reservation, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void stubStateCatalogs() {
        when(stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA"))
                .thenReturn(Optional.of(activeUserState));
        when(stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO"))
                .thenReturn(Optional.of(activeServiceState));
        when(stateRepository.findByCategoryAndStateName("tbl_disponibilidad_servicio", "HABILITADA"))
                .thenReturn(Optional.of(enabledAvailabilityState));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(createdReservationState));
    }
}
