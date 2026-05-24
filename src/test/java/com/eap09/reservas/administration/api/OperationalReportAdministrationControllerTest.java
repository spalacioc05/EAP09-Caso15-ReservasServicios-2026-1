package com.eap09.reservas.administration.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.administration.api.dto.BookingsByProviderResponse;
import com.eap09.reservas.administration.api.dto.BookingsByServiceResponse;
import com.eap09.reservas.administration.api.dto.BookingsByStatusResponse;
import com.eap09.reservas.administration.api.dto.OperationalReportResponse;
import com.eap09.reservas.administration.application.OperationalReportAdministrationResult;
import com.eap09.reservas.administration.application.OperationalReportAdministrationService;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.OperationalReportGenerationFailedException;
import com.eap09.reservas.common.exception.OperationalReportUnavailableException;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import com.eap09.reservas.security.config.SecurityConfig;
import com.eap09.reservas.security.infrastructure.JwtAuthenticationFilter;
import com.eap09.reservas.security.infrastructure.RestAccessDeniedHandler;
import com.eap09.reservas.security.infrastructure.RestAuthenticationEntryPoint;
import com.eap09.reservas.support.ControllerAdviceTestConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OperationalReportAdministrationController.class)
@AutoConfigureMockMvc
@Import({
        ControllerAdviceTestConfig.class,
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtAuthenticationFilter.class
})
class OperationalReportAdministrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperationalReportAdministrationService operationalReportAdministrationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldGenerateOperationalReportWithoutFilters() throws Exception {
        when(operationalReportAdministrationService.generateOperationalReport("admin@reservas.test", null, null))
                .thenReturn(new OperationalReportAdministrationResult(
                        "Reporte operativo generado correctamente",
                        response(null, null)));

        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reporte operativo generado correctamente"))
                .andExpect(jsonPath("$.data.totalBookings").value(10))
                .andExpect(jsonPath("$.data.activeBookings").value(3))
                .andExpect(jsonPath("$.data.cancelledBookings").value(2))
                .andExpect(jsonPath("$.data.bookingsByStatus[0].status").value("CREADA"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldGenerateOperationalReportWithDateRange() throws Exception {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        when(operationalReportAdministrationService.generateOperationalReport(
                eq("admin@reservas.test"),
                eq(from),
                eq(to)))
                .thenReturn(new OperationalReportAdministrationResult(
                        "Reporte operativo generado correctamente",
                        response(from, to)));

        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value("2026-05-01"))
                .andExpect(jsonPath("$.data.to").value("2026-05-31"));
    }

    @Test
    void shouldReturnConflictWhenNoHistoryIsAvailable() throws Exception {
        when(operationalReportAdministrationService.generateOperationalReport("admin@reservas.test", null, null))
                .thenThrow(new OperationalReportUnavailableException(
                        "No hay reservas registradas para generar el reporte operativo"));

        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("OPERATIONAL_REPORT_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("No hay reservas registradas para generar el reporte operativo"));
    }

    @Test
    void shouldRejectWhenNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/operational"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotAdmin() throws Exception {
        when(operationalReportAdministrationService.generateOperationalReport("cliente@reservas.test", null, null))
                .thenThrow(new AdminRoleRequiredException(
                        "Solo un administrador autenticado puede generar reportes operativos globales"));

        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .with(user("cliente@reservas.test").roles("CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_ROLE_REQUIRED"));
    }

    @Test
    void shouldRejectInvalidDateParameter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .param("from", "2026-99-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(operationalReportAdministrationService);
    }

    @Test
    void shouldRejectInvalidDateRange() throws Exception {
        when(operationalReportAdministrationService.generateOperationalReport(
                "admin@reservas.test",
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 6, 1)))
                .thenThrow(new ApiException("INVALID_DATE_RANGE", "El rango de fechas no es valido"));

        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .param("from", "2026-06-30")
                        .param("to", "2026-06-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_DATE_RANGE"));
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(operationalReportAdministrationService.generateOperationalReport("admin@reservas.test", null, null))
                .thenThrow(new OperationalReportGenerationFailedException(
                        "No fue posible generar el reporte operativo. Intenta nuevamente mas tarde"));

        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("OPERATIONAL_REPORT_GENERATION_FAILED"))
                .andExpect(jsonPath("$.message").value("No fue posible generar el reporte operativo. Intenta nuevamente mas tarde"));
    }

    private OperationalReportResponse response(LocalDate from, LocalDate to) {
        return new OperationalReportResponse(
                from,
                to,
                10L,
                3L,
                5L,
                2L,
                new BigDecimal("20.00"),
                new BigDecimal("50.00"),
                new BigDecimal("40.00"),
                List.of(
                        new BookingsByStatusResponse("CREADA", 3L),
                        new BookingsByStatusResponse("FINALIZADA", 5L),
                        new BookingsByStatusResponse("CANCELADA", 2L)),
                List.of(new BookingsByServiceResponse(310L, "Consulta medica", 6L)),
                List.of(new BookingsByProviderResponse(205L, "Proveedor Demo", 7L)));
    }
}