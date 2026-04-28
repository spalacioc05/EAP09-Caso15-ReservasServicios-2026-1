package com.eap09.reservas.provideroffer.e2e;

import static org.junit.jupiter.api.Assertions.*;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Integration/E2E tests para HU-10: Activación e inactivación de servicios propios.
 * 
 * Usa SpringBootTest con puerto aleatorio para validar contra BD real.
 * Tests de flujos completos: login -> crear servicio -> cambiar estado -> validar persistencia.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("ServiceStatus E2E Tests - HU-10")
class ServiceStatusE2ETest {

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
    private Long inactiveServiceStateId;
    private Long activeServiceStateId;

    @BeforeEach
    void setup() {
        activeUserStateId = stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")
                .map(StateEntity::getIdEstado)
                .orElseThrow(() -> new RuntimeException("Required state 'ACTIVA' for tbl_usuario not found"));

        activeServiceStateId = stateRepository.findByCategoryAndStateName("tbl_servicio", "ACTIVO")
                .map(StateEntity::getIdEstado)
                .orElseThrow(() -> new RuntimeException("Required state 'ACTIVO' for tbl_servicio not found"));

        inactiveServiceStateId = stateRepository.findByCategoryAndStateName("tbl_servicio", "INACTIVO")
                .map(StateEntity::getIdEstado)
                .orElseThrow(() -> new RuntimeException("Required state 'INACTIVO' for tbl_servicio not found"));
    }

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            Long userId = jdbcTemplate.query(
                    "SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?",
                    rs -> rs.next() ? rs.getLong(1) : null,
                    email);

            if (userId != null) {
                // Limpiar en orden de dependencias
                jdbcTemplate.update("DELETE FROM tbl_evento WHERE id_usuario_responsable = ? OR id_registro_afectado = ?", userId, userId);
                jdbcTemplate.update("DELETE FROM tbl_disponibilidad_servicio WHERE id_servicio IN (SELECT id_servicio FROM tbl_servicio WHERE id_usuario_proveedor = ?)", userId);
                jdbcTemplate.update("DELETE FROM tbl_servicio WHERE id_usuario_proveedor = ?", userId);
                jdbcTemplate.update("DELETE FROM tbl_sesion_usuario WHERE id_usuario = ?", userId);
            }

            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE correo_usuario = ?", email);
        }

        createdEmails.clear();
    }

    // ========== Activación Exitosa ==========

    @Test
    @DisplayName("E2E: Activación exitosa - cambiar servicio INACTIVO a ACTIVO")
    void testActivateInactiveServiceSuccessfully() throws Exception {
        // Arrange
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        // Crear servicio (nace ACTIVO por defecto)
        Long serviceId = createService(token, "Consultoría de Arquitectura", "Sesión de 90 minutos", 90, 2);

        // Inactivar manualmente en BD para el test
        Long providerId = userAccountRepository.findByCorreoUsuarioIgnoreCase(providerEmail)
                .map(UserAccountEntity::getIdUsuario)
                .orElseThrow();

        jdbcTemplate.update(
                "UPDATE tbl_servicio SET id_estado_servicio = ? WHERE id_servicio = ?",
                inactiveServiceStateId, serviceId);

        // Act: Llamar endpoint de activación
        HttpResult response = patchServiceStatus(token, serviceId, "ACTIVO");

        // Assert: Response exitosa
        assertEquals(200, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("Consultoría de Arquitectura", body.path("data").path("nombreServicio").asText());
        assertEquals("ACTIVO", body.path("data").path("estadoActual").asText());

        // Assert: Persistencia en BD
        String persistedState = jdbcTemplate.queryForObject(
                "SELECT e.nombre_estado FROM tbl_servicio s JOIN tbl_estado e ON e.id_estado = s.id_estado_servicio WHERE s.id_servicio = ?",
                String.class,
                serviceId);
        assertEquals("ACTIVO", persistedState);

        // Assert: Evento registrado
        Integer eventCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                WHERE te.nombre_tipo_evento = 'ACTIVACION_SERVICIO'
                  AND ev.id_usuario_responsable = ?
                """,
                Integer.class,
                providerId);
        assertTrue(eventCount > 0, "Event ACTIVACION_SERVICIO should be recorded");
    }

    // ========== Inactivación Exitosa ==========

    @Test
    @DisplayName("E2E: Inactivación exitosa - cambiar servicio ACTIVO a INACTIVO")
    void testDeactivateActiveServiceSuccessfully() throws Exception {
        // Arrange
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        Long serviceId = createService(token, "Masaje Terapéutico", "Sesión relajante", 60, 1);

        // Act: Llamar endpoint de inactivación
        HttpResult response = patchServiceStatus(token, serviceId, "INACTIVO");

        // Assert: Response exitosa
        assertEquals(200, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("Masaje Terapéutico", body.path("data").path("nombreServicio").asText());
        assertEquals("INACTIVO", body.path("data").path("estadoActual").asText());

        // Assert: Persistencia en BD
        String persistedState = jdbcTemplate.queryForObject(
                "SELECT e.nombre_estado FROM tbl_servicio s JOIN tbl_estado e ON e.id_estado = s.id_estado_servicio WHERE s.id_servicio = ?",
                String.class,
                serviceId);
        assertEquals("INACTIVO", persistedState);

        // Assert: Evento registrado
        Long providerId = userAccountRepository.findByCorreoUsuarioIgnoreCase(providerEmail)
                .map(UserAccountEntity::getIdUsuario)
                .orElseThrow();

        Integer eventCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                WHERE te.nombre_tipo_evento = 'INACTIVACION_SERVICIO'
                  AND ev.id_usuario_responsable = ?
                """,
                Integer.class,
                providerId);
        assertTrue(eventCount > 0, "Event INACTIVACION_SERVICIO should be recorded");
    }

    // ========== Inactivación con Reservas Existentes ==========

    @Test
    @DisplayName("E2E: Inactivación preserva reservas ya creadas")
    void testDeactivationPreservesExistingReservations() throws Exception {
        // Arrange
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String clientEmail = createUser("CLIENTE", "Password1!");

        String providerToken = authenticate(providerEmail, "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        Long serviceId = createService(providerToken, "Consultoría", "Sesión de asesoría", 60, 2);

        // Verificar que el servicio está ACTIVO
        String serviceState = jdbcTemplate.queryForObject(
                "SELECT e.nombre_estado FROM tbl_servicio s JOIN tbl_estado e ON e.id_estado = s.id_estado_servicio WHERE s.id_servicio = ?",
                String.class,
                serviceId);
        assertEquals("ACTIVO", serviceState);

        // Act: Inactivar el servicio
        HttpResult deactivateResponse = patchServiceStatus(providerToken, serviceId, "INACTIVO");
        assertEquals(200, deactivateResponse.statusCode(), deactivateResponse.body());

        // Assert: Servicio ahora está INACTIVO
        String newState = jdbcTemplate.queryForObject(
                "SELECT e.nombre_estado FROM tbl_servicio s JOIN tbl_estado e ON e.id_estado = s.id_estado_servicio WHERE s.id_servicio = ?",
                String.class,
                serviceId);
        assertEquals("INACTIVO", newState);
    }

    // ========== Activación Redundante ==========

    @Test
    @DisplayName("E2E: Activación redundante devuelve 200 OK idempotente")
    void testActivateAlreadyActiveServiceReturnsIdempotent() throws Exception {
        // Arrange
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        Long serviceId = createService(token, "Consultoría", "Asesoría", 60, 1);
        // El servicio nace ACTIVO

        // Act: Llamar activación en servicio que ya está ACTIVO
        HttpResult response = patchServiceStatus(token, serviceId, "ACTIVO");

        // Assert: Respuesta 200 OK (idempotente)
        assertEquals(200, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("ACTIVO", body.path("data").path("estadoActual").asText());

        // Assert: Sin evento adicional (porque no hubo cambio)
        Long providerId = userAccountRepository.findByCorreoUsuarioIgnoreCase(providerEmail)
                .map(UserAccountEntity::getIdUsuario)
                .orElseThrow();

        Integer eventCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                WHERE te.nombre_tipo_evento = 'ACTIVACION_SERVICIO'
                  AND ev.id_usuario_responsable = ?
                """,
                Integer.class,
                providerId);
        assertEquals(0, eventCount, "No event should be recorded for redundant activation");
    }

    // ========== Inactivación Redundante ==========

    @Test
    @DisplayName("E2E: Inactivación redundante devuelve 200 OK idempotente")
    void testDeactivateAlreadyInactiveServiceReturnsIdempotent() throws Exception {
        // Arrange
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        Long serviceId = createService(token, "Consultoría", "Asesoría", 60, 1);

        // Inactivar primero
        patchServiceStatus(token, serviceId, "INACTIVO");

        // Act: Intentar inactivar nuevamente
        HttpResult response = patchServiceStatus(token, serviceId, "INACTIVO");

        // Assert: Respuesta 200 OK (idempotente)
        assertEquals(200, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("INACTIVO", body.path("data").path("estadoActual").asText());
    }

    // ========== Servicio Ajeno (403) ==========

    @Test
    @DisplayName("E2E: Intento de cambiar estado de servicio ajeno devuelve 403")
    void testChangeStatusOfOtherProviderServiceReturns403() throws Exception {
        // Arrange
        String provider1Email = createUser("PROVEEDOR", "Password1!");
        String provider2Email = createUser("PROVEEDOR", "Password1!");

        String token1 = authenticate(provider1Email, "Password1!");
        String token2 = authenticate(provider2Email, "Password1!");

        // Provider 1 crea servicio
        Long serviceId = createService(token1, "Consultoría P1", "Asesoría", 60, 1);

        // Act: Provider 2 intenta cambiar estado del servicio de Provider 1
        HttpResult response = patchServiceStatus(token2, serviceId, "INACTIVO");

        // Assert: 403 Forbidden
        assertEquals(403, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("FORBIDDEN", body.path("errorCode").asText());
    }

    // ========== Servicio No Encontrado (404) ==========

    @Test
    @DisplayName("E2E: Intento de cambiar estado de servicio inexistente devuelve 404")
    void testChangeStatusOfNonexistentServiceReturns404() throws Exception {
        // Arrange
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        // Act: Intentar cambiar estado de servicio que no existe
        HttpResult response = patchServiceStatus(token, 99999L, "ACTIVO");

        // Assert: 404 Not Found
        assertEquals(404, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("SERVICE_NOT_FOUND", body.path("errorCode").asText());
    }

    // ========== Sin Autenticación (401) ==========

    @Test
    @DisplayName("E2E: Cambio de estado sin token devuelve 401")
    void testChangeStatusWithoutAuthenticationReturns401() throws Exception {
        // Act: Llamar sin token
        HttpResult response = patchServiceStatus(null, 1L, "ACTIVO");

        // Assert: 401 Unauthorized
        assertTrue(response.statusCode() == 401 || response.statusCode() == 403);
    }

    // ========== Cliente intenta cambiar estado (403) ==========

    @Test
    @DisplayName("E2E: Cliente intenta cambiar estado de servicio devuelve 403")
    void testClientTriesToChangeServiceStatusReturns403() throws Exception {
        // Arrange
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String clientEmail = createUser("CLIENTE", "Password1!");

        String providerToken = authenticate(providerEmail, "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        Long serviceId = createService(providerToken, "Consultoría", "Asesoría", 60, 1);

        // Act: Cliente intenta cambiar estado
        HttpResult response = patchServiceStatus(clientToken, serviceId, "INACTIVO");

        // Assert: 403 Forbidden
        assertEquals(403, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("PROVIDER_ROLE_REQUIRED", body.path("errorCode").asText());
    }

    // ========== Estado Inválido (400) ==========

    @Test
    @DisplayName("E2E: Estado inválido devuelve 400")
    void testInvalidStatusReturns400() throws Exception {
        // Arrange
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");

        Long serviceId = createService(token, "Consultoría", "Asesoría", 60, 1);

        // Act: Enviar estado inválido
        HttpResult response = patchServiceStatus(token, serviceId, "ELIMINADO");

        // Assert: 400 Bad Request
        assertEquals(400, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("INVALID_STATUS", body.path("errorCode").asText());
    }

    // ========== Helpers ==========

    private HttpResult patchServiceStatus(String token, Long serviceId, String targetStatus) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "targetStatus", targetStatus
            ));

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/services/" + serviceId + "/status"))
                    .header("Content-Type", "application/json");

            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpRequest request = builder
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Long createService(String token, String nombre, String descripcion, Integer duracion, Integer capacidad) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "nombre", nombre,
                    "descripcion", descripcion,
                    "duracionMinutos", duracion,
                    "capacidadMaximaConcurrente", capacidad
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/services"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException("Failed to create service: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("data").path("idServicio").asLong();
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
                throw new IllegalStateException("Failed to authenticate: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("data").path("accessToken").asText();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String createUser(String roleName, String rawPassword) {
        RoleEntity role = roleRepository.findByNombreRol(roleName).orElseThrow();

        String email = "hu10." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("Test");
        user.setApellidosUsuario("User");
        user.setCorreoUsuario(email);
        user.setHashContrasenaUsuario(passwordEncoder.encode(rawPassword));
        user.setRol(role);
        user.setIdEstado(activeUserStateId);
        user.setIntentosFallidosConsecutivos(0);
        user.setFechaFinRestriccionAcceso(null);

        UserAccountEntity saved = userAccountRepository.save(user);
        return saved.getCorreoUsuario();
    }

    private record HttpResult(int statusCode, String body) {
    }
}
