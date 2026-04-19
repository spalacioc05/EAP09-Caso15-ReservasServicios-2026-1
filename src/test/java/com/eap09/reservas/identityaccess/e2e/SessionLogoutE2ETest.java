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
import com.eap09.reservas.security.application.JwtService;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionLogoutE2ETest {

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

    @Autowired
    private JwtService jwtService;

    private final List<String> createdEmails = new ArrayList<>();

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
                       OR id_registro_afectado IN (
                        SELECT su.id_sesion_usuario
                        FROM tbl_sesion_usuario su
                        JOIN tbl_usuario u ON u.id_usuario = su.id_usuario
                        WHERE u.correo_usuario = ?
                    )
                    """, email, email, email);
            jdbcTemplate.update("DELETE FROM tbl_sesion_usuario WHERE id_usuario IN (SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?)", email);
            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE correo_usuario = ?", email);
        }
        createdEmails.clear();
    }

    @Test
    void shouldCloseSessionAndRejectTokenReuseOnProtectedEndpoint() throws Exception {
        String email = createUser("CLIENTE", "Password1!");
        Long userId = queryUserId(email);
        String token = authenticate(email, "Password1!");
        String tokenId = jwtService.extractTokenId(token);
        assertNotNull(tokenId);

        HttpResult protectedBefore = callProtectedStatus(token);
        assertEquals(HttpStatus.OK.value(), protectedBefore.statusCode(), protectedBefore.body());

        HttpResult logout = logout(token);
        assertEquals(HttpStatus.OK.value(), logout.statusCode(), logout.body());

        HttpResult protectedAfter = callProtectedStatus(token);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), protectedAfter.statusCode(), protectedAfter.body());

        Map<String, Object> sessionRow = jdbcTemplate.queryForMap("""
            SELECT su.id_sesion_usuario AS id_sesion,
                   e.nombre_estado AS estado,
                   su.fecha_cierre_sesion AS fecha_cierre
                FROM tbl_sesion_usuario su
                JOIN tbl_estado e ON e.id_estado = su.id_estado_sesion
                WHERE su.jti_token = CAST(? AS uuid)
                """, tokenId);
        Long sessionId = ((Number) sessionRow.get("id_sesion")).longValue();
        assertEquals("CERRADA", sessionRow.get("estado"));
        assertNotNull(sessionRow.get("fecha_cierre"));

        Integer logoutEvents = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
            JOIN tbl_tipo_registro tr ON tr.id_tipo_registro = ev.id_tipo_registro
                WHERE te.nombre_tipo_evento = 'CIERRE_SESION_USUARIO'
              AND tr.nombre_tipo_registro = 'tbl_sesion_usuario'
                  AND ev.id_usuario_responsable = ?
              AND ev.id_registro_afectado = ?
            """, Integer.class, userId, sessionId);
        assertNotNull(logoutEvents);
        assertTrue(logoutEvents > 0);
    }

    @Test
    void shouldReturnControlledConflictWhenSessionIsNoLongerActive() throws Exception {
        String email = createUser("CLIENTE", "Password1!");
        String token = authenticate(email, "Password1!");

        HttpResult firstLogout = logout(token);
        assertEquals(HttpStatus.OK.value(), firstLogout.statusCode(), firstLogout.body());

        HttpResult secondLogout = logout(token);
        assertEquals(HttpStatus.CONFLICT.value(), secondLogout.statusCode(), secondLogout.body());

        JsonNode error = objectMapper.readTree(secondLogout.body());
        assertEquals("SESSION_NOT_ACTIVE", error.path("errorCode").asText());
        assertEquals("No existe una sesion activa valida para cerrar", error.path("message").asText());
    }

    @Test
    void shouldReturnUnauthorizedWhenLogoutWithoutBearerToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/sessions/current"))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.statusCode(), response.body());

        JsonNode error = objectMapper.readTree(response.body());
        assertEquals("UNAUTHORIZED", error.path("errorCode").asText());
        assertEquals("Autenticacion requerida", error.path("message").asText());
    }

    private String createUser(String roleName, String rawPassword) {
        RoleEntity role = roleRepository.findByNombreRol(roleName).orElseThrow();
        Long activeStateId = stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")
                .map(StateEntity::getIdEstado)
                .orElseThrow();

        String email = "hu04." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("HU04");
        user.setApellidosUsuario("Logout");
        user.setCorreoUsuario(email);
        user.setHashContrasenaUsuario(passwordEncoder.encode(rawPassword));
        user.setRol(role);
        user.setIdEstado(activeStateId);
        user.setIntentosFallidosConsecutivos(0);
        user.setFechaFinRestriccionAcceso((LocalDateTime) null);

        userAccountRepository.save(user);
        return email;
    }

    private String authenticate(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("correo", email, "contrasena", password));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/sessions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != HttpStatus.OK.value()) {
            throw new IllegalStateException("No se pudo autenticar: " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        return json.path("data").path("accessToken").asText();
    }

    private HttpResult logout(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/auth/sessions/current"))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpResult(response.statusCode(), response.body());
    }

    private HttpResult callProtectedStatus(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/protected/status"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpResult(response.statusCode(), response.body());
    }

    private Long queryUserId(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?",
                Long.class,
                email);
    }

    private record HttpResult(int statusCode, String body) {
    }
}
