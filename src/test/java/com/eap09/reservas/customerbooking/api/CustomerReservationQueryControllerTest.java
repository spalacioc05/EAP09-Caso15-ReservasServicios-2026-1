package com.eap09.reservas.customerbooking.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.CustomerReservationQueryFailedException;
import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.customerbooking.api.dto.CustomerReservationResponse;
import com.eap09.reservas.customerbooking.application.CustomerReservationQueryResult;
import com.eap09.reservas.customerbooking.application.CustomerReservationQueryService;
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

@WebMvcTest(controllers = CustomerReservationQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CustomerReservationQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerReservationQueryService customerReservationQueryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldReturnOwnBookings() throws Exception {
        when(customerReservationQueryService.getOwnBookings(eq("customer@test.local")))
                .thenReturn(new CustomerReservationQueryResult(
                        "Consulta de reservas del cliente exitosa",
                        List.of(response())));

        mockMvc.perform(get("/api/v1/bookings/me")
                        .principal(new UsernamePasswordAuthenticationToken("customer@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta de reservas del cliente exitosa"))
                .andExpect(jsonPath("$.data[0].bookingId").value(700))
                .andExpect(jsonPath("$.data[0].serviceName").value("Servicio HU19"));
    }

    @Test
    void shouldReturnEmptyListMessage() throws Exception {
        when(customerReservationQueryService.getOwnBookings(eq("customer@test.local")))
                .thenReturn(new CustomerReservationQueryResult("No existen reservas asociadas a tu cuenta", List.of()));

        mockMvc.perform(get("/api/v1/bookings/me")
                        .principal(new UsernamePasswordAuthenticationToken("customer@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No existen reservas asociadas a tu cuenta"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectWhenRoleIsNotClient() throws Exception {
        when(customerReservationQueryService.getOwnBookings(eq("provider@test.local")))
                .thenThrow(new ClientRoleRequiredException("Solo un cliente autenticado puede consultar sus reservas"));

        mockMvc.perform(get("/api/v1/bookings/me")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_ROLE_REQUIRED"));
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(customerReservationQueryService.getOwnBookings(eq("customer@test.local")))
                .thenThrow(new CustomerReservationQueryFailedException(
                        "No fue posible completar la consulta de reservas. Intenta nuevamente mas tarde"));

        mockMvc.perform(get("/api/v1/bookings/me")
                        .principal(new UsernamePasswordAuthenticationToken("customer@test.local", "N/A")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("CUSTOMER_BOOKING_QUERY_FAILED"));
    }

    private CustomerReservationResponse response() {
        return new CustomerReservationResponse(
                700L,
                300L,
                "Servicio HU19",
                10L,
                "Proveedor Uno",
                LocalDate.of(2026, 6, 1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "CREADA",
                OffsetDateTime.now());
    }
}