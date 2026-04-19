package com.eap09.reservas.customerbooking.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.AvailabilityQueryFailedException;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.customerbooking.api.dto.AvailabilityResponse;
import com.eap09.reservas.customerbooking.application.AvailabilityQueryResult;
import com.eap09.reservas.customerbooking.application.CustomerBookingAvailabilityService;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import java.time.LocalDate;
import java.time.LocalTime;
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

@WebMvcTest(controllers = CustomerBookingAvailabilityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CustomerBookingAvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerBookingAvailabilityService customerBookingAvailabilityService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

        @MockBean
        private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldReturnAvailableSlotsSuccessfully() throws Exception {
        when(customerBookingAvailabilityService.getAvailability(
                eq(10L), eq(20L), eq(LocalDate.of(2026, 4, 20)), eq("client@test.local")))
                .thenReturn(new AvailabilityQueryResult(
                        "Consulta de horarios y cupos exitosa",
                        List.of(new AvailabilityResponse(100L, LocalTime.of(9, 0), LocalTime.of(10, 0), 2))));

        mockMvc.perform(get("/api/v1/providers/10/services/20/availabilities")
                        .param("date", "2026-04-20")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta de horarios y cupos exitosa"))
                .andExpect(jsonPath("$.data[0].availabilityId").value(100))
                .andExpect(jsonPath("$.data[0].remainingSlots").value(2));
    }

    @Test
    void shouldReturnInvalidRelationMessage() throws Exception {
        when(customerBookingAvailabilityService.getAvailability(
                eq(10L), eq(20L), eq(LocalDate.of(2026, 4, 20)), eq("client@test.local")))
                .thenReturn(new AvailabilityQueryResult("No existe disponibilidad para la seleccion realizada", List.of()));

        mockMvc.perform(get("/api/v1/providers/10/services/20/availabilities")
                        .param("date", "2026-04-20")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No existe disponibilidad para la seleccion realizada"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldReturnNoAvailabilityMessageForDate() throws Exception {
        when(customerBookingAvailabilityService.getAvailability(
                eq(10L), eq(20L), eq(LocalDate.of(2026, 4, 20)), eq("client@test.local")))
                .thenReturn(new AvailabilityQueryResult("No hay disponibilidad para reserva en la fecha seleccionada", List.of()));

        mockMvc.perform(get("/api/v1/providers/10/services/20/availabilities")
                        .param("date", "2026-04-20")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No hay disponibilidad para reserva en la fecha seleccionada"));
    }

    @Test
    void shouldRejectWhenRequiredFieldsAreMissing() throws Exception {
        when(customerBookingAvailabilityService.getAvailability(eq(10L), eq(20L), isNull(), eq("client@test.local")))
                .thenThrow(new ApiException("REQUIRED_FIELDS_MISSING", "Proveedor, servicio y fecha son requeridos"));

        mockMvc.perform(get("/api/v1/providers/10/services/20/availabilities")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQUIRED_FIELDS_MISSING"));
    }

    @Test
    void shouldRejectWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/providers/10/services/20/availabilities").param("date", "2026-04-20"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectWhenRoleIsNotClient() throws Exception {
        when(customerBookingAvailabilityService.getAvailability(
                eq(10L), eq(20L), eq(LocalDate.of(2026, 4, 20)), eq("provider@test.local")))
                .thenThrow(new ClientRoleRequiredException("Solo un cliente autenticado puede consultar horarios y cupos"));

        mockMvc.perform(get("/api/v1/providers/10/services/20/availabilities")
                        .param("date", "2026-04-20")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_ROLE_REQUIRED"));
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(customerBookingAvailabilityService.getAvailability(
                eq(10L), eq(20L), eq(LocalDate.of(2026, 4, 20)), eq("client@test.local")))
                .thenThrow(new AvailabilityQueryFailedException("No fue posible obtener la disponibilidad. Intenta nuevamente mas tarde"));

        mockMvc.perform(get("/api/v1/providers/10/services/20/availabilities")
                        .param("date", "2026-04-20")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("AVAILABILITY_QUERY_UNAVAILABLE"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details.length()").value(0));
    }
}
