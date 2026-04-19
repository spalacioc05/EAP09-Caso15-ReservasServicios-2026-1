package com.eap09.reservas.identityaccess.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eap09.reservas.common.exception.EmailAlreadyRegisteredException;
import com.eap09.reservas.common.exception.GlobalExceptionHandler;
import com.eap09.reservas.identityaccess.api.dto.CustomerRegistrationResponse;
import com.eap09.reservas.identityaccess.application.CustomerRegistrationService;
import com.eap09.reservas.security.application.JwtService;
import com.eap09.reservas.security.application.SessionTokenValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;

@WebMvcTest(controllers = CustomerRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CustomerRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerRegistrationService customerRegistrationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionTokenValidationService sessionTokenValidationService;

    @Test
    void shouldRegisterCustomerSuccessfully() throws Exception {
        CustomerRegistrationResponse response = new CustomerRegistrationResponse(
                200L,
                "ana@example.com",
                "CLIENTE",
                "ACTIVA"
        );

        when(customerRegistrationService.registerCustomer(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Ana",
                                  "apellidos":"Perez",
                                  "correo":"ana@example.com",
                                  "contrasena":"Password1!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Cliente registrado correctamente"))
                .andExpect(jsonPath("$.data.idUsuario").value(200))
                .andExpect(jsonPath("$.data.correo").value("ana@example.com"))
                .andExpect(jsonPath("$.data.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.data.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.data.hash").doesNotExist())
                .andExpect(jsonPath("$.data.contrasena").doesNotExist());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        when(customerRegistrationService.registerCustomer(any()))
        .thenThrow(new EmailAlreadyRegisteredException("El correo ingresado ya esta registrado"));

      mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Ana",
                                  "apellidos":"Perez",
                                  "correo":"ana@example.com",
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
        mockMvc.perform(post("/api/v1/clients")
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
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Ana",
                                  "apellidos":"Perez",
                                  "correo":"invalid-email",
                                  "contrasena":"Password1!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectWeakPassword() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres":"Ana",
                                  "apellidos":"Perez",
                                  "correo":"ana@example.com",
                                  "contrasena":"weak"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
