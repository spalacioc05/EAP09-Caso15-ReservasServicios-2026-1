package com.eap09.reservas.customerbooking.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ReservationConflictException;
import com.eap09.reservas.customerbooking.api.dto.ReservationFinalizationResponse;
import com.eap09.reservas.customerbooking.application.ProviderReservationFinalizationService;
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

@WebMvcTest(controllers = ProviderReservationFinalizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProviderReservationFinalizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProviderReservationFinalizationService providerReservationFinalizationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldFinalizeBooking() throws Exception {
        when(providerReservationFinalizationService.finalizeOwnBooking(eq("provider@test.local"), eq(100L)))
                .thenReturn(new ReservationFinalizationResponse(100L, "FINALIZADA", OffsetDateTime.now()));

        mockMvc.perform(patch("/api/v1/providers/me/bookings/100/finalization")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reserva finalizada correctamente"))
                .andExpect(jsonPath("$.data.bookingId").value(100))
                .andExpect(jsonPath("$.data.bookingStatus").value("FINALIZADA"));
    }

    @Test
    void shouldReturnConflictWhenNotFinalizable() throws Exception {
        when(providerReservationFinalizationService.finalizeOwnBooking(eq("provider@test.local"), eq(100L)))
                .thenThrow(new ReservationConflictException("BOOKING_SLOT_NOT_FINISHED", "La reserva solo puede finalizarse una vez concluida su franja"));

        mockMvc.perform(patch("/api/v1/providers/me/bookings/100/finalization")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BOOKING_SLOT_NOT_FINISHED"));
    }

    @Test
    void shouldRejectWhenRoleIsNotProvider() throws Exception {
        when(providerReservationFinalizationService.finalizeOwnBooking(eq("client@test.local"), eq(100L)))
                .thenThrow(new ProviderRoleRequiredException("Solo un proveedor autenticado puede finalizar reservas"));

        mockMvc.perform(patch("/api/v1/providers/me/bookings/100/finalization")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROVIDER_ROLE_REQUIRED"));
    }
}