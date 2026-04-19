package com.eap09.reservas.provideroffer.e2e;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServiceRegistrationE2ETest {

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
    private Long activeUserStateId;

    @BeforeEach
    void setup() {
        activeUserStateId = stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")
                .map(StateEntity::getIdEstado)
                .orElseThrow();
    }

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            Long userId = jdbcTemplate.query("SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?", rs ->
                    rs.next() ? rs.getLong(1) : null, email);

            if (userId != null) {
                jdbcTemplate.update("DELETE FROM tbl_disponibilidad_servicio WHERE id_servicio IN (SELECT id_servicio FROM tbl_servicio WHERE id_usuario_proveedor = ?)", userId);
                jdbcTemplate.update("DELETE FROM tbl_servicio WHERE id_usuario_proveedor = ?", userId);
                jdbcTemplate.update("DELETE FROM tbl_evento WHERE id_usuario_responsable = ? OR id_registro_afectado = ?", userId, userId);
            }

            jdbcTemplate.update("DELETE FROM tbl_sesion_usuario WHERE id_usuario IN (SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?)", email);
            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE correo_usuario = ?", email);
        }

        createdEmails.clear();
    }

    @Test
    void shouldRegisterServiceSuccessfullyAndPersistAssociationStateAndEvent() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        HttpResult response = postService(token, "Masaje terapeutico", "Sesion de relajacion", 60, 2);
        assertEquals(201, response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("Servicio registrado correctamente", body.path("message").asText());
        assertEquals("Masaje terapeutico", body.path("data").path("nombre").asText());
        assertEquals("ACTIVO", body.path("data").path("estadoServicio").asText());

        Long userId = userAccountRepository.findByCorreoUsuarioIgnoreCase(providerEmail).orElseThrow().getIdUsuario();

        Map<String, Object> persisted = jdbcTemplate.queryForMap("""
                SELECT s.nombre_servicio, s.descripcion_servicio, s.duracion_minutos, s.capacidad_maxima_concurrente,
                       e.nombre_estado
                FROM tbl_servicio s
                JOIN tbl_estado e ON e.id_estado = s.id_estado_servicio
                WHERE s.id_usuario_proveedor = ?
                  AND s.nombre_servicio = 'Masaje terapeutico'
                """, userId);

        assertEquals("Masaje terapeutico", persisted.get("nombre_servicio"));
        assertEquals("Sesion de relajacion", persisted.get("descripcion_servicio"));
        assertEquals(60, persisted.get("duracion_minutos"));
        assertEquals(2, persisted.get("capacidad_maxima_concurrente"));
        assertEquals("ACTIVO", persisted.get("nombre_estado"));

        Integer eventCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                WHERE te.nombre_tipo_evento = 'REGISTRO_SERVICIO'
                  AND ev.id_usuario_responsable = ?
                """, Integer.class, userId);
        assertNotNull(eventCount);
        assertTrue(eventCount > 0);
    }

    @Test
    void shouldRejectMissingRequiredFields() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        HttpResult response = postServiceRaw(token, """
                {
                  "nombre":"",
                  "descripcion":"",
                  "duracionMinutos":null,
                  "capacidadMaximaConcurrente":null
                }
                """);

        assertEquals(400, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("VALIDATION_ERROR", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectDuplicateNameForSameProvider() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        HttpResult first = postService(token, "Masaje terapeutico", "Sesion de relajacion", 60, 2);
        assertEquals(201, first.statusCode(), first.body());

        HttpResult second = postService(token, "Masaje terapeutico", "Sesion duplicada", 30, 1);
        assertEquals(409, second.statusCode(), second.body());

        JsonNode body = objectMapper.readTree(second.body());
        assertEquals("SERVICE_NAME_ALREADY_EXISTS", body.path("errorCode").asText());
    }

    @Test
    void shouldAllowSameNameForDifferentProviders() {
        String providerOneEmail = createUser("PROVEEDOR", "Password1!");
        String providerTwoEmail = createUser("PROVEEDOR", "Password1!");
        String tokenOne = authenticate(providerOneEmail, "Password1!");
        String tokenTwo = authenticate(providerTwoEmail, "Password1!");

        HttpResult first = postService(tokenOne, "Masaje terapeutico", "Sesion uno", 60, 2);
        HttpResult second = postService(tokenTwo, "Masaje terapeutico", "Sesion dos", 45, 1);

        assertEquals(201, first.statusCode(), first.body());
        assertEquals(201, second.statusCode(), second.body());
    }

    @Test
    void shouldRejectInvalidDuration() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        HttpResult response = postService(token, "Masaje terapeutico", "Sesion de relajacion", 0, 2);
        assertEquals(400, response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("VALIDATION_ERROR", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectInvalidCapacity() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        HttpResult response = postService(token, "Masaje terapeutico", "Sesion de relajacion", 60, 0);
        assertEquals(400, response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("VALIDATION_ERROR", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsNotProvider() throws Exception {
        String clientEmail = createUser("CLIENTE", "Password1!");
        String token = authenticate(clientEmail, "Password1!");

        HttpResult response = postService(token, "Masaje terapeutico", "Sesion de relajacion", 60, 2);
        assertEquals(403, response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("PROVIDER_ROLE_REQUIRED", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectWhenMissingAuthentication() {
        HttpResult response = postService(null, "Masaje terapeutico", "Sesion de relajacion", 60, 2);
        assertTrue(response.statusCode() == 401 || response.statusCode() == 403);
    }

    private HttpResult postService(String token,
                                   String nombre,
                                   String descripcion,
                                   Integer duracionMinutos,
                                   Integer capacidadMaximaConcurrente) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "nombre", nombre,
                    "descripcion", descripcion,
                    "duracionMinutos", duracionMinutos,
                    "capacidadMaximaConcurrente", capacidadMaximaConcurrente
            ));
            return postServiceRaw(token, body);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult postServiceRaw(String token, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/services"))
                    .header("Content-Type", "application/json");

            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpRequest request = builder
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String authenticate(String email, String rawPassword) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "correo", email,
                    "contrasena", rawPassword
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/auth/sessions"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("No se pudo autenticar usuario de prueba: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("data").path("accessToken").asText();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String createUser(String roleName, String rawPassword) {
        RoleEntity role = roleRepository.findByNombreRol(roleName).orElseThrow();

        String email = "hu09." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("Servicio");
        user.setApellidosUsuario("Prueba");
        user.setCorreoUsuario(email);
        user.setHashContrasenaUsuario(passwordEncoder.encode(rawPassword));
        user.setRol(role);
        user.setIdEstado(activeUserStateId);
        user.setIntentosFallidosConsecutivos(0);
        user.setFechaFinRestriccionAcceso(null);

        userAccountRepository.save(user);
        return email;
    }

    private record HttpResult(int statusCode, String body) {
    }
}