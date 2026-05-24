package com.eap09.reservas.administration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eap09.reservas.administration.api.dto.AdminReservationSupervisionItemResponse;
import com.eap09.reservas.administration.infrastructure.AdminReservationSupervisionProjection;
import com.eap09.reservas.administration.infrastructure.AdminReservationSupervisionRepository;
import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AdminReservationQueryFailedException;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateCategoryEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

@ExtendWith(MockitoExtension.class)
class AdminReservationSupervisionServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private AdminReservationSupervisionRepository adminReservationSupervisionRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private AdminReservationSupervisionService adminReservationSupervisionService;

    @Test
    void shouldReturnAllReservationsWithoutFilters() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser()));
        when(adminReservationSupervisionRepository.findReservations(null, null, null, null, null, null))
                .thenReturn(List.of(createdProjection(), finishedProjection()));

        AdminReservationSupervisionResult result = adminReservationSupervisionService.getReservations(
                "admin@reservas.test",
                null,
                null,
                null,
                null,
                null,
                null);

        assertEquals("Reservas consultadas correctamente", result.message());
        assertEquals(2, result.bookings().size());
        AdminReservationSupervisionItemResponse firstBooking = result.bookings().get(0);
        assertEquals(100L, firstBooking.bookingId());
        assertEquals("CREADA", firstBooking.bookingStatus());
        assertEquals("cliente1@reservas.test", firstBooking.customerEmail());
        verify(adminReservationSupervisionRepository, never()).existsAnyReservation();

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("CONSULTA_ADMIN_RESERVAS", eventCaptor.getValue().type());
        assertEquals("EXITO", eventCaptor.getValue().result());
    }

    @Test
    void shouldReturnReservationsWithAllFiltersAndNormalizedStatus() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser()));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA"))
                .thenReturn(Optional.of(reservationState(7L, "CREADA")));
        when(adminReservationSupervisionRepository.findReservations(10L, 20L, 30L, 7L,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(createdProjection()));

        AdminReservationSupervisionResult result = adminReservationSupervisionService.getReservations(
                "admin@reservas.test",
                10L,
                20L,
                30L,
                " creada ",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31));

        assertEquals("Reservas consultadas correctamente", result.message());
        assertEquals(1, result.bookings().size());
        verify(adminReservationSupervisionRepository).findReservations(
                10L,
                20L,
                30L,
                7L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31));
    }

    @Test
    void shouldReturnReservationsWithPartialFilters() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser()));
        when(adminReservationSupervisionRepository.findReservations(10L, null, null, null, null, null))
                .thenReturn(List.of(createdProjection()));

        AdminReservationSupervisionResult result = adminReservationSupervisionService.getReservations(
                "admin@reservas.test",
                10L,
                null,
                null,
                null,
                null,
                null);

        assertEquals(1, result.bookings().size());
        verify(adminReservationSupervisionRepository).findReservations(10L, null, null, null, null, null);
    }

    @Test
    void shouldReturnNoMatchesMessageWhenPlatformHasReservations() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser()));
        when(adminReservationSupervisionRepository.findReservations(null, null, null, null, null, null))
                .thenReturn(List.of());
        when(adminReservationSupervisionRepository.existsAnyReservation()).thenReturn(true);

        AdminReservationSupervisionResult result = adminReservationSupervisionService.getReservations(
                "admin@reservas.test",
                null,
                null,
                null,
                null,
                null,
                null);

        assertEquals("No existen reservas que cumplan con esos filtros", result.message());
        assertEquals(0, result.bookings().size());
        verify(adminReservationSupervisionRepository).existsAnyReservation();
    }

    @Test
    void shouldReturnNoReservationsMessageWhenPlatformHasNoBookings() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser()));
        when(adminReservationSupervisionRepository.findReservations(null, null, null, null, null, null))
                .thenReturn(List.of());
        when(adminReservationSupervisionRepository.existsAnyReservation()).thenReturn(false);

        AdminReservationSupervisionResult result = adminReservationSupervisionService.getReservations(
                "admin@reservas.test",
                null,
                null,
                null,
                null,
                null,
                null);

        assertEquals("No existen reservas registradas en la plataforma", result.message());
        assertEquals(0, result.bookings().size());
    }

    @Test
    void shouldRejectInvalidDateRange() {
        LocalDate from = LocalDate.of(2026, 6, 30);
        LocalDate to = LocalDate.of(2026, 6, 1);

        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> adminReservationSupervisionService.getReservations(
                        "admin@reservas.test",
                        null,
                        null,
                        null,
                        null,
                from,
                to));

        assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
        verify(adminReservationSupervisionRepository, never()).findReservations(any(), any(), any(), any(), any(), any());
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectBlankStatusFilter() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser()));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> adminReservationSupervisionService.getReservations(
                        "admin@reservas.test",
                        null,
                        null,
                        null,
                        "   ",
                        null,
                        null));

        assertEquals("INVALID_RESERVATION_STATUS", exception.getErrorCode());
        verifyNoInteractions(stateRepository);
        verify(adminReservationSupervisionRepository, never()).findReservations(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectUnknownStatusFilter() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser()));
        when(stateRepository.findByCategoryAndStateName("tbl_reserva", "NO_VALIDO"))
                .thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> adminReservationSupervisionService.getReservations(
                        "admin@reservas.test",
                        null,
                        null,
                        null,
                        "NO_VALIDO",
                        null,
                        null));

        assertEquals("INVALID_RESERVATION_STATUS", exception.getErrorCode());
        verify(adminReservationSupervisionRepository, never()).findReservations(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotAdmin() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(Optional.of(nonAdminUser()));

        assertThrows(
                AdminRoleRequiredException.class,
                () -> adminReservationSupervisionService.getReservations(
                        "cliente@reservas.test",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        verifyNoInteractions(adminReservationSupervisionRepository);
        verifyNoInteractions(systemEventPublisher);
    }

    @Test
    void shouldRejectWhenAuthenticatedActorDoesNotExist() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("desconocido@reservas.test"))
                .thenReturn(Optional.empty());

        assertThrows(
                AdminRoleRequiredException.class,
                () -> adminReservationSupervisionService.getReservations(
                        "desconocido@reservas.test",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        verifyNoInteractions(adminReservationSupervisionRepository);
        verifyNoInteractions(systemEventPublisher);
    }

    @Test
    void shouldRejectWhenAuthenticationIsMissing() {
        assertThrows(
                InsufficientAuthenticationException.class,
                () -> adminReservationSupervisionService.getReservations(
                        " ",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        verifyNoInteractions(userAccountRepository);
        verifyNoInteractions(systemEventPublisher);
    }

    @Test
    void shouldTranslateUnexpectedQueryFailureIntoControlledException() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser()));
        when(adminReservationSupervisionRepository.findReservations(null, null, null, null, null, null))
                .thenThrow(new DataAccessResourceFailureException("db unavailable"));

        AdminReservationQueryFailedException exception = assertThrows(
                AdminReservationQueryFailedException.class,
                () -> adminReservationSupervisionService.getReservations(
                        "admin@reservas.test",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        assertEquals(
                "No fue posible consultar las reservas administrativas. Intenta nuevamente mas tarde",
                exception.getMessage());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
    }

    private UserAccountEntity adminUser() {
        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(1L);
        user.setCorreoUsuario("admin@reservas.test");
        user.setRol(role("ADMINISTRADOR"));
        return user;
    }

    private UserAccountEntity nonAdminUser() {
        UserAccountEntity user = new UserAccountEntity();
        user.setIdUsuario(2L);
        user.setCorreoUsuario("cliente@reservas.test");
        user.setRol(role("CLIENTE"));
        return user;
    }

    private RoleEntity role(String name) {
        RoleEntity role = new RoleEntity();
        role.setNombreRol(name);
        return role;
    }

    private StateEntity reservationState(Long stateId, String stateName) {
        StateCategoryEntity category = new StateCategoryEntity();
        category.setNombreCategoriaEstado("tbl_reserva");

        StateEntity state = new StateEntity();
        state.setIdEstado(stateId);
        state.setNombreEstado(stateName);
        state.setCategoriaEstado(category);
        return state;
    }

    private AdminReservationSupervisionProjection createdProjection() {
        return projection(
                100L,
                10L,
                "Cliente Uno",
                "cliente1@reservas.test",
                20L,
                "Proveedor Uno",
                "proveedor1@reservas.test",
                30L,
                "Consulta medica",
                40L,
                LocalDate.of(2026, 5, 20),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "CREADA",
                Instant.parse("2026-05-01T15:30:00Z"),
                Instant.parse("2026-05-02T13:00:00Z"),
                null,
                null);
    }

    private AdminReservationSupervisionProjection finishedProjection() {
        return projection(
                101L,
                11L,
                "Cliente Dos",
                "cliente2@reservas.test",
                21L,
                "Proveedor Dos",
                "proveedor2@reservas.test",
                31L,
                "Asesoria legal",
                41L,
                LocalDate.of(2026, 4, 20),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0),
                "FINALIZADA",
                Instant.parse("2026-04-01T18:30:00Z"),
                Instant.parse("2026-04-20T21:00:00Z"),
                null,
                Instant.parse("2026-04-20T21:00:00Z"));
    }

    private AdminReservationSupervisionProjection projection(Long bookingId,
                                                             Long customerId,
                                                             String customerFullName,
                                                             String customerEmail,
                                                             Long providerId,
                                                             String providerFullName,
                                                             String providerEmail,
                                                             Long serviceId,
                                                             String serviceName,
                                                             Long availabilityId,
                                                             LocalDate slotDate,
                                                             LocalTime startTime,
                                                             LocalTime endTime,
                                                             String bookingStatus,
                                                             Instant createdAt,
                                                             Instant updatedAt,
                                                             Instant cancelledAt,
                                                             Instant finishedAt) {
        return new AdminReservationSupervisionProjection() {
            @Override
            public Long getBookingId() {
                return bookingId;
            }

            @Override
            public Long getCustomerId() {
                return customerId;
            }

            @Override
            public String getCustomerFullName() {
                return customerFullName;
            }

            @Override
            public String getCustomerEmail() {
                return customerEmail;
            }

            @Override
            public Long getProviderId() {
                return providerId;
            }

            @Override
            public String getProviderFullName() {
                return providerFullName;
            }

            @Override
            public String getProviderEmail() {
                return providerEmail;
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
                return slotDate;
            }

            @Override
            public LocalTime getStartTime() {
                return startTime;
            }

            @Override
            public LocalTime getEndTime() {
                return endTime;
            }

            @Override
            public String getBookingStatus() {
                return bookingStatus;
            }

            @Override
            public Instant getCreatedAt() {
                return createdAt;
            }

            @Override
            public Instant getUpdatedAt() {
                return updatedAt;
            }

            @Override
            public Instant getCancelledAt() {
                return cancelledAt;
            }

            @Override
            public Instant getFinishedAt() {
                return finishedAt;
            }
        };
    }
}