package com.eap09.reservas.identityaccess.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.EmailAlreadyRegisteredException;
import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.identityaccess.api.dto.ProviderRegistrationResponse;
import com.eap09.reservas.identityaccess.application.ProviderRegistrationService;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProviderRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProviderRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProviderRegistrationService providerRegistrationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldRegisterProviderSuccessfully() throws Exception {
        ProviderRegistrationResponse response = new ProviderRegistrationResponse(
                201L,
                "proveedor@example.com",
                "PROVEEDOR",
                "ACTIVA"
        );

        when(providerRegistrationService.registerProvider(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Paula",
                                  "apellidos":"Lopez",
                                  "correo":"proveedor@example.com",
                                  "contrasena":"Password1!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Proveedor registrado correctamente"))
                .andExpect(jsonPath("$.data.idUsuario").value(201))
                .andExpect(jsonPath("$.data.correo").value("proveedor@example.com"))
                .andExpect(jsonPath("$.data.rol").value("PROVEEDOR"))
                .andExpect(jsonPath("$.data.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.data.hash").doesNotExist())
                .andExpect(jsonPath("$.data.contrasena").doesNotExist());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        when(providerRegistrationService.registerProvider(any()))
                .thenThrow(new EmailAlreadyRegisteredException("El correo ingresado ya esta registrado"));

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Paula",
                                  "apellidos":"Lopez",
                                  "correo":"proveedor@example.com",
                                  "contrasena":"Password1!"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message").value("El correo ingresado ya esta registrado"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldRejectMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"",
                                  "apellidos":"",
                                  "correo":"",
                                  "contrasena":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validacion de la solicitud fallida"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Paula",
                                  "apellidos":"Lopez",
                                  "correo":"correo-invalido",
                                  "contrasena":"Password1!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectWeakPassword() throws Exception {
        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Paula",
                                  "apellidos":"Lopez",
                                  "correo":"proveedor@example.com",
                                  "contrasena":"weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnInternalErrorWhenUnexpectedFailure() throws Exception {
        when(providerRegistrationService.registerProvider(any()))
                .thenThrow(new RuntimeException("db unavailable"));

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Paula",
                                  "apellidos":"Lopez",
                                  "correo":"proveedor@example.com",
                                  "contrasena":"Password1!"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("No fue posible completar la solicitud"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
