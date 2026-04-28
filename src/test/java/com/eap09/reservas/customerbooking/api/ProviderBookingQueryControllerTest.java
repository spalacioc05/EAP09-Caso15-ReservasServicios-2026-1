package com.eap09.reservas.customerbooking.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.common.exception.ProviderReservationQueryFailedException;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.customerbooking.api.dto.ProviderBookingResponse;
import com.eap09.reservas.customerbooking.application.ProviderBookingQueryResult;
import com.eap09.reservas.customerbooking.application.ProviderBookingQueryService;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProviderBookingQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProviderBookingQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProviderBookingQueryService providerBookingQueryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldReturnOwnBookingsWithoutFilters() throws Exception {
        when(providerBookingQueryService.getOwnBookings(eq("provider@test.local"), eq(null), eq(null), eq(null)))
                .thenReturn(new ProviderBookingQueryResult(
                        "Consulta operativa de reservas exitosa",
                        List.of(response())));

        mockMvc.perform(get("/api/v1/providers/me/bookings")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta operativa de reservas exitosa"))
                .andExpect(jsonPath("$.data[0].bookingId").value(100))
                .andExpect(jsonPath("$.data[0].serviceName").value("Servicio HU12"));
    }

    @Test
    void shouldReturnOwnBookingsWithCombinedFilters() throws Exception {
        when(providerBookingQueryService.getOwnBookings(
                eq("provider@test.local"),
                eq(LocalDate.of(2026, 5, 1)),
                eq("CREADA"),
                eq(200L)))
                .thenReturn(new ProviderBookingQueryResult(
                        "Consulta operativa de reservas exitosa",
                        List.of(response())));

        mockMvc.perform(get("/api/v1/providers/me/bookings")
                        .param("date", "2026-05-01")
                        .param("status", "CREADA")
                        .param("serviceId", "200")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bookingStatus").value("CREADA"));
    }

    @Test
    void shouldReturnEmptyListWithControlledMessage() throws Exception {
        when(providerBookingQueryService.getOwnBookings(eq("provider@test.local"), eq(null), eq(null), eq(null)))
                .thenReturn(new ProviderBookingQueryResult(
                        "No existen reservas registradas para sus servicios",
                        List.of()));

        mockMvc.perform(get("/api/v1/providers/me/bookings")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No existen reservas registradas para sus servicios"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectInvalidDateParameter() throws Exception {
        mockMvc.perform(get("/api/v1/providers/me/bookings")
                        .param("date", "2026-99-99")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidStatusFilter() throws Exception {
        when(providerBookingQueryService.getOwnBookings(eq("provider@test.local"), eq(null), eq("NO_VALIDO"), eq(null)))
                .thenThrow(new ApiException("INVALID_RESERVATION_STATUS", "El estado de reserva consultado no es valido"));

        mockMvc.perform(get("/api/v1/providers/me/bookings")
                        .param("status", "NO_VALIDO")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_RESERVATION_STATUS"));
    }

    @Test
    void shouldRejectWhenNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/providers/me/bookings"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectWhenRoleIsNotProvider() throws Exception {
        when(providerBookingQueryService.getOwnBookings(eq("client@test.local"), eq(null), eq(null), eq(null)))
                .thenThrow(new ProviderRoleRequiredException("Solo un proveedor autenticado puede consultar reservas operativas"));

        mockMvc.perform(get("/api/v1/providers/me/bookings")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROVIDER_ROLE_REQUIRED"));
    }

    @Test
    void shouldRejectForeignServiceFilter() throws Exception {
        when(providerBookingQueryService.getOwnBookings(eq("provider@test.local"), eq(null), eq(null), eq(999L)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                        "No tiene permisos para consultar reservas de este servicio"));

        mockMvc.perform(get("/api/v1/providers/me/bookings")
                        .param("serviceId", "999")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(providerBookingQueryService.getOwnBookings(eq("provider@test.local"), eq(null), eq(null), eq(null)))
                .thenThrow(new ProviderReservationQueryFailedException(
                        "No fue posible completar la consulta de reservas. Intenta nuevamente mas tarde"));

        mockMvc.perform(get("/api/v1/providers/me/bookings")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("PROVIDER_BOOKING_QUERY_FAILED"));
    }

    private ProviderBookingResponse response() {
        return new ProviderBookingResponse(
                100L,
                200L,
                "Servicio HU12",
                300L,
                LocalDate.of(2026, 5, 1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                400L,
                "Cliente Uno",
                "cliente1@test.local",
                "CREADA",
                OffsetDateTime.now());
    }
}