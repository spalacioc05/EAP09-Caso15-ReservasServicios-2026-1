package com.eap09.reservas.customerbooking.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.ClientRoleRequiredException;
import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.common.exception.OfferQueryFailedException;
import com.eap09.reservas.customerbooking.api.dto.OfferResponse;
import com.eap09.reservas.customerbooking.application.CustomerBookingOfferService;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
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

@WebMvcTest(controllers = CustomerBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CustomerBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerBookingOfferService customerBookingOfferService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

        @MockBean
        private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldReturnOfferSuccessfully() throws Exception {
        when(customerBookingOfferService.getAvailableOffers(eq("client@test.local")))
                .thenReturn(List.of(new OfferResponse(10L, "Servicio", "Descripcion", "Proveedor")));

        mockMvc.perform(get("/api/v1/offers")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Consulta de oferta exitosa"))
                .andExpect(jsonPath("$.data[0].serviceId").value(10))
                .andExpect(jsonPath("$.data[0].providerName").value("Proveedor"));
    }

    @Test
    void shouldReturnEmptyOfferMessageWhenNoResults() throws Exception {
        when(customerBookingOfferService.getAvailableOffers(eq("client@test.local")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/offers")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No hay oferta disponible para mostrar"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldRejectWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/offers"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectWhenRoleIsNotClient() throws Exception {
        when(customerBookingOfferService.getAvailableOffers(eq("provider@test.local")))
                .thenThrow(new ClientRoleRequiredException("Solo un cliente autenticado puede consultar la oferta"));

        mockMvc.perform(get("/api/v1/offers")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_ROLE_REQUIRED"));
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(customerBookingOfferService.getAvailableOffers(eq("client@test.local")))
                .thenThrow(new OfferQueryFailedException("No fue posible obtener la oferta. Intenta nuevamente mas tarde"));

        mockMvc.perform(get("/api/v1/offers")
                        .principal(new UsernamePasswordAuthenticationToken("client@test.local", "N/A")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("OFFER_QUERY_UNAVAILABLE"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details.length()").value(0));
    }
}
