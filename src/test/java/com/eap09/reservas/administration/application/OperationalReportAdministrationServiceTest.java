package com.eap09.reservas.administration.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eap09.reservas.administration.api.dto.OperationalReportResponse;
import com.eap09.reservas.administration.infrastructure.BookingsByProviderProjection;
import com.eap09.reservas.administration.infrastructure.BookingsByServiceProjection;
import com.eap09.reservas.administration.infrastructure.BookingsByStatusProjection;
import com.eap09.reservas.administration.infrastructure.OperationalReportRepository;
import com.eap09.reservas.common.audit.SystemEvent;
import com.eap09.reservas.common.audit.SystemEventPublisher;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.OperationalReportGenerationFailedException;
import com.eap09.reservas.common.exception.OperationalReportUnavailableException;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class OperationalReportAdministrationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private OperationalReportRepository operationalReportRepository;

    @Mock
    private SystemEventPublisher systemEventPublisher;

    @InjectMocks
    private OperationalReportAdministrationService operationalReportAdministrationService;

    @Test
    void shouldGenerateOperationalReportSuccessfully() {
        stubAdmin();
        stubReportData(null, null, 10L, 4L, 4L, 2L, 20L);

        OperationalReportAdministrationResult result = operationalReportAdministrationService.generateOperationalReport(
                "admin@reservas.test",
                null,
                null);

        assertEquals("Reporte operativo generado correctamente", result.message());
        OperationalReportResponse report = result.report();
        assertEquals(10L, report.totalBookings());
        assertEquals(4L, report.activeBookings());
        assertEquals(4L, report.completedBookings());
        assertEquals(2L, report.cancelledBookings());
        assertEquals(new BigDecimal("20.00"), report.cancellationRate());
        assertEquals(new BigDecimal("40.00"), report.completionRate());
        assertEquals(new BigDecimal("40.00"), report.occupancyRate());
        assertEquals(3, report.bookingsByStatus().size());
        assertEquals(2, report.bookingsByService().size());
        assertEquals(2, report.bookingsByProvider().size());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("GENERACION_REPORTE_OPERATIVO", eventCaptor.getValue().type());
        assertEquals("EXITO", eventCaptor.getValue().result());
    }

    @Test
    void shouldCalculateCancellationRateCorrectly() {
        stubAdmin();
        stubReportData(null, null, 10L, 3L, 5L, 2L, 25L);

        OperationalReportAdministrationResult result = operationalReportAdministrationService.generateOperationalReport(
                "admin@reservas.test",
                null,
                null);

        assertEquals(new BigDecimal("20.00"), result.report().cancellationRate());
    }

    @Test
    void shouldCalculateCompletionRateCorrectly() {
        stubAdmin();
        stubReportData(null, null, 10L, 3L, 5L, 2L, 25L);

        OperationalReportAdministrationResult result = operationalReportAdministrationService.generateOperationalReport(
                "admin@reservas.test",
                null,
                null);

        assertEquals(new BigDecimal("50.00"), result.report().completionRate());
    }

    @Test
    void shouldCalculateOccupancyRateCorrectly() {
        stubAdmin();
        stubReportData(null, null, 10L, 4L, 4L, 2L, 16L);

        OperationalReportAdministrationResult result = operationalReportAdministrationService.generateOperationalReport(
                "admin@reservas.test",
                null,
                null);

        assertEquals(new BigDecimal("50.00"), result.report().occupancyRate());
    }

    @Test
    void shouldReturnZeroOccupancyRateWhenCapacityIsZero() {
        stubAdmin();
        stubReportData(null, null, 10L, 4L, 4L, 2L, 0L);

        OperationalReportAdministrationResult result = operationalReportAdministrationService.generateOperationalReport(
                "admin@reservas.test",
                null,
                null);

        assertEquals(new BigDecimal("0.00"), result.report().occupancyRate());
    }

    @Test
    void shouldGenerateReportWithoutFilters() {
        stubAdmin();
        stubReportData(null, null, 10L, 4L, 4L, 2L, 20L);

        operationalReportAdministrationService.generateOperationalReport("admin@reservas.test", null, null);

        verify(operationalReportRepository).countBookings(isNull(), isNull());
        verify(operationalReportRepository).countBookingsByStatus(isNull(), isNull());
        verify(operationalReportRepository).countBookingsByService(isNull(), isNull());
        verify(operationalReportRepository).countBookingsByProvider(isNull(), isNull());
        verify(operationalReportRepository).calculateTotalOfferedCapacity(isNull(), isNull());
    }

    @Test
    void shouldGenerateReportWithDateRange() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        stubAdmin();
        stubReportData(from, to, 10L, 4L, 4L, 2L, 20L);

        operationalReportAdministrationService.generateOperationalReport("admin@reservas.test", from, to);

        verify(operationalReportRepository).countBookings(from, to);
        verify(operationalReportRepository).countBookingsByStatus(from, to);
        verify(operationalReportRepository).countBookingsByService(from, to);
        verify(operationalReportRepository).countBookingsByProvider(from, to);
        verify(operationalReportRepository).calculateTotalOfferedCapacity(from, to);
    }

    @Test
    void shouldRejectInvalidDateRange() {
        stubAdmin();

        ApiException exception = assertThrows(
                ApiException.class,
                () -> operationalReportAdministrationService.generateOperationalReport(
                        "admin@reservas.test",
                        LocalDate.of(2026, 6, 30),
                        LocalDate.of(2026, 6, 1)));

        assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
        verifyNoInteractions(operationalReportRepository);
        verify(systemEventPublisher).publish(any(SystemEvent.class));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotAdmin() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("cliente@reservas.test"))
                .thenReturn(Optional.of(nonAdminUser()));

        assertThrows(
                AdminRoleRequiredException.class,
                () -> operationalReportAdministrationService.generateOperationalReport(
                        "cliente@reservas.test",
                        null,
                        null));

        verifyNoInteractions(operationalReportRepository);
        verifyNoInteractions(systemEventPublisher);
    }

    @Test
    void shouldRejectWhenAuthenticatedActorDoesNotExist() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("desconocido@reservas.test"))
                .thenReturn(Optional.empty());

        assertThrows(
                AdminRoleRequiredException.class,
                () -> operationalReportAdministrationService.generateOperationalReport(
                        "desconocido@reservas.test",
                        null,
                        null));

        verifyNoInteractions(operationalReportRepository);
        verifyNoInteractions(systemEventPublisher);
    }

    @Test
    void shouldRejectWhenAuthenticationIsMissing() {
        assertThrows(
                InsufficientAuthenticationException.class,
                () -> operationalReportAdministrationService.generateOperationalReport(
                        " ",
                        null,
                        null));

        verifyNoInteractions(userAccountRepository);
        verifyNoInteractions(systemEventPublisher);
    }

    @Test
    void shouldRejectWhenNoHistoryIsAvailable() {
        stubAdmin();
        when(operationalReportRepository.countBookings(null, null)).thenReturn(0L);

        OperationalReportUnavailableException exception = assertThrows(
                OperationalReportUnavailableException.class,
                () -> operationalReportAdministrationService.generateOperationalReport(
                        "admin@reservas.test",
                        null,
                        null));

        assertEquals("No hay reservas registradas para generar el reporte operativo", exception.getMessage());
        verify(operationalReportRepository, never()).countBookingsByStatus(any(), any());
        verify(operationalReportRepository, never()).countBookingsByService(any(), any());
        verify(operationalReportRepository, never()).countBookingsByProvider(any(), any());
        verify(operationalReportRepository, never()).calculateTotalOfferedCapacity(any(), any());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
    }

    @Test
    void shouldWrapUnexpectedQueryFailureIntoControlledException() {
        stubAdmin();
        when(operationalReportRepository.countBookings(null, null))
                .thenThrow(new DataAccessResourceFailureException("db unavailable"));

        OperationalReportGenerationFailedException exception = assertThrows(
                OperationalReportGenerationFailedException.class,
                () -> operationalReportAdministrationService.generateOperationalReport(
                        "admin@reservas.test",
                        null,
                        null));

        assertEquals(
                "No fue posible generar el reporte operativo. Intenta nuevamente mas tarde",
                exception.getMessage());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(systemEventPublisher).publish(eventCaptor.capture());
        assertEquals("FALLO", eventCaptor.getValue().result());
    }

    private void stubAdmin() {
        when(userAccountRepository.findByCorreoUsuarioIgnoreCase("admin@reservas.test"))
                .thenReturn(Optional.of(adminUser()));
    }

    private void stubReportData(LocalDate from,
                                LocalDate to,
                                long totalBookings,
                                long activeBookings,
                                long completedBookings,
                                long cancelledBookings,
                                long totalOfferedCapacity) {
        when(operationalReportRepository.countBookings(from, to)).thenReturn(totalBookings);
        when(operationalReportRepository.countBookingsByStatus(from, to)).thenReturn(List.of(
                statusProjection("CREADA", activeBookings),
                statusProjection("FINALIZADA", completedBookings),
                statusProjection("CANCELADA", cancelledBookings)));
        when(operationalReportRepository.countBookingsByService(from, to)).thenReturn(List.of(
                serviceProjection(310L, "Consulta medica", 6L),
                serviceProjection(320L, "Terapia ocupacional", 4L)));
        when(operationalReportRepository.countBookingsByProvider(from, to)).thenReturn(List.of(
                providerProjection(205L, "Proveedor Demo", 7L),
                providerProjection(206L, "Proveedor Dos", 3L)));
        when(operationalReportRepository.calculateTotalOfferedCapacity(from, to)).thenReturn(totalOfferedCapacity);
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

    private BookingsByStatusProjection statusProjection(String status, long total) {
        return new BookingsByStatusProjection() {
            @Override
            public String getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private BookingsByServiceProjection serviceProjection(Long serviceId, String serviceName, long total) {
        return new BookingsByServiceProjection() {
            @Override
            public Long getServiceId() {
                return serviceId;
            }

            @Override
            public String getServiceName() {
                return serviceName;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private BookingsByProviderProjection providerProjection(Long providerId, String providerName, long total) {
        return new BookingsByProviderProjection() {
            @Override
            public Long getProviderId() {
                return providerId;
            }

            @Override
            public String getProviderName() {
                return providerName;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}