package com.eap09.reservas.customerbooking.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.common.exception.ReservationCreationFailedException;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationRequest;
import com.eap09.reservas.customerbooking.api.dto.CreateReservationResponse;
import com.eap09.reservas.customerbooking.application.ReservationService;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

@WebMvcTest(controllers = ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

        @MockBean
        private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldCreateReservationSuccessfully() throws Exception {
        when(reservationService.createReservation(
                eq(new CreateReservationRequest(10L, 20L, 30L)),
                eq("client@test.local")))
                .thenReturn(new CreateReservationResponse(
                        901L,
                        10L,
                        20L,
                        30L,
                        40L,
                        LocalDate.of(2026, 4, 20),
                        "CREADA",
                        OffsetDateTime.parse("2026-04-02T12:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId": 10,
                                  "serviceId": 20,
                                  "availabilityId": 30
                                }
                                """)
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Reserva creada correctamente"))
                .andExpect(jsonPath("$.data.bookingId").value(901))
                .andExpect(jsonPath("$.data.bookingStatus").value("CREADA"));
    }

    @Test
    void shouldRejectWhenRequiredFieldsAreMissing() throws Exception {
        when(reservationService.createReservation(
                eq(new CreateReservationRequest(null, 20L, 30L)),
                eq("client@test.local")))
                .thenThrow(new ApiException("REQUIRED_FIELDS_MISSING", "Proveedor, servicio y franja son requeridos"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceId": 20,
                                  "availabilityId": 30
                                }
                                """)
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQUIRED_FIELDS_MISSING"))
                .andExpect(jsonPath("$.message").value("Proveedor, servicio y franja son requeridos"));
    }

    @Test
    void shouldRejectWhenServiceIsInactive() throws Exception {
        when(reservationService.createReservation(
                eq(new CreateReservationRequest(10L, 20L, 30L)),
                eq("client@test.local")))
                .thenThrow(new ReservationConflictException("SERVICE_NOT_AVAILABLE", "El servicio no esta disponible"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId": 10,
                                  "serviceId": 20,
                                  "availabilityId": 30
                                }
                                """)
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_NOT_AVAILABLE"));
    }

    @Test
    void shouldRejectWhenProviderIsInactive() throws Exception {
        when(reservationService.createReservation(
                eq(new CreateReservationRequest(10L, 20L, 30L)),
                eq("client@test.local")))
                .thenThrow(new ReservationConflictException("PROVIDER_NOT_AVAILABLE", "El proveedor no esta disponible"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId": 10,
                                  "serviceId": 20,
                                  "availabilityId": 30
                                }
                                """)
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PROVIDER_NOT_AVAILABLE"));
    }

    @Test
    void shouldRejectWhenAvailabilityIsNotReservable() throws Exception {
        when(reservationService.createReservation(
                eq(new CreateReservationRequest(10L, 20L, 30L)),
                eq("client@test.local")))
                .thenThrow(new ReservationConflictException("AVAILABILITY_NOT_RESERVABLE", "La franja seleccionada ya no puede reservarse"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId": 10,
                                  "serviceId": 20,
                                  "availabilityId": 30
                                }
                                """)
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("AVAILABILITY_NOT_RESERVABLE"));
    }

    @Test
    void shouldRejectWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId": 10,
                                  "serviceId": 20,
                                  "availabilityId": 30
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectWhenRoleIsNotClient() throws Exception {
        when(reservationService.createReservation(
                eq(new CreateReservationRequest(10L, 20L, 30L)),
                eq("provider@test.local")))
                .thenThrow(new ClientRoleRequiredException("Solo un cliente autenticado puede crear reservas"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId": 10,
                                  "serviceId": 20,
                                  "availabilityId": 30
                                }
                                """)
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_ROLE_REQUIRED"));
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(reservationService.createReservation(
                eq(new CreateReservationRequest(10L, 20L, 30L)),
                eq("client@test.local")))
                .thenThrow(new ReservationCreationFailedException("No fue posible completar la reserva. Intenta nuevamente mas tarde"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerId": 10,
                                  "serviceId": 20,
                                  "availabilityId": 30
                                }
                                """)
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("RESERVATION_CREATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details.length()").value(0));
    }
}
