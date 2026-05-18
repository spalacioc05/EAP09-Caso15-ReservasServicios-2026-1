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

        UserAccountEntity user = new UserAccountEntity();
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

        UserAccountEntity createdUser = userAccountRepository.save(user);
    }
        
    void cleanup() {
        String truncate_sql = """
            TRUNCATE TABLE tbl_evento, tbl_sesion_usuario,
            tbl_usuario RESTART IDENTITY CASCADE
        """;
        jdbcTemplate.update(truncate_sql);
        //userSessionRepository.deleteByIdUsuario(idTest);
        //userAccountRepository.deleteById(idTest);
    }

    @Test
    void shouldAuthenticateSuccessfully() throws Exception {
        
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

}
