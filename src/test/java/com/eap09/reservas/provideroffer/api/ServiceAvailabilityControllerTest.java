package com.eap09.reservas.provideroffer.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.AvailabilityOverlapException;
import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.provideroffer.api.dto.ServiceAvailabilityResponse;
import com.eap09.reservas.provideroffer.application.ServiceAvailabilityService;
import com.eap09.reservas.security.application.JwtService;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ServiceAvailabilityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ServiceAvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceAvailabilityService serviceAvailabilityService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void shouldCreateAvailabilitySuccessfully() throws Exception {
        ServiceAvailabilityResponse response = new ServiceAvailabilityResponse(
                500L, 200L, LocalDate.of(2026, 4, 7), LocalTime.of(9, 0), LocalTime.of(10, 0), "HABILITADA");

        when(serviceAvailabilityService.createAvailability(eq("provider@test.local"), eq(200L), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/providers/me/services/200/availabilities")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fecha":"2026-04-07",
                                  "horaInicio":"09:00:00",
                                  "horaFin":"10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Disponibilidad creada correctamente"))
                .andExpect(jsonPath("$.data.idDisponibilidad").value(500))
                .andExpect(jsonPath("$.data.estadoDisponibilidad").value("HABILITADA"));
    }

    @Test
    void shouldRejectCreateWithMissingFields() throws Exception {
        mockMvc.perform(post("/api/v1/providers/me/services/200/availabilities")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fecha":null,
                                  "horaInicio":null,
                                  "horaFin":null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectCreateWhenRangeInvalid() throws Exception {
        when(serviceAvailabilityService.createAvailability(eq("provider@test.local"), eq(200L), any()))
                .thenThrow(new ApiException("INVALID_TIME_RANGE", "La hora de fin debe ser posterior a la hora de inicio"));

        mockMvc.perform(post("/api/v1/providers/me/services/200/availabilities")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fecha":"2026-04-07",
                                  "horaInicio":"10:00:00",
                                  "horaFin":"09:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TIME_RANGE"));
    }

    @Test
    void shouldRejectCreateWhenOverlapping() throws Exception {
        when(serviceAvailabilityService.createAvailability(eq("provider@test.local"), eq(200L), any()))
                .thenThrow(new AvailabilityOverlapException("La franja propuesta se superpone con una disponibilidad existente"));

        mockMvc.perform(post("/api/v1/providers/me/services/200/availabilities")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fecha":"2026-04-07",
                                  "horaInicio":"09:00:00",
                                  "horaFin":"10:00:00"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("AVAILABILITY_OVERLAP"));
    }

    @Test
    void shouldRejectCreateWhenNoAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/providers/me/services/200/availabilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fecha":"2026-04-07",
                                  "horaInicio":"09:00:00",
                                  "horaFin":"10:00:00"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectCreateWhenNotProvider() throws Exception {
        when(serviceAvailabilityService.createAvailability(eq("client@test.local"), eq(200L), any()))
                .thenThrow(new ProviderRoleRequiredException("Solo un proveedor autenticado puede gestionar disponibilidad"));

        mockMvc.perform(post("/api/v1/providers/me/services/200/availabilities")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fecha":"2026-04-07",
                                  "horaInicio":"09:00:00",
                                  "horaFin":"10:00:00"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROVIDER_ROLE_REQUIRED"));
    }

    @Test
    void shouldBlockAvailabilitySuccessfully() throws Exception {
        ServiceAvailabilityResponse response = new ServiceAvailabilityResponse(
                500L, 200L, LocalDate.of(2026, 4, 7), LocalTime.of(9, 0), LocalTime.of(10, 0), "BLOQUEADA");

        when(serviceAvailabilityService.blockAvailability("provider@test.local", 200L, 500L))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/providers/me/services/200/availabilities/500/block")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Disponibilidad bloqueada correctamente"))
                .andExpect(jsonPath("$.data.estadoDisponibilidad").value("BLOQUEADA"));
    }

    @Test
    void shouldRejectBlockWhenNotFound() throws Exception {
        when(serviceAvailabilityService.blockAvailability("provider@test.local", 200L, 500L))
                .thenThrow(new ResourceNotFoundException("AVAILABILITY_NOT_FOUND", "La disponibilidad indicada no existe"));

        mockMvc.perform(patch("/api/v1/providers/me/services/200/availabilities/500/block")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("AVAILABILITY_NOT_FOUND"));
    }

    @Test
    void shouldRejectBlockWhenNoAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/providers/me/services/200/availabilities/500/block"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }
}