package com.eap09.reservas.provideroffer.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.common.exception.ProviderRoleRequiredException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.exception.ServiceStatusAlreadySetException;
import com.eap09.reservas.common.exception.ServiceStatusChangeFailedException;
import com.eap09.reservas.provideroffer.api.dto.ServiceStatusUpdateResponse;
import com.eap09.reservas.provideroffer.application.ServiceRegistrationService;
import com.eap09.reservas.provideroffer.application.ServiceStatusManagementService;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import com.eap09.reservas.security.config.SecurityConfig;
import com.eap09.reservas.security.infrastructure.JwtAuthenticationFilter;
import com.eap09.reservas.security.infrastructure.RestAccessDeniedHandler;
import com.eap09.reservas.security.infrastructure.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ServiceRegistrationController.class)
@AutoConfigureMockMvc
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtAuthenticationFilter.class
})
class ServiceStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceRegistrationService serviceRegistrationService;

    @MockBean
    private ServiceStatusManagementService serviceStatusManagementService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldActivateServiceSuccessfully() throws Exception {
        when(serviceStatusManagementService.updateOwnServiceStatus(eq("provider@test.local"), eq(900L), any()))
                .thenReturn(new ServiceStatusUpdateResponse(900L, "Masaje terapeutico", "ACTIVO"));

        mockMvc.perform(patch("/api/v1/providers/me/services/900/status")
                        .with(user("provider@test.local").roles("PROVEEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"ACTIVO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Servicio activado correctamente"))
                .andExpect(jsonPath("$.data.idServicio").value(900))
                .andExpect(jsonPath("$.data.estadoServicio").value("ACTIVO"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    void shouldInactivateServiceSuccessfully() throws Exception {
        when(serviceStatusManagementService.updateOwnServiceStatus(eq("provider@test.local"), eq(901L), any()))
                .thenReturn(new ServiceStatusUpdateResponse(901L, "Masaje terapeutico", "INACTIVO"));

        mockMvc.perform(patch("/api/v1/providers/me/services/901/status")
                        .with(user("provider@test.local").roles("PROVEEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"INACTIVO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Servicio inactivado correctamente"))
                .andExpect(jsonPath("$.data.estadoServicio").value("INACTIVO"));
    }

    @Test
    void shouldRejectRedundantTargetState() throws Exception {
        when(serviceStatusManagementService.updateOwnServiceStatus(eq("provider@test.local"), eq(902L), any()))
                .thenThrow(new ServiceStatusAlreadySetException("El servicio ya se encuentra activo"));

        mockMvc.perform(patch("/api/v1/providers/me/services/902/status")
                        .with(user("provider@test.local").roles("PROVEEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"ACTIVO"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_STATUS_ALREADY_SET"));
    }

    @Test
    void shouldRejectForeignService() throws Exception {
        when(serviceStatusManagementService.updateOwnServiceStatus(eq("provider@test.local"), eq(903L), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                        "No tiene permisos para cambiar el estado de este servicio"));

        mockMvc.perform(patch("/api/v1/providers/me/services/903/status")
                        .with(user("provider@test.local").roles("PROVEEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"INACTIVO"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectWhenServiceDoesNotExist() throws Exception {
        when(serviceStatusManagementService.updateOwnServiceStatus(eq("provider@test.local"), eq(904L), any()))
                .thenThrow(new ResourceNotFoundException("SERVICE_NOT_FOUND", "El servicio indicado no existe"));

        mockMvc.perform(patch("/api/v1/providers/me/services/904/status")
                        .with(user("provider@test.local").roles("PROVEEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"ACTIVO"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_NOT_FOUND"));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotProvider() throws Exception {
        when(serviceStatusManagementService.updateOwnServiceStatus(eq("client@test.local"), eq(905L), any()))
                .thenThrow(new ProviderRoleRequiredException(
                        "Solo un proveedor autenticado puede cambiar el estado de sus servicios"));

        mockMvc.perform(patch("/api/v1/providers/me/services/905/status")
                        .with(user("client@test.local").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"ACTIVO"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PROVIDER_ROLE_REQUIRED"));
    }

    @Test
    void shouldRejectWhenNoAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/providers/me/services/906/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"ACTIVO"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(patch("/api/v1/providers/me/services/907/status")
                        .with(user("provider@test.local").roles("PROVEEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(serviceStatusManagementService.updateOwnServiceStatus(eq("provider@test.local"), eq(908L), any()))
                .thenThrow(new ServiceStatusChangeFailedException(
                        "No fue posible completar el cambio de estado del servicio. Intenta nuevamente mas tarde"));

        mockMvc.perform(patch("/api/v1/providers/me/services/908/status")
                        .with(user("provider@test.local").roles("PROVEEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"INACTIVO"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_STATUS_CHANGE_FAILED"));
    }
}