package com.eap09.reservas.administration.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.administration.api.dto.UserAccountStatusUpdateResponse;
import com.eap09.reservas.administration.application.UserAccountStatusAdministrationService;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.exception.UserAccountStatusAlreadySetException;
import com.eap09.reservas.common.exception.UserAccountStatusUpdateFailedException;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import com.eap09.reservas.security.config.SecurityConfig;
import com.eap09.reservas.security.infrastructure.JwtAuthenticationFilter;
import com.eap09.reservas.security.infrastructure.RestAccessDeniedHandler;
import com.eap09.reservas.security.infrastructure.RestAuthenticationEntryPoint;
import com.eap09.reservas.support.ControllerAdviceTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserAccountStatusAdministrationController.class)
@AutoConfigureMockMvc
@Import({
        ControllerAdviceTestConfig.class,
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtAuthenticationFilter.class
})
class UserAccountStatusAdministrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAccountStatusAdministrationService userAccountStatusAdministrationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldActivateUserAccountSuccessfully() throws Exception {
        when(userAccountStatusAdministrationService.updateUserAccountStatus(eq("admin@reservas.test"), eq(25L), any()))
                .thenReturn(new UserAccountStatusUpdateResponse(25L, "usuario@reservas.test", "INACTIVA", "ACTIVA"));

        mockMvc.perform(patch("/api/v1/admin/users/25/status")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"ACTIVA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta de usuario activada correctamente"))
                .andExpect(jsonPath("$.data.idUsuario").value(25))
                .andExpect(jsonPath("$.data.estadoAnterior").value("INACTIVA"))
                .andExpect(jsonPath("$.data.estadoActual").value("ACTIVA"));
    }

    @Test
    void shouldDeactivateUserAccountSuccessfully() throws Exception {
        when(userAccountStatusAdministrationService.updateUserAccountStatus(eq("admin@reservas.test"), eq(25L), any()))
                .thenReturn(new UserAccountStatusUpdateResponse(25L, "usuario@reservas.test", "ACTIVA", "INACTIVA"));

        mockMvc.perform(patch("/api/v1/admin/users/25/status")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"INACTIVA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cuenta de usuario inactivada correctamente"))
                .andExpect(jsonPath("$.data.estadoAnterior").value("ACTIVA"))
                .andExpect(jsonPath("$.data.estadoActual").value("INACTIVA"));
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/25/status")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(userAccountStatusAdministrationService);
    }

    @Test
    void shouldRejectWhenTargetStatusIsInvalid() throws Exception {
        when(userAccountStatusAdministrationService.updateUserAccountStatus(eq("admin@reservas.test"), eq(25L), any()))
                .thenThrow(new ApiException("INVALID_USER_ACCOUNT_STATUS", "El estado solicitado no es valido para usuarios"));

        mockMvc.perform(patch("/api/v1/admin/users/25/status")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"BLOQUEADA"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_USER_ACCOUNT_STATUS"));
    }

    @Test
    void shouldRejectWhenNoAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/25/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"ACTIVA"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotAdmin() throws Exception {
        when(userAccountStatusAdministrationService.updateUserAccountStatus(eq("cliente@reservas.test"), eq(25L), any()))
                .thenThrow(new AdminRoleRequiredException(
                        "Solo un administrador autenticado puede actualizar el estado de cuentas de usuario"));

        mockMvc.perform(patch("/api/v1/admin/users/25/status")
                        .with(user("cliente@reservas.test").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"INACTIVA"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_ROLE_REQUIRED"));
    }

    @Test
    void shouldRejectWhenActivatingAlreadyActiveAccount() throws Exception {
        when(userAccountStatusAdministrationService.updateUserAccountStatus(eq("admin@reservas.test"), eq(25L), any()))
                .thenThrow(new UserAccountStatusAlreadySetException("La cuenta ya se encontraba activada"));

        mockMvc.perform(patch("/api/v1/admin/users/25/status")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"ACTIVA"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ACCOUNT_STATUS_ALREADY_SET"))
                .andExpect(jsonPath("$.message").value("La cuenta ya se encontraba activada"));
    }

    @Test
    void shouldRejectWhenDeactivatingAlreadyInactiveAccount() throws Exception {
        when(userAccountStatusAdministrationService.updateUserAccountStatus(eq("admin@reservas.test"), eq(25L), any()))
                .thenThrow(new UserAccountStatusAlreadySetException("La cuenta ya se encontraba desactivada"));

        mockMvc.perform(patch("/api/v1/admin/users/25/status")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"INACTIVA"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ACCOUNT_STATUS_ALREADY_SET"))
                .andExpect(jsonPath("$.message").value("La cuenta ya se encontraba desactivada"));
    }

    @Test
    void shouldRejectWhenTargetUserDoesNotExist() throws Exception {
        when(userAccountStatusAdministrationService.updateUserAccountStatus(eq("admin@reservas.test"), eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));

        mockMvc.perform(patch("/api/v1/admin/users/99/status")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"ACTIVA"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(userAccountStatusAdministrationService.updateUserAccountStatus(eq("admin@reservas.test"), eq(25L), any()))
                .thenThrow(new UserAccountStatusUpdateFailedException(
                        "No fue posible completar la actualizacion del estado de la cuenta de usuario. Intenta nuevamente mas tarde"));

        mockMvc.perform(patch("/api/v1/admin/users/25/status")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetStatus":"INACTIVA"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("USER_ACCOUNT_STATUS_UPDATE_FAILED"));
    }
}