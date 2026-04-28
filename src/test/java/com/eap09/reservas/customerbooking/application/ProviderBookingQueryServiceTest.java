package com.eap09.reservas.customerbooking.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ProviderReservationQueryFailedException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.customerbooking.api.dto.ProviderBookingResponse;
import com.eap09.reservas.customerbooking.infrastructure.ProviderBookingProjection;
import com.eap09.reservas.customerbooking.infrastructure.ProviderBookingQueryRepository;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.eap09.reservas.provideroffer.domain.ServiceEntity;
import com.eap09.reservas.provideroffer.infrastructure.ServiceRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ProviderBookingQueryServiceTest {

    @Mock
    private ProviderBookingQueryRepository providerBookingQueryRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private ProviderBookingQueryService service;

    @Test
    void shouldQueryOwnBookingsWithoutFilters() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(providerBookingQueryRepository.findProviderBookings(10L, null, null, null))
                .thenReturn(List.of(projection(500L, 200L, "Servicio A", 700L, LocalDate.of(2026, 4, 28), "CREADA")));
        when(providerBookingQueryRepository.existsAnyReservationForProvider(10L)).thenReturn(true);

        ProviderBookingQueryResult result = service.getOwnBookings("provider@test.local", null, null, null);

        assertEquals("Consulta operativa de reservas exitosa", result.message());
        assertEquals(1, result.bookings().size());
        ProviderBookingResponse booking = result.bookings().get(0);
        assertEquals(500L, booking.bookingId());
        assertEquals("CREADA", booking.bookingStatus());
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldFilterByDate() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(providerBookingQueryRepository.findProviderBookings(10L, null, LocalDate.of(2026, 4, 28), null))
                .thenReturn(List.of(projection(501L, 201L, "Servicio Fecha", 701L, LocalDate.of(2026, 4, 28), "CREADA")));
        when(providerBookingQueryRepository.existsAnyReservationForProvider(10L)).thenReturn(true);

        ProviderBookingQueryResult result = service.getOwnBookings("provider@test.local", LocalDate.of(2026, 4, 28), null, null);

        assertEquals(1, result.bookings().size());
        assertEquals(LocalDate.of(2026, 4, 28), result.bookings().get(0).slotDate());
    }

    @Test
    void shouldFilterByState() {
        StateEntity createdState = new StateEntity();
        createdState.setIdEstado(7L);
        createdState.setNombreEstado("CREADA");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(createdState));
        when(providerBookingQueryRepository.findProviderBookings(10L, null, null, 7L))
                .thenReturn(List.of(projection(502L, 202L, "Servicio Estado", 702L, LocalDate.of(2026, 4, 29), "CREADA")));
        when(providerBookingQueryRepository.existsAnyReservationForProvider(10L)).thenReturn(true);

        ProviderBookingQueryResult result = service.getOwnBookings("provider@test.local", null, "CREADA", null);

        assertEquals(1, result.bookings().size());
        assertEquals("CREADA", result.bookings().get(0).bookingStatus());
    }

    @Test
    void shouldFilterByOwnService() {
        ServiceEntity ownService = new ServiceEntity();
        ownService.setIdServicio(300L);
        ownService.setIdUsuarioProveedor(10L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(serviceRepository.findByIdServicio(300L)).thenReturn(Optional.of(ownService));
        when(providerBookingQueryRepository.findProviderBookings(10L, 300L, null, null))
                .thenReturn(List.of(projection(503L, 300L, "Servicio Propio", 703L, LocalDate.of(2026, 4, 30), "CREADA")));
        when(providerBookingQueryRepository.existsAnyReservationForProvider(10L)).thenReturn(true);

        ProviderBookingQueryResult result = service.getOwnBookings("provider@test.local", null, null, 300L);

        assertEquals(1, result.bookings().size());
        assertEquals(300L, result.bookings().get(0).serviceId());
    }

    @Test
    void shouldFilterWithCombination() {
        StateEntity createdState = new StateEntity();
        createdState.setIdEstado(7L);
        createdState.setNombreEstado("CREADA");

        ServiceEntity ownService = new ServiceEntity();
        ownService.setIdServicio(301L);
        ownService.setIdUsuarioProveedor(10L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(serviceRepository.findByIdServicio(301L)).thenReturn(Optional.of(ownService));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(createdState));
        when(providerBookingQueryRepository.findProviderBookings(10L, 301L, LocalDate.of(2026, 5, 1), 7L))
                .thenReturn(List.of(projection(504L, 301L, "Servicio Combo", 704L, LocalDate.of(2026, 5, 1), "CREADA")));
        when(providerBookingQueryRepository.existsAnyReservationForProvider(10L)).thenReturn(true);

        ProviderBookingQueryResult result = service.getOwnBookings(
                "provider@test.local",
                LocalDate.of(2026, 5, 1),
                "CREADA",
                301L);

        assertEquals(1, result.bookings().size());
        assertEquals(LocalDate.of(2026, 5, 1), result.bookings().get(0).slotDate());
    }

    @Test
    void shouldReturnFilteredNoResultsMessage() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(providerBookingQueryRepository.findProviderBookings(10L, null, LocalDate.of(2026, 5, 2), null))
                .thenReturn(List.of());
        when(providerBookingQueryRepository.existsAnyReservationForProvider(10L)).thenReturn(true);

        ProviderBookingQueryResult result = service.getOwnBookings("provider@test.local", LocalDate.of(2026, 5, 2), null, null);

        assertEquals("No existen reservas que cumplan con los filtros aplicados", result.message());
        assertEquals(0, result.bookings().size());
    }

    @Test
    void shouldReturnNoReservationsRegisteredMessage() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(providerBookingQueryRepository.findProviderBookings(10L, null, null, null))
                .thenReturn(List.of());
        when(providerBookingQueryRepository.existsAnyReservationForProvider(10L)).thenReturn(false);

        ProviderBookingQueryResult result = service.getOwnBookings("provider@test.local", null, null, null);

        assertEquals("No existen reservas registradas para sus servicios", result.message());
        assertEquals(0, result.bookings().size());
    }

    @Test
    void shouldRejectForeignServiceFilter() {
        ServiceEntity foreignService = new ServiceEntity();
        foreignService.setIdServicio(302L);
        foreignService.setIdUsuarioProveedor(99L);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(serviceRepository.findByIdServicio(302L)).thenReturn(Optional.of(foreignService));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> service.getOwnBookings("provider@test.local", null, null, 302L));

        assertEquals("No tiene permisos para consultar reservas de este servicio", ex.getMessage());
        verify(providerBookingQueryRepository, never()).findProviderBookings(any(), any(), any(), any());
    }

    @Test
    void shouldRejectWhenServiceFilterDoesNotExist() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(serviceRepository.findByIdServicio(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getOwnBookings("provider@test.local", null, null, 999L));
    }

    @Test
    void shouldRejectInvalidReservationStatusFilter() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "NO_VALIDO"))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.getOwnBookings("provider@test.local", null, "NO_VALIDO", null));

        assertEquals("INVALID_RESERVATION_STATUS", ex.getErrorCode());
    }

    @Test
    void shouldBeReadOnlyAndNeverSaveData() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(providerBookingQueryRepository.findProviderBookings(10L, null, null, null))
                .thenReturn(List.of(projection(600L, 400L, "Servicio RO", 800L, LocalDate.of(2026, 5, 3), "CREADA")));
        when(providerBookingQueryRepository.existsAnyReservationForProvider(10L)).thenReturn(true);

        ProviderBookingQueryResult result = service.getOwnBookings("provider@test.local", null, null, null);

        assertNotNull(result);
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void shouldTranslateDataErrorToControlledException() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(provider()));
        when(providerBookingQueryRepository.findProviderBookings(10L, null, null, null))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        ProviderReservationQueryFailedException ex = assertThrows(ProviderReservationQueryFailedException.class,
                () -> service.getOwnBookings("provider@test.local", null, null, null));

        assertNotNull(ex);
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectWhenUserIsNotProvider() {
        UserAccountEntity client = provider();
        client.getRol().setNombreRol("CLIENTE");

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("provider@test.local"))
                .thenReturn(Optional.of(client));

        assertThrows(ProviderRoleRequiredException.class,
                () -> service.getOwnBookings("provider@test.local", null, null, null));
    }

    private UserAccountEntity provider() {
        RoleEntity role = new RoleEntity();
        role.setNombreRol("PROVEEDOR");

        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(10L);
        user.setCorreoUsuario("provider@test.local");
        user.setRol(role);
        return user;
    }

    private ProviderBookingProjection projection(Long bookingId,
                                                 Long serviceId,
                                                 String serviceName,
                                                 Long availabilityId,
                                                 LocalDate date,
                                                 String status) {
        return new ProviderBookingProjection() {
            @Override
            public Long getBookingId() {
                return bookingId;
            }

            @Override
            public Long getServiceId() {
                return serviceId;
            }

            @Override
            public String getServiceName() {
                return serviceName;
            }

            @Override
            public Long getAvailabilityId() {
                return availabilityId;
            }

            @Override
            public LocalDate getSlotDate() {
                return date;
            }

            @Override
            public LocalTime getStartTime() {
                return LocalTime.of(9, 0);
            }

            @Override
            public LocalTime getEndTime() {
                return LocalTime.of(10, 0);
            }

            @Override
            public Long getCustomerId() {
                return 30L;
            }

            @Override
            public String getCustomerFullName() {
                return "Cliente Uno";
            }

            @Override
            public String getCustomerEmail() {
                return "cliente@test.local";
            }

            @Override
            public String getBookingStatus() {
                return status;
            }

            @Override
            public OffsetDateTime getCreatedAt() {
                return OffsetDateTime.now();
            }
        };
    }
}