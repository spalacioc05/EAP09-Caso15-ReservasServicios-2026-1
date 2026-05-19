package com.eap09.reservas.integrationtests.identityacces;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eap09.reservas.ReservasApplication;
import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, 
                classes = ReservasApplication.class)

@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
class AuthenticationIntegrationTest {
    
    private final int MAX_FAILED_ATTEMPTS = 5;

    private UserAccountEntity user;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        cleanup();
        insertTestData();
    }

    void insertTestData(){

        user = new UserAccountEntity();
        user.setNombresUsuario("Camilo");
        user.setApellidosUsuario("Lopez");
        user.setCorreoUsuario("user@example.com");
        user.setHashContrasenaUsuario(passwordEncoder.encode("Password1!"));
        user.setIdEstado(1L);
        user.setIntentosFallidosConsecutivos(0);

        RoleEntity userRole = new RoleEntity();
        userRole.setIdRol(1L);
        userRole.setNombreRol("CLIENTE");
        user.setRol(userRole);

        UserAccountEntity savedUser = userAccountRepository.save(user);
        user.setIdUsuario(savedUser.getIdUsuario());
    }
        
    void cleanup() {
        String truncate_sql = """
            TRUNCATE TABLE tbl_evento, tbl_sesion_usuario,
            tbl_usuario RESTART IDENTITY CASCADE;
        """;
        jdbcTemplate.update(truncate_sql);
    }

    @Test
    @DisplayName("Should authenticate succesfully when correct credentials")
    void shouldAuthenticateWhenValidCredentials() throws Exception {
        
        mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "correo":"user@example.com",
                      "contrasena":"Password1!"
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Should Reject when invalid credentials")
    void shouldRejectInvalidCredentials() throws Exception {
         mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "correo":"user@example.com",
                      "contrasena":"Cliente123!"
                    }
                """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Credenciales no validas"));
    }

    @Test
    @DisplayName("Should Reject when not registered email")
    void shouldRejectNonExistingEmail() throws Exception {
         mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "correo":"non_existing_mail@example.com",
                      "contrasena":"Password1!"
                    }
                """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("Credenciales no validas"));
    }

    @Test
    @DisplayName("Should Reject when email empty field")
    void shouldRejectEmptyEmailField() throws Exception {
         mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "correo":"",
                      "contrasena":"Password1!"
                    }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("El campo email es oligatorio"));
    }

    @Test
    @DisplayName("Should Reject when email empty field")
    void shouldRejectEmptyPasswordField() throws Exception {
         mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "correo":"user@example.com",
                      "contrasena":""
                    }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("El campo contraseña es obligatorio"));
    }

    @Test
    @DisplayName("Should Reject when email and password empty fields")
    void shouldRejectEmptyFields() throws Exception {
         mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "correo":"",
                      "contrasena":""
                    }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Los campos de email y contraseña son obligatorios"));
    }

    @Test
    @DisplayName("Should apply account block when repeated invalid credentials")
    void shouldApplyAccountBlock() throws Exception {

        for(int i=0; i< MAX_FAILED_ATTEMPTS; i++){
            mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "correo":"user@example.com",
                      "contrasena":"WrongPassword1!"
                    }
                """));
        }
        // Block with wrong password
        mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "correo":"user@example.com",
                        "contrasena":"wrongPassword1!"
                    }
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACCESS_TEMPORARILY_RESTRICTED"))
            .andExpect(jsonPath("$.message").value("La cuenta tiene una restriccion temporal de acceso"));
        
        // Is still blocked with correct password
        mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "correo":"user@example.com",
                        "contrasena":"Password1!"
                    }
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACCESS_TEMPORARILY_RESTRICTED"))
            .andExpect(jsonPath("$.message").value("La cuenta tiene una restriccion temporal de acceso"));
    }

    @Test
    @DisplayName("Should Reject when account is innactive")
    void shouldRejectInnactiveAccount() throws Exception {

        // Account innactivation
        user.setIdEstado(2L);
        userAccountRepository.save(user);
        mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "correo":"user@example.com",
                      "contrasena":"Pasword1!"
                    }
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INACTIVE"))
            .andExpect(jsonPath("$.message").value("La cuenta se encuentra inactiva"));
    }

}
