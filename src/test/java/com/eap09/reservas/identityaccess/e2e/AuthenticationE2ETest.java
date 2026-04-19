package com.eap09.reservas.identityaccess.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eap09.reservas.identityaccess.domain.RoleEntity;
import com.eap09.reservas.identityaccess.domain.StateEntity;
import com.eap09.reservas.identityaccess.domain.UserAccountEntity;
import com.eap09.reservas.identityaccess.infrastructure.RoleRepository;
import com.eap09.reservas.identityaccess.infrastructure.StateRepository;
import com.eap09.reservas.identityaccess.infrastructure.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationE2ETest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<String> createdEmails = new ArrayList<>();

    private Long activeStateId;
    private Long inactiveStateId;

    @BeforeEach
    void setupStates() {
        activeStateId = stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")
                .map(StateEntity::getIdEstado)
                .orElseThrow();
        inactiveStateId = stateRepository.findByCategoryAndStateName("tbl_usuario", "INACTIVA")
                .map(StateEntity::getIdEstado)
                .orElseThrow();
    }

    @AfterEach
    void cleanupTestData() {
        for (String email : createdEmails) {
            jdbcTemplate.update("""
                    DELETE FROM tbl_evento
                    WHERE id_usuario_responsable IN (
                        SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?
                    )
                       OR id_registro_afectado IN (
                        SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?
                    )
                    """, email, email);
            jdbcTemplate.update("DELETE FROM tbl_sesion_usuario WHERE id_usuario IN (SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?)", email);
            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE correo_usuario = ?", email);
        }
        createdEmails.clear();
    }

    @Test
    void shouldAuthenticateCustomerSuccessfully() throws Exception {
        String email = createUser("CLIENTE", activeStateId, null, 0, "Password1!");

        HttpResult response = postAuth(email, "Password1!");
        assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("Autenticacion exitosa", body.path("message").asText());
        assertEquals("CLIENTE", body.path("data").path("role").asText());
        assertEquals("Bearer", body.path("data").path("tokenType").asText());
        assertTrue(body.path("data").path("expiresIn").asLong() > 0);
        assertTrue(body.path("data").path("accessToken").asText().length() > 20);

        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(email).orElseThrow();
        assertEquals(0, user.getIntentosFallidosConsecutivos());
        assertEquals(null, user.getFechaFinRestriccionAcceso());
    }

    @Test
    void shouldAuthenticateProviderSuccessfully() throws Exception {
        String email = createUser("PROVEEDOR", activeStateId, null, 0, "Password1!");

        HttpResult response = postAuth(email, "Password1!");
        assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("PROVEEDOR", body.path("data").path("role").asText());
    }

    @Test
    void shouldAuthenticateAdministratorSuccessfully() throws Exception {
        String email = createUser("ADMINISTRADOR", activeStateId, null, 0, "Password1!");

        HttpResult response = postAuth(email, "Password1!");
        assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("ADMINISTRADOR", body.path("data").path("role").asText());
    }

    @Test
    void shouldRejectUnknownEmail() throws Exception {
        HttpResult response = postAuth("no-existe." + UUID.randomUUID() + "@test.local", "Password1!");
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.statusCode());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("INVALID_CREDENTIALS", body.path("errorCode").asText());
        assertEquals("Credenciales no validas", body.path("message").asText());
    }

    @Test
    void shouldRejectEmptyFields() throws Exception {
        HttpResult response = postAuthRaw("""
            {
              "correo":"",
              "contrasena":""
            }
            """);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("VALIDATION_ERROR", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectWrongPasswordAndIncreaseFailedAttempts() throws Exception {
        String email = createUser("CLIENTE", activeStateId, null, 0, "Password1!");

        HttpResult response = postAuth(email, "WrongPassword1!");
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.statusCode());

        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(email).orElseThrow();
        assertEquals(1, user.getIntentosFallidosConsecutivos());
    }

    @Test
    void shouldApplyTemporaryRestrictionAfterMultipleFailedAttempts() throws Exception {
        String email = createUser("CLIENTE", activeStateId, null, 4, "Password1!");

        HttpResult response = postAuth(email, "WrongPassword1!");
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.statusCode());

        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(email).orElseThrow();
        assertEquals(5, user.getIntentosFallidosConsecutivos());
        assertNotNull(user.getFechaFinRestriccionAcceso());
        assertTrue(user.getFechaFinRestriccionAcceso().isAfter(LocalDateTime.now()));

        Integer restrictionEvents = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                WHERE te.nombre_tipo_evento = 'APLICACION_RESTRICCION_ACCESO'
                  AND ev.id_usuario_responsable = ?
                """, Integer.class, user.getIdUsuario());
        assertNotNull(restrictionEvents);
        assertTrue(restrictionEvents > 0);
    }

    @Test
    void shouldRejectRestrictedAccount() throws Exception {
        String email = createUser("CLIENTE", activeStateId, LocalDateTime.now().plusMinutes(10), 0, "Password1!");

        HttpResult response = postAuth(email, "Password1!");
        assertEquals(HttpStatus.FORBIDDEN.value(), response.statusCode());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("ACCESS_TEMPORARILY_RESTRICTED", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectInactiveAccount() throws Exception {
        String email = createUser("CLIENTE", inactiveStateId, null, 0, "Password1!");

        HttpResult response = postAuth(email, "Password1!");
        assertEquals(HttpStatus.FORBIDDEN.value(), response.statusCode());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("ACCOUNT_INACTIVE", body.path("errorCode").asText());
    }

    @Test
    void shouldRecordAuthenticationEventOnSuccess() {
        String email = createUser("CLIENTE", activeStateId, null, 0, "Password1!");

        HttpResult response = postAuth(email, "Password1!");
        assertEquals(HttpStatus.OK.value(), response.statusCode(), response.body());

        UserAccountEntity user = userAccountRepository.findByCorreoUsuarioIgnoreCase(email).orElseThrow();
        Integer authEvents = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                WHERE te.nombre_tipo_evento = 'AUTENTICACION_USUARIO'
                  AND ev.id_usuario_responsable = ?
                """, Integer.class, user.getIdUsuario());
        assertNotNull(authEvents);
        assertTrue(authEvents > 0);
    }

    private HttpResult postAuth(String email, String password) {
        Map<String, Object> request = Map.of(
                "correo", email,
                "contrasena", password
        );

        try {
            String body = objectMapper.writeValueAsString(request);
            return postAuthRaw(body);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult postAuthRaw(String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/auth/sessions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String createUser(String roleName,
                              Long stateId,
                              LocalDateTime restrictionEnd,
                              int failedAttempts,
                              String rawPassword) {
        RoleEntity role = roleRepository.findByNombreRol(roleName).orElseThrow();

        String email = "hu03." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("Auth");
        user.setApellidosUsuario("Test");
        user.setCorreoUsuario(email);
        user.setHashContrasenaUsuario(passwordEncoder.encode(rawPassword));
        user.setRol(role);
        user.setIdEstado(stateId);
        user.setIntentosFallidosConsecutivos(failedAttempts);
        user.setFechaFinRestriccionAcceso(restrictionEnd);

        userAccountRepository.save(user);
        return email;
    }

    private record HttpResult(int statusCode, String body) {
    }
}
