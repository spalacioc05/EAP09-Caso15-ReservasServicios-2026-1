package com.eap09.reservas.administration.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.administration.api.dto.AdminReservationSupervisionItemResponse;
import com.eap09.reservas.administration.application.AdminReservationSupervisionResult;
import com.eap09.reservas.administration.application.AdminReservationSupervisionService;
import com.eap09.reservas.common.exception.AdminReservationQueryFailedException;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import com.eap09.reservas.security.config.SecurityConfig;
import com.eap09.reservas.security.infrastructure.JwtAuthenticationFilter;
import com.eap09.reservas.security.infrastructure.RestAccessDeniedHandler;
import com.eap09.reservas.security.infrastructure.RestAuthenticationEntryPoint;
import com.eap09.reservas.support.ControllerAdviceTestConfig;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminReservationSupervisionController.class)
@AutoConfigureMockMvc
@Import({
        ControllerAdviceTestConfig.class,
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtAuthenticationFilter.class
})
class AdminReservationSupervisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminReservationSupervisionService adminReservationSupervisionService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldReturnReservationsWithoutFilters() throws Exception {
        when(adminReservationSupervisionService.getReservations("admin@reservas.test", null, null, null, null, null, null))
                .thenReturn(new AdminReservationSupervisionResult(
                        "Reservas consultadas correctamente",
                        List.of(response())));

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservas consultadas correctamente"))
                .andExpect(jsonPath("$.data[0].bookingId").value(100))
                .andExpect(jsonPath("$.data[0].customerFullName").value("Cliente Uno"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturnReservationsWithFilters() throws Exception {
        when(adminReservationSupervisionService.getReservations(
                "admin@reservas.test",
                10L,
                20L,
                30L,
                "CREADA",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)))
                .thenReturn(new AdminReservationSupervisionResult(
                        "Reservas consultadas correctamente",
                        List.of(response())));

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .param("customerId", "10")
                        .param("providerId", "20")
                        .param("serviceId", "30")
                        .param("status", "CREADA")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].serviceId").value(30))
                .andExpect(jsonPath("$.data[0].bookingStatus").value("CREADA"));
    }

    @Test
    void shouldReturnNoMatchesMessage() throws Exception {
        when(adminReservationSupervisionService.getReservations("admin@reservas.test", null, null, null, null, null, null))
                .thenReturn(new AdminReservationSupervisionResult(
                        "No existen reservas que cumplan con esos filtros",
                        List.of()));

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No existen reservas que cumplan con esos filtros"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldReturnNoReservationsMessage() throws Exception {
        when(adminReservationSupervisionService.getReservations("admin@reservas.test", null, null, null, null, null, null))
                .thenReturn(new AdminReservationSupervisionResult(
                        "No existen reservas registradas en la plataforma",
                        List.of()));

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No existen reservas registradas en la plataforma"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectWhenNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotAdmin() throws Exception {
        when(adminReservationSupervisionService.getReservations("cliente@reservas.test", null, null, null, null, null, null))
                .thenThrow(new AdminRoleRequiredException(
                        "Solo un administrador autenticado puede consultar reservas globales"));

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("cliente@reservas.test").roles("CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_ROLE_REQUIRED"));
    }

    @Test
    void shouldRejectInvalidDateParameter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .param("from", "2026-99-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(adminReservationSupervisionService);
    }

    @Test
    void shouldRejectInvalidDateRange() throws Exception {
        when(adminReservationSupervisionService.getReservations(
                "admin@reservas.test",
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 6, 1)))
                .thenThrow(new ApiException("INVALID_DATE_RANGE", "El rango de fechas no es valido"));

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .param("from", "2026-06-30")
                        .param("to", "2026-06-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_DATE_RANGE"));
    }

    @Test
    void shouldRejectInvalidStatusFilter() throws Exception {
        when(adminReservationSupervisionService.getReservations("admin@reservas.test", null, null, null, "NO_VALIDO", null, null))
                .thenThrow(new ApiException("INVALID_RESERVATION_STATUS", "El estado de reserva solicitado no es valido"));

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .param("status", "NO_VALIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RESERVATION_STATUS"));
    }

    @Test
    void shouldRejectNegativeIdentifierFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .param("customerId", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(adminReservationSupervisionService);
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(adminReservationSupervisionService.getReservations("admin@reservas.test", null, null, null, null, null, null))
                .thenThrow(new AdminReservationQueryFailedException(
                        "No fue posible consultar las reservas administrativas. Intenta nuevamente mas tarde"));

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_RESERVATION_QUERY_FAILED"));
    }

    private AdminReservationSupervisionItemResponse response() {
        return new AdminReservationSupervisionItemResponse(
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
                OffsetDateTime.parse("2026-05-01T15:30:00Z"),
                OffsetDateTime.parse("2026-05-02T13:00:00Z"),
                null,
                null);
    }
}