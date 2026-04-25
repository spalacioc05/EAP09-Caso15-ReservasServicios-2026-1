package com.eap09.reservas.identityaccess.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.ApiException;
import com.eap09.reservas.common.exception.EmailAlreadyRegisteredException;
import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.common.exception.ProfileNoChangesException;
import com.eap09.reservas.identityaccess.api.dto.UpdateOwnProfileResponse;
import com.eap09.reservas.identityaccess.application.UserProfileService;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    @WithMockUser(username = "cliente@reservas.test")
    void shouldUpdateOwnProfileSuccessfully() throws Exception {
        UpdateOwnProfileResponse response = new UpdateOwnProfileResponse(
                10L,
                "Ana Maria",
                "Cliente",
                "cliente.nuevo@reservas.test"
        );
        when(userProfileService.updateOwnProfile(any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Ana Maria",
                                  "correo":"cliente.nuevo@reservas.test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Perfil actualizado correctamente"))
                .andExpect(jsonPath("$.data.idUsuario").value(10))
                .andExpect(jsonPath("$.data.nombres").value("Ana Maria"))
                .andExpect(jsonPath("$.data.apellidos").value("Cliente"))
                .andExpect(jsonPath("$.data.correo").value("cliente.nuevo@reservas.test"));
    }

    @Test
    @WithMockUser(username = "cliente@reservas.test")
    void shouldRejectInvalidPayloadByBeanValidation() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "cliente@reservas.test")
    void shouldRejectInvalidEmailFormat() throws Exception {
        when(userProfileService.updateOwnProfile(any(), any()))
                .thenThrow(new ApiException("PROFILE_EMAIL_INVALID", "El correo ingresado no es valido"));

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo":"correo-invalido"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PROFILE_EMAIL_INVALID"))
                .andExpect(jsonPath("$.message").value("El correo ingresado no es valido"));
    }

    @Test
    @WithMockUser(username = "cliente@reservas.test")
    void shouldRejectBlankProvidedValue() throws Exception {
        when(userProfileService.updateOwnProfile(any(), any()))
                .thenThrow(new ApiException("PROFILE_NAME_REQUIRED", "nombres no puede estar vacio"));

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PROFILE_NAME_REQUIRED"));
    }

    @Test
    @WithMockUser(username = "cliente@reservas.test")
    void shouldRejectDuplicateEmail() throws Exception {
        when(userProfileService.updateOwnProfile(any(), any()))
                .thenThrow(new EmailAlreadyRegisteredException("El correo ingresado ya esta registrado"));

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo":"duplicado@reservas.test"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    @WithMockUser(username = "cliente@reservas.test")
    void shouldReturnConflictWhenNoRealChangesAreProvided() throws Exception {
        when(userProfileService.updateOwnProfile(any(), any()))
                .thenThrow(new ProfileNoChangesException("No existen cambios para aplicar"));

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Ana",
                                  "apellidos":"Cliente",
                                  "correo":"cliente@reservas.test"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PROFILE_NO_CHANGES"));
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Ana"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autenticacion requerida"));
    }
}