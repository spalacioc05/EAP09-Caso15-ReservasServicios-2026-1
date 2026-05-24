package com.eap09.reservas.administration.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.administration.api.dto.UserRoleUpdateResponse;
import com.eap09.reservas.administration.application.UserRoleAdministrationService;
import com.eap09.reservas.common.exception.AdminRoleRequiredException;
import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.ResourceNotFoundException;
import com.eap09.reservas.common.exception.TargetUserInactiveException;
import com.eap09.reservas.common.exception.UserRoleAlreadyAssignedException;
import com.eap09.reservas.common.exception.UserRoleUpdateFailedException;
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

@WebMvcTest(controllers = UserRoleAdministrationController.class)
@AutoConfigureMockMvc
@Import({
        ControllerAdviceTestConfig.class,
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        JwtAuthenticationFilter.class
})
class UserRoleAdministrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRoleAdministrationService userRoleAdministrationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldUpdateUserRoleSuccessfully() throws Exception {
        when(userRoleAdministrationService.updateUserRole(eq("admin@reservas.test"), eq(25L), any()))
                .thenReturn(new UserRoleUpdateResponse(25L, "proveedor@reservas.test", "CLIENTE", "PROVEEDOR"));

        mockMvc.perform(patch("/api/v1/admin/users/25/role")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName":"PROVEEDOR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rol actualizado correctamente"))
                .andExpect(jsonPath("$.data.idUsuario").value(25))
                .andExpect(jsonPath("$.data.rolAnterior").value("CLIENTE"))
                .andExpect(jsonPath("$.data.rolActual").value("PROVEEDOR"));
    }

    @Test
    void shouldRejectWhenUserAlreadyHasRequestedRole() throws Exception {
        when(userRoleAdministrationService.updateUserRole(eq("admin@reservas.test"), eq(25L), any()))
                .thenThrow(new UserRoleAlreadyAssignedException("El usuario ya tiene asignado ese rol"));

        mockMvc.perform(patch("/api/v1/admin/users/25/role")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName":"CLIENTE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USER_ROLE_ALREADY_ASSIGNED"))
                .andExpect(jsonPath("$.message").value("El usuario ya tiene asignado ese rol"));
    }

    @Test
    void shouldRejectWhenTargetUserIsInactive() throws Exception {
        when(userRoleAdministrationService.updateUserRole(eq("admin@reservas.test"), eq(25L), any()))
                .thenThrow(new TargetUserInactiveException("El usuario se encuentra desactivado"));

        mockMvc.perform(patch("/api/v1/admin/users/25/role")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName":"PROVEEDOR"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TARGET_USER_INACTIVE"))
                .andExpect(jsonPath("$.message").value("El usuario se encuentra desactivado"));
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotAdmin() throws Exception {
        when(userRoleAdministrationService.updateUserRole(eq("cliente@reservas.test"), eq(25L), any()))
                .thenThrow(new AdminRoleRequiredException(
                        "Solo un administrador autenticado puede actualizar roles de usuario"));

        mockMvc.perform(patch("/api/v1/admin/users/25/role")
                        .with(user("cliente@reservas.test").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName":"PROVEEDOR"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_ROLE_REQUIRED"));
    }

    @Test
    void shouldRejectWhenTargetUserDoesNotExist() throws Exception {
        when(userRoleAdministrationService.updateUserRole(eq("admin@reservas.test"), eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("USER_NOT_FOUND", "El usuario indicado no existe"));

        mockMvc.perform(patch("/api/v1/admin/users/99/role")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName":"PROVEEDOR"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    void shouldRejectWhenRoleNameIsInvalid() throws Exception {
        when(userRoleAdministrationService.updateUserRole(eq("admin@reservas.test"), eq(25L), any()))
                .thenThrow(new ApiException("ROLE_NAME_INVALID", "El rol indicado no existe"));

        mockMvc.perform(patch("/api/v1/admin/users/25/role")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName":"SUPERVISOR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ROLE_NAME_INVALID"));
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/25/role")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(userRoleAdministrationService);
    }

    @Test
    void shouldRejectWhenNoAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/25/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName":"PROVEEDOR"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnControlledInternalError() throws Exception {
        when(userRoleAdministrationService.updateUserRole(eq("admin@reservas.test"), eq(25L), any()))
                .thenThrow(new UserRoleUpdateFailedException(
                        "No fue posible completar la actualizacion del rol del usuario. Intenta nuevamente mas tarde"));

        mockMvc.perform(patch("/api/v1/admin/users/25/role")
                        .with(user("admin@reservas.test").roles("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName":"PROVEEDOR"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("USER_ROLE_UPDATE_FAILED"));
    }
}