package com.eap09.reservas.provideroffer.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ServiceNameAlreadyExistsException;
import com.eap09.reservas.provideroffer.api.dto.ServiceRegistrationResponse;
import com.eap09.reservas.provideroffer.application.ServiceRegistrationService;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
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

@WebMvcTest(controllers = ServiceRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ServiceRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceRegistrationService serviceRegistrationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

        @MockBean
        private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldRegisterServiceSuccessfully() throws Exception {
        ServiceRegistrationResponse response = new ServiceRegistrationResponse(
                900L,
                "Masaje terapeutico",
                "Sesion de relajacion",
                60,
                2,
                "ACTIVO"
        );

        when(serviceRegistrationService.registerService(eq("provider@test.local"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/providers/me/services")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Masaje terapeutico",
                                  "descripcion":"Sesion de relajacion",
                                  "duracionMinutos":60,
                                  "capacidadMaximaConcurrente":2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Servicio registrado correctamente"))
                .andExpect(jsonPath("$.data.idServicio").value(900))
                .andExpect(jsonPath("$.data.estadoServicio").value("ACTIVO"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    void shouldRejectMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/providers/me/services")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"",
                                  "descripcion":"",
                                  "duracionMinutos":null,
                                  "capacidadMaximaConcurrente":null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectDuplicateServiceName() throws Exception {
        when(serviceRegistrationService.registerService(eq("provider@test.local"), any()))
                .thenThrow(new ServiceNameAlreadyExistsException(
                        "No es posible crear un servicio con nombre repetido para el mismo proveedor"));

        mockMvc.perform(post("/api/v1/providers/me/services")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Masaje terapeutico",
                                  "descripcion":"Sesion de relajacion",
                                  "duracionMinutos":60,
                                  "capacidadMaximaConcurrente":2
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_NAME_ALREADY_EXISTS"));
    }

    @Test
    void shouldRejectInvalidDuration() throws Exception {
        mockMvc.perform(post("/api/v1/providers/me/services")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Masaje terapeutico",
                                  "descripcion":"Sesion de relajacion",
                                  "duracionMinutos":0,
                                  "capacidadMaximaConcurrente":2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectInvalidCapacity() throws Exception {
        mockMvc.perform(post("/api/v1/providers/me/services")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Masaje terapeutico",
                                  "descripcion":"Sesion de relajacion",
                                  "duracionMinutos":60,
                                  "capacidadMaximaConcurrente":0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotProvider() throws Exception {
        when(serviceRegistrationService.registerService(eq("cliente@test.local"), any()))
                .thenThrow(new ProviderRoleRequiredException("Solo un proveedor autenticado puede registrar servicios"));

        mockMvc.perform(post("/api/v1/providers/me/services")
                        .principal(new UsernamePasswordAuthenticationToken("cliente@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Masaje terapeutico",
                                  "descripcion":"Sesion de relajacion",
                                  "duracionMinutos":60,
                                  "capacidadMaximaConcurrente":2
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROVIDER_ROLE_REQUIRED"));
    }

    @Test
    void shouldRejectWhenNoAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/providers/me/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Masaje terapeutico",
                                  "descripcion":"Sesion de relajacion",
                                  "duracionMinutos":60,
                                  "capacidadMaximaConcurrente":2
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void shouldReturnInternalErrorWhenUnexpectedFailure() throws Exception {
        when(serviceRegistrationService.registerService(eq("provider@test.local"), any()))
                .thenThrow(new RuntimeException("db unavailable"));

        mockMvc.perform(post("/api/v1/providers/me/services")
                        .principal(new UsernamePasswordAuthenticationToken("provider@test.local", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Masaje terapeutico",
                                  "descripcion":"Sesion de relajacion",
                                  "duracionMinutos":60,
                                  "capacidadMaximaConcurrente":2
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }
}