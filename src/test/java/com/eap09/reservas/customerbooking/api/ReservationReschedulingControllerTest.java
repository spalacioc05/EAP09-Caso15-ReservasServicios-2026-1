package com.eap09.reservas.customerbooking.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.common.exception.ReservationReschedulingFailedException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.customerbooking.api.dto.ReservationReschedulingResponse;
import com.eap09.reservas.customerbooking.application.ReservationReschedulingService;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReservationReschedulingController.class)
@AutoConfigureMockMvc
@Import({
        ControllerAdviceTestConfig.class,
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtAuthenticationFilter.class
})
class ReservationReschedulingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationReschedulingService reservationReschedulingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldRescheduleReservationSuccessfully() throws Exception {
        when(reservationReschedulingService.rescheduleOwnBooking(eq("customer@test.local"), eq(101L), any()))
                .thenReturn(new ReservationReschedulingResponse(
                        101L,
                        700L,
                        701L,
                        LocalDate.of(2026, 5, 3),
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0),
                        LocalDate.of(2026, 5, 5),
                        LocalTime.of(11, 0),
                        LocalTime.of(12, 0),
                        "CREADA",
                        OffsetDateTime.parse("2026-05-01T10:00:00Z")));

        mockMvc.perform(patch("/api/v1/bookings/101/reschedule")
                        .with(user("customer@test.local").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityId":701
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reserva reprogramada correctamente"))
                .andExpect(jsonPath("$.data.bookingId").value(101))
                .andExpect(jsonPath("$.data.previousAvailabilityId").value(700))
                .andExpect(jsonPath("$.data.newAvailabilityId").value(701))
                .andExpect(jsonPath("$.data.bookingStatus").value("CREADA"));
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(patch("/api/v1/bookings/101/reschedule")
                        .with(user("customer@test.local").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityId": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(reservationReschedulingService);
    }

    @Test
    void shouldRejectWhenNoAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/bookings/101/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityId":701
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotClient() throws Exception {
        when(reservationReschedulingService.rescheduleOwnBooking(eq("admin@test.local"), eq(101L), any()))
                .thenThrow(new ClientRoleRequiredException("Solo un cliente autenticado puede reprogramar reservas"));

        mockMvc.perform(patch("/api/v1/bookings/101/reschedule")
                        .with(user("admin@test.local").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityId":701
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_ROLE_REQUIRED"));
    }

    @Test
    void shouldRejectWhenReschedulingWindowHasExpired() throws Exception {
        when(reservationReschedulingService.rescheduleOwnBooking(eq("customer@test.local"), eq(101L), any()))
                .thenThrow(new ReservationConflictException(
                        "RESERVATION_RESCHEDULING_TOO_LATE",
                        "No es posible reprogramar una reserva con menos de 24 horas de antelacion"));

        mockMvc.perform(patch("/api/v1/bookings/101/reschedule")
                        .with(user("customer@test.local").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityId":701
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESERVATION_RESCHEDULING_TOO_LATE"));
    }

    @Test
    void shouldRejectWhenTargetAvailabilityIsUnavailable() throws Exception {
        when(reservationReschedulingService.rescheduleOwnBooking(eq("customer@test.local"), eq(101L), any()))
                .thenThrow(new ReservationConflictException(
                        "RESERVATION_RESCHEDULING_SLOT_UNAVAILABLE",
                        "No es posible reprogramar la reserva porque la fecha elegida no esta disponible"));

        mockMvc.perform(patch("/api/v1/bookings/101/reschedule")
                        .with(user("customer@test.local").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityId":701
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESERVATION_RESCHEDULING_SLOT_UNAVAILABLE"));
    }

    @Test
    void shouldRejectWhenBookingDoesNotExist() throws Exception {
        when(reservationReschedulingService.rescheduleOwnBooking(eq("customer@test.local"), eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("BOOKING_NOT_FOUND", "La reserva indicada no existe"));

        mockMvc.perform(patch("/api/v1/bookings/999/reschedule")
                        .with(user("customer@test.local").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityId":701
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BOOKING_NOT_FOUND"));
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(reservationReschedulingService.rescheduleOwnBooking(eq("customer@test.local"), eq(101L), any()))
                .thenThrow(new ReservationReschedulingFailedException(
                        "No fue posible completar la reprogramacion de la reserva. Intenta nuevamente mas tarde"));

        mockMvc.perform(patch("/api/v1/bookings/101/reschedule")
                        .with(user("customer@test.local").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityId":701
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("RESERVATION_RESCHEDULING_FAILED"));
    }
}