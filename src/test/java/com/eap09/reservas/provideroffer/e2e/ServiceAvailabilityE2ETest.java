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
import java.time.DayOfWeek;
import java.time.LocalDate;
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
class ServiceAvailabilityE2ETest {

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
                jdbcTemplate.update("DELETE FROM tbl_reserva WHERE id_disponibilidad_servicio IN (SELECT ds.id_disponibilidad_servicio FROM tbl_disponibilidad_servicio ds JOIN tbl_servicio s ON s.id_servicio = ds.id_servicio WHERE s.id_usuario_proveedor = ?)", userId);
                jdbcTemplate.update("DELETE FROM tbl_disponibilidad_servicio WHERE id_servicio IN (SELECT id_servicio FROM tbl_servicio WHERE id_usuario_proveedor = ?)", userId);
                jdbcTemplate.update("DELETE FROM tbl_servicio WHERE id_usuario_proveedor = ?", userId);
                jdbcTemplate.update("DELETE FROM tbl_horario_general_proveedor WHERE id_usuario_proveedor = ?", userId);
                jdbcTemplate.update("DELETE FROM tbl_evento WHERE id_usuario_responsable = ? OR id_registro_afectado = ?", userId, userId);
            }

            jdbcTemplate.update("DELETE FROM tbl_sesion_usuario WHERE id_usuario IN (SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?)", email);
            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE correo_usuario = ?", email);
        }

        createdEmails.clear();
    }

    @Test
    void shouldCreateAvailabilitySuccessfullyAndPersistEnabledStateAndEvent() throws Exception {
        LocalDate targetDate = nextDate(DayOfWeek.MONDAY);
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(providerEmail, "Password1!");
        Long serviceId = createService(token, "Service A");
        defineGeneralSchedule(token, "LUNES", "08:00:00", "18:00:00");

        HttpResult response = createAvailability(token, serviceId, targetDate, "09:00:00", "10:00:00");
        assertEquals(201, response.statusCode(), response.body());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("Disponibilidad creada correctamente", body.path("message").asText());
        Long availabilityId = body.path("data").path("idDisponibilidad").asLong();
        assertTrue(availabilityId > 0);

        Map<String, Object> persisted = jdbcTemplate.queryForMap("""
                SELECT ds.id_disponibilidad_servicio, ds.id_servicio, ds.fecha_disponibilidad,
                       ds.hora_inicio::text AS hora_inicio, ds.hora_fin::text AS hora_fin,
                       e.nombre_estado
                FROM tbl_disponibilidad_servicio ds
                JOIN tbl_estado e ON e.id_estado = ds.id_estado_disponibilidad
                WHERE ds.id_disponibilidad_servicio = ?
                """, availabilityId);

        assertEquals(serviceId, ((Number) persisted.get("id_servicio")).longValue());
        assertEquals("HABILITADA", persisted.get("nombre_estado"));

        Long providerUserId = userAccountRepository.findByCorreoUsuarioIgnoreCase(providerEmail).orElseThrow().getIdUsuario();
        Integer eventCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                WHERE te.nombre_tipo_evento = 'CREACION_DISPONIBILIDAD'
                  AND ev.id_usuario_responsable = ?
                """, Integer.class, providerUserId);
        assertNotNull(eventCount);
        assertTrue(eventCount > 0);
    }

    @Test
    void shouldRejectCreateWithMissingFields() throws Exception {
        Long serviceId = setupProviderServiceAndScheduleFor(DayOfWeek.MONDAY);
        String token = currentToken;

        HttpResult response = createAvailabilityRaw(token, serviceId, """
                {
                  "fecha":null,
                  "horaInicio":null,
                  "horaFin":null
                }
                """);

        assertEquals(400, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("VALIDATION_ERROR", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectCreateWhenEndBeforeStart() throws Exception {
        Long serviceId = setupProviderServiceAndScheduleFor(DayOfWeek.MONDAY);
        String token = currentToken;

        HttpResult response = createAvailability(token, serviceId, nextDate(DayOfWeek.MONDAY), "11:00:00", "10:00:00");
        assertEquals(400, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("INVALID_TIME_RANGE", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectCreateOutsideGeneralSchedule() throws Exception {
        Long serviceId = setupProviderServiceAndScheduleFor(DayOfWeek.MONDAY);
        String token = currentToken;

        HttpResult response = createAvailability(token, serviceId, nextDate(DayOfWeek.MONDAY), "07:00:00", "09:00:00");
        assertEquals(400, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("OUTSIDE_GENERAL_SCHEDULE", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectCreateWhenOverlapping() throws Exception {
        Long serviceId = setupProviderServiceAndScheduleFor(DayOfWeek.MONDAY);
        String token = currentToken;
        LocalDate date = nextDate(DayOfWeek.MONDAY);

        HttpResult first = createAvailability(token, serviceId, date, "09:00:00", "10:00:00");
        assertEquals(201, first.statusCode(), first.body());

        HttpResult second = createAvailability(token, serviceId, date, "09:30:00", "10:30:00");
        assertEquals(409, second.statusCode(), second.body());
        JsonNode body = objectMapper.readTree(second.body());
        assertEquals("AVAILABILITY_OVERLAP", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectCreateWhenServiceBelongsToAnotherProvider() throws Exception {
        String providerAEmail = createUser("PROVEEDOR", "Password1!");
        String providerAToken = authenticate(providerAEmail, "Password1!");
        Long providerAServiceId = createService(providerAToken, "Service A");
        defineGeneralSchedule(providerAToken, "LUNES", "08:00:00", "18:00:00");

        String providerBEmail = createUser("PROVEEDOR", "Password1!");
        String providerBToken = authenticate(providerBEmail, "Password1!");
        defineGeneralSchedule(providerBToken, "LUNES", "08:00:00", "18:00:00");

        HttpResult response = createAvailability(providerBToken, providerAServiceId, nextDate(DayOfWeek.MONDAY), "09:00:00", "10:00:00");
        assertEquals(403, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("FORBIDDEN", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectCreateWhenNotProvider() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long serviceId = createService(providerToken, "Service A");
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        HttpResult response = createAvailability(clientToken, serviceId, nextDate(DayOfWeek.MONDAY), "09:00:00", "10:00:00");
        assertEquals(403, response.statusCode(), response.body());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("PROVIDER_ROLE_REQUIRED", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectCreateWhenMissingAuthentication() {
        HttpResult response = createAvailabilityRaw(null, 1L, """
                {
                  "fecha":"2026-04-06",
                  "horaInicio":"09:00:00",
                  "horaFin":"10:00:00"
                }
                """);

        assertTrue(response.statusCode() == 401 || response.statusCode() == 403);
    }

    @Test
    void shouldBlockAvailabilitySuccessfullyAndPersistBlockedStateAndEvent() throws Exception {
        Long serviceId = setupProviderServiceAndScheduleFor(DayOfWeek.MONDAY);
        String token = currentToken;
        String email = currentEmail;

        HttpResult create = createAvailability(token, serviceId, nextDate(DayOfWeek.MONDAY), "09:00:00", "10:00:00");
        assertEquals(201, create.statusCode(), create.body());
        Long availabilityId = objectMapper.readTree(create.body()).path("data").path("idDisponibilidad").asLong();

        HttpResult block = blockAvailability(token, serviceId, availabilityId);
        assertEquals(200, block.statusCode(), block.body());
        JsonNode body = objectMapper.readTree(block.body());
        assertEquals("Disponibilidad bloqueada correctamente", body.path("message").asText());
        assertEquals("BLOQUEADA", body.path("data").path("estadoDisponibilidad").asText());

        String state = jdbcTemplate.queryForObject("""
                SELECT e.nombre_estado
                FROM tbl_disponibilidad_servicio ds
                JOIN tbl_estado e ON e.id_estado = ds.id_estado_disponibilidad
                WHERE ds.id_disponibilidad_servicio = ?
                """, String.class, availabilityId);
        assertEquals("BLOQUEADA", state);

        Long providerUserId = userAccountRepository.findByCorreoUsuarioIgnoreCase(email).orElseThrow().getIdUsuario();
        Integer eventCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tbl_evento ev
                JOIN tbl_tipo_evento te ON te.id_tipo_evento = ev.id_tipo_evento
                WHERE te.nombre_tipo_evento = 'BLOQUEO_DISPONIBILIDAD'
                  AND ev.id_usuario_responsable = ?
                """, Integer.class, providerUserId);
        assertNotNull(eventCount);
        assertTrue(eventCount > 0);
    }

    @Test
    void shouldRejectBlockWhenAvailabilityNotFound() throws Exception {
        Long serviceId = setupProviderServiceAndScheduleFor(DayOfWeek.MONDAY);

        HttpResult block = blockAvailability(currentToken, serviceId, 999999L);
        assertEquals(404, block.statusCode(), block.body());
        JsonNode body = objectMapper.readTree(block.body());
        assertEquals("AVAILABILITY_NOT_FOUND", body.path("errorCode").asText());
    }

    @Test
    void shouldRejectBlockWhenAvailabilityBelongsToAnotherProvider() throws Exception {
        String providerAEmail = createUser("PROVEEDOR", "Password1!");
        String providerAToken = authenticate(providerAEmail, "Password1!");
        Long providerAServiceId = createService(providerAToken, "Service A");
        defineGeneralSchedule(providerAToken, "LUNES", "08:00:00", "18:00:00");
        HttpResult created = createAvailability(providerAToken, providerAServiceId, nextDate(DayOfWeek.MONDAY), "09:00:00", "10:00:00");
        Long availabilityId = objectMapper.readTree(created.body()).path("data").path("idDisponibilidad").asLong();

        String providerBEmail = createUser("PROVEEDOR", "Password1!");
        String providerBToken = authenticate(providerBEmail, "Password1!");
        Long providerBServiceId = createService(providerBToken, "Service B");
        defineGeneralSchedule(providerBToken, "LUNES", "08:00:00", "18:00:00");

        HttpResult block = blockAvailability(providerBToken, providerBServiceId, availabilityId);
        assertEquals(403, block.statusCode(), block.body());
        JsonNode body = objectMapper.readTree(block.body());
        assertEquals("FORBIDDEN", body.path("errorCode").asText());
    }

    private String currentToken;
    private String currentEmail;

    private Long setupProviderServiceAndScheduleFor(DayOfWeek day) {
        String dayName = toSpanish(day);
        String email = createUser("PROVEEDOR", "Password1!");
        String token = authenticate(email, "Password1!");
        Long serviceId = createService(token, "Service A");
        defineGeneralSchedule(token, dayName, "08:00:00", "18:00:00");
        currentToken = token;
        currentEmail = email;
        return serviceId;
    }

    private HttpResult createAvailability(String token, Long serviceId, LocalDate date, String start, String end) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "fecha", date.toString(),
                    "horaInicio", start,
                    "horaFin", end
            ));
            return createAvailabilityRaw(token, serviceId, body);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult createAvailabilityRaw(String token, Long serviceId, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/services/" + serviceId + "/availabilities"))
                    .header("Content-Type", "application/json");

            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpRequest request = builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult blockAvailability(String token, Long serviceId, Long availabilityId) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/services/" + serviceId + "/availabilities/" + availabilityId + "/block"));

            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpRequest request = builder.method("PATCH", HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Long createService(String token, String name) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "nombre", name,
                    "descripcion", "Servicio para disponibilidad",
                    "duracionMinutos", 60,
                    "capacidadMaximaConcurrente", 2
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/services"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException("No se pudo crear servicio de prueba: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("data").path("idServicio").asLong();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void defineGeneralSchedule(String token, String dayName, String start, String end) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("horaInicio", start, "horaFin", end));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/general-schedule/" + dayName))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("No se pudo definir horario general de prueba: " + response.body());
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String authenticate(String email, String rawPassword) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("correo", email, "contrasena", rawPassword));

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

        String email = "hu11." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("Disponibilidad");
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

    private LocalDate nextDate(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now();
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }

    private String toSpanish(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }

    private record HttpResult(int statusCode, String body) {
    }
}