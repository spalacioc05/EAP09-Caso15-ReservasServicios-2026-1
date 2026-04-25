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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
class ProfileUpdateE2ETest {

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

    private final List<Long> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanupTestData() {
        Set<Long> ids = new HashSet<>(createdUserIds);
        for (Long userId : ids) {
            jdbcTemplate.update("DELETE FROM tbl_evento WHERE id_usuario_responsable = ? OR id_registro_afectado = ?", userId, userId);
            jdbcTemplate.update("DELETE FROM tbl_sesion_usuario WHERE id_usuario = ?", userId);
            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE id_usuario = ?", userId);
        }
        createdUserIds.clear();
    }

    @Test
    void shouldUpdateOwnProfileAndKeepLoginFlowWorking() throws Exception {
        TestUser user = createUser("CLIENTE", "Password1!");
        String newEmail = "hu05.updated." + UUID.randomUUID().toString().replace("-", "") + "@test.local";

        HttpResult loginBefore = authenticate(user.email(), user.password());
        assertEquals(HttpStatus.OK.value(), loginBefore.statusCode(), loginBefore.body());
        String token = accessToken(loginBefore.body());

        HttpResult updateResponse = patchProfile(token, Map.of(
                "nombres", "HU05 Nombre",
                "correo", newEmail
        ));

        assertEquals(HttpStatus.OK.value(), updateResponse.statusCode(), updateResponse.body());
        JsonNode updateBody = objectMapper.readTree(updateResponse.body());
        assertEquals("Perfil actualizado correctamente", updateBody.path("message").asText());
        assertEquals("HU05 Nombre", updateBody.path("data").path("nombres").asText());
        assertEquals(newEmail, updateBody.path("data").path("correo").asText());

        UserAccountEntity updated = userAccountRepository.findById(user.id()).orElseThrow();
        assertEquals("HU05 Nombre", updated.getNombresUsuario());
        assertEquals(newEmail, updated.getCorreoUsuario());

        HttpResult loginWithNewEmail = authenticate(newEmail, user.password());
        assertEquals(HttpStatus.OK.value(), loginWithNewEmail.statusCode(), loginWithNewEmail.body());

        HttpResult loginWithOldEmail = authenticate(user.email(), user.password());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), loginWithOldEmail.statusCode(), loginWithOldEmail.body());

        Integer successEvents = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                JOIN tbl_estado es ON es.id_estado = ev.id_estado_evento
                JOIN tbl_categoria_estado ce ON ce.id_categoria_estado = es.id_categoria_estado
                WHERE te.nombre_tipo_evento = 'ACTUALIZACION_PERFIL_USUARIO'
                  AND ce.nombre_categoria_estado = 'tbl_evento'
                  AND es.nombre_estado = 'EXITO'
                  AND ev.id_usuario_responsable = ?
                """, Integer.class, user.id());
        assertNotNull(successEvents);
        assertTrue(successEvents > 0);
    }

    @Test
    void shouldRejectUpdateWhenEmailBelongsToAnotherAccount() throws Exception {
        TestUser sourceUser = createUser("CLIENTE", "Password1!");
        TestUser otherUser = createUser("CLIENTE", "Password1!");

        HttpResult login = authenticate(sourceUser.email(), sourceUser.password());
        String token = accessToken(login.body());

        HttpResult updateResponse = patchProfile(token, Map.of("correo", otherUser.email()));
        assertEquals(HttpStatus.CONFLICT.value(), updateResponse.statusCode(), updateResponse.body());

        JsonNode body = objectMapper.readTree(updateResponse.body());
        assertEquals("EMAIL_ALREADY_REGISTERED", body.path("errorCode").asText());

        UserAccountEntity sourceAfter = userAccountRepository.findById(sourceUser.id()).orElseThrow();
        assertEquals(sourceUser.email(), sourceAfter.getCorreoUsuario());
    }

    @Test
    void shouldRejectUpdateWithoutToken() throws Exception {
        HttpResult response = patchProfileWithoutToken(Map.of("nombres", "Sin Token"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("UNAUTHORIZED", body.path("errorCode").asText());
    }

    @Test
    void shouldReturnConflictWhenNoRealChangesAreSent() throws Exception {
        TestUser user = createUser("CLIENTE", "Password1!");

        HttpResult login = authenticate(user.email(), user.password());
        String token = accessToken(login.body());

        UserAccountEntity current = userAccountRepository.findById(user.id()).orElseThrow();
        HttpResult response = patchProfile(token, Map.of(
                "nombres", current.getNombresUsuario(),
                "apellidos", current.getApellidosUsuario(),
                "correo", current.getCorreoUsuario()
        ));

        assertEquals(HttpStatus.CONFLICT.value(), response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("PROFILE_NO_CHANGES", body.path("errorCode").asText());
    }

    private TestUser createUser(String roleName, String rawPassword) {
        RoleEntity role = roleRepository.findByNombreRol(roleName).orElseThrow();
        Long activeStateId = stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")
                .map(StateEntity::getIdEstado)
                .orElseThrow();

        String email = "hu05." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("HU05");
        user.setApellidosUsuario("Profile");
        user.setCorreoUsuario(email);
        user.setHashContrasenaUsuario(passwordEncoder.encode(rawPassword));
        user.setRol(role);
        user.setIdEstado(activeStateId);
        user.setIntentosFallidosConsecutivos(0);
        user.setFechaFinRestriccionAcceso(null);

        UserAccountEntity saved = userAccountRepository.save(user);
        createdUserIds.add(saved.getIdUsuario());
        return new TestUser(saved.getIdUsuario(), email, rawPassword);
    }

    private HttpResult authenticate(String email, String password) {
        return post("/api/v1/auth/sessions", Map.of(
                "correo", email,
                "contrasena", password
        ), null);
    }

    private String accessToken(String responseBody) throws Exception {
        JsonNode body = objectMapper.readTree(responseBody);
        return body.path("data").path("accessToken").asText();
    }

    private HttpResult patchProfile(String token, Map<String, Object> payload) {
        return requestWithBody("PATCH", "/api/v1/users/me/profile", payload, token);
    }

    private HttpResult patchProfileWithoutToken(Map<String, Object> payload) {
        return requestWithBody("PATCH", "/api/v1/users/me/profile", payload, null);
    }

    private HttpResult post(String path, Map<String, Object> payload, String bearerToken) {
        return requestWithBody("POST", path, payload, bearerToken);
    }

    private HttpResult requestWithBody(String method, String path, Map<String, Object> payload, String bearerToken) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));

            if (bearerToken != null) {
                builder.header("Authorization", "Bearer " + bearerToken);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record HttpResult(int statusCode, String body) {
    }

    private record TestUser(Long id, String email, String password) {
    }
}