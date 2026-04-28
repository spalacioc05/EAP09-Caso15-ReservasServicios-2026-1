package com.eap09.reservas.customerbooking.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.customerbooking.api.dto.ReservationCancellationResponse;
import com.eap09.reservas.customerbooking.application.CustomerReservationCancellationService;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CustomerReservationCancellationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CustomerReservationCancellationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerReservationCancellationService customerReservationCancellationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldCancelBooking() throws Exception {
        when(customerReservationCancellationService.cancelOwnBooking(eq("customer@test.local"), eq(101L)))
                .thenReturn(new ReservationCancellationResponse(101L, "CANCELADA", OffsetDateTime.now()));

        mockMvc.perform(patch("/api/v1/bookings/101/cancellation")
                        .principal(new UsernamePasswordAuthenticationToken("customer@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reserva cancelada correctamente"))
                .andExpect(jsonPath("$.data.bookingId").value(101))
                .andExpect(jsonPath("$.data.bookingStatus").value("CANCELADA"));
    }

    @Test
    void shouldReturnConflictWhenNotCancelable() throws Exception {
        when(customerReservationCancellationService.cancelOwnBooking(eq("customer@test.local"), eq(101L)))
                .thenThrow(new ReservationConflictException("BOOKING_SLOT_ALREADY_STARTED", "La reserva ya no puede ser cancelada porque su franja ha iniciado"));

        mockMvc.perform(patch("/api/v1/bookings/101/cancellation")
                        .principal(new UsernamePasswordAuthenticationToken("customer@test.local", "N/A")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BOOKING_SLOT_ALREADY_STARTED"));
    }

    @Test
    void shouldRejectWhenRoleIsNotClient() throws Exception {
        when(customerReservationCancellationService.cancelOwnBooking(eq("provider@test.local"), eq(101L)))
                .thenThrow(new ClientRoleRequiredException("Solo un cliente autenticado puede cancelar reservas"));

        mockMvc.perform(patch("/api/v1/bookings/101/cancellation")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_ROLE_REQUIRED"));
    }
}