package integrationtests.identityaccess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.AfterAll;
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
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static Long idTest;

    @BeforeEach
    void setUp() {

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("Camilo");
        user.setApellidosUsuario("Lopez");
        user.setCorreoUsuario("user@example.com");
        user.setHashContrasenaUsuario(passwordEncoder.encode("Password1!"));
        user.setIdEstado(2L);
        user.setIntentosFallidosConsecutivos(0);

        RoleEntity userRole = new RoleEntity();
        userRole.setIdRol(1L);
        userRole.setNombreRol("CLIENTE");
        user.setRol(userRole);

        UserAccountEntity createdUser = userAccountRepository.save(user);
        idTest = createdUser.getIdUsuario();

    }

    @AfterAll
    void cleanup() {
        userAccountRepository.deleteById(idTest);
    }

    @Test
    void shouldAuthenticateSuccessfully() throws Exception {
        
        mockMvc.perform(post("/api/v1/auth/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "correo":"cliente.ana@reservas.test",
                      "contrasena":"Cliente123!"
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

}
