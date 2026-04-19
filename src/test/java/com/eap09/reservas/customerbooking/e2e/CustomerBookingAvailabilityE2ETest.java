package com.eap09.reservas.customerbooking.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerBookingAvailabilityE2ETest {

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

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            Long userId = queryUserId(email);

            if (userId != null) {
                jdbcTemplate.update("DELETE FROM tbl_reserva WHERE id_disponibilidad_servicio IN (SELECT ds.id_disponibilidad_servicio FROM tbl_disponibilidad_servicio ds JOIN tbl_servicio s ON s.id_servicio = ds.id_servicio WHERE s.id_usuario_proveedor = ?)", userId);
                jdbcTemplate.update("DELETE FROM tbl_disponibilidad_servicio WHERE id_servicio IN (SELECT id_servicio FROM tbl_servicio WHERE id_usuario_proveedor = ?)", userId);
                jdbcTemplate.update("DELETE FROM tbl_servicio WHERE id_usuario_proveedor = ?", userId);
                jdbcTemplate.update("DELETE FROM tbl_horario_general_proveedor WHERE id_usuario_proveedor = ?", userId);
                jdbcTemplate.update("DELETE FROM tbl_evento WHERE id_usuario_responsable = ? OR id_registro_afectado = ?", userId, userId);
                jdbcTemplate.update("DELETE FROM tbl_reserva WHERE id_usuario_cliente = ?", userId);
            }

            jdbcTemplate.update("DELETE FROM tbl_sesion_usuario WHERE id_usuario IN (SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?)", email);
            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE correo_usuario = ?", email);
        }

        createdEmails.clear();
    }

    @Test
    void shouldReturnAvailableSlotsWithRemainingCapacityForClient() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);

        Long serviceId = createService(providerToken, "Servicio HU15 Exitoso", 2);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        LocalDate date = nextDate(DayOfWeek.MONDAY);
        Long availabilityId = createAvailability(providerToken, serviceId, date, "09:00:00", "10:00:00");

        String bookedClientEmail = createUser("CLIENTE", "Password1!");
        Long bookedClientId = queryUserId(bookedClientEmail);
        insertReservation(availabilityId, bookedClientId);

        String queryClientEmail = createUser("CLIENTE", "Password1!");
        String queryClientToken = authenticate(queryClientEmail, "Password1!");

        HttpResult response = getAvailabilities(providerId, serviceId, date, queryClientToken);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("Consulta de horarios y cupos exitosa", json.path("message").asText());
        assertTrue(json.path("data").isArray());
        assertEquals(1, json.path("data").size());
        assertEquals(availabilityId, json.path("data").get(0).path("availabilityId").asLong());
        assertEquals(1, json.path("data").get(0).path("remainingSlots").asLong());
    }

    @Test
    void shouldReturnInvalidRelationMessageWhenServiceDoesNotBelongToProvider() throws Exception {
        String providerOneEmail = createUser("PROVEEDOR", "Password1!");
        String providerOneToken = authenticate(providerOneEmail, "Password1!");
        Long serviceId = createService(providerOneToken, "Servicio HU15 Relacion", 1);

        String providerTwoEmail = createUser("PROVEEDOR", "Password1!");
        Long providerTwoId = queryUserId(providerTwoEmail);

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        HttpResult response = getAvailabilities(providerTwoId, serviceId, nextDate(DayOfWeek.MONDAY), clientToken);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("No existe disponibilidad para la seleccion realizada", json.path("message").asText());
        assertEquals(0, json.path("data").size());
    }

    @Test
    void shouldReturnNoAvailabilityForSelectedDate() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);

        Long serviceId = createService(providerToken, "Servicio HU15 Sin Fecha", 1);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        createAvailability(providerToken, serviceId, nextDate(DayOfWeek.MONDAY), "11:00:00", "12:00:00");

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        LocalDate differentDate = nextDate(DayOfWeek.TUESDAY);
        HttpResult response = getAvailabilities(providerId, serviceId, differentDate, clientToken);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("No hay disponibilidad para reserva en la fecha seleccionada", json.path("message").asText());
        assertEquals(0, json.path("data").size());
    }

    @Test
    void shouldExcludeBlockedOrFullSlots() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);

        Long serviceId = createService(providerToken, "Servicio HU15 Filtrado", 1);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        LocalDate date = nextDate(DayOfWeek.MONDAY);

        Long blockedAvailabilityId = createAvailability(providerToken, serviceId, date, "08:00:00", "09:00:00");
        blockAvailability(providerToken, serviceId, blockedAvailabilityId);

        Long fullAvailabilityId = createAvailability(providerToken, serviceId, date, "10:00:00", "11:00:00");
        String bookedClientEmail = createUser("CLIENTE", "Password1!");
        Long bookedClientId = queryUserId(bookedClientEmail);
        insertReservation(fullAvailabilityId, bookedClientId);

        String queryClientEmail = createUser("CLIENTE", "Password1!");
        String queryClientToken = authenticate(queryClientEmail, "Password1!");

        HttpResult response = getAvailabilities(providerId, serviceId, date, queryClientToken);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("No hay disponibilidad para reserva en la fecha seleccionada", json.path("message").asText());
        assertFalse(containsAvailabilityId(json.path("data"), blockedAvailabilityId));
        assertFalse(containsAvailabilityId(json.path("data"), fullAvailabilityId));
    }

    @Test
    void shouldRejectWhenDateIsMissing() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);
        Long serviceId = createService(providerToken, "Servicio HU15 Requeridos", 1);

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        HttpResult response = getAvailabilitiesWithoutDate(providerId, serviceId, clientToken);
        assertEquals(400, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("REQUIRED_FIELDS_MISSING", json.path("errorCode").asText());
    }

    @Test
    void shouldRejectWhenNotAuthenticated() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);
        Long serviceId = createService(providerToken, "Servicio HU15 NoAuth", 1);

        HttpResult response = getAvailabilities(providerId, serviceId, nextDate(DayOfWeek.MONDAY), null);
        assertTrue(response.statusCode() == 401 || response.statusCode() == 403);
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsProvider() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);
        Long serviceId = createService(providerToken, "Servicio HU15 Rol", 1);

        HttpResult response = getAvailabilities(providerId, serviceId, nextDate(DayOfWeek.MONDAY), providerToken);
        assertEquals(403, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("CLIENT_ROLE_REQUIRED", json.path("errorCode").asText());
    }

    private HttpResult getAvailabilities(Long providerId, Long serviceId, LocalDate date, String token) {
        try {
            String url = "http://localhost:" + port + "/api/v1/providers/" + providerId + "/services/" + serviceId
                    + "/availabilities?date=" + date;

            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> response = httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult getAvailabilitiesWithoutDate(Long providerId, Long serviceId, String token) {
        try {
            String url = "http://localhost:" + port + "/api/v1/providers/" + providerId + "/services/" + serviceId
                    + "/availabilities";

            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> response = httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Long createService(String token, String name, int capacity) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "nombre", name,
                    "descripcion", "Servicio para pruebas HU-15",
                    "duracionMinutos", 60,
                    "capacidadMaximaConcurrente", capacity
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

    private Long createAvailability(String token, Long serviceId, LocalDate date, String start, String end) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "fecha", date.toString(),
                    "horaInicio", start,
                    "horaFin", end
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/services/" + serviceId + "/availabilities"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException("No se pudo crear disponibilidad: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("data").path("idDisponibilidad").asLong();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void blockAvailability(String token, Long serviceId, Long availabilityId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/services/" + serviceId + "/availabilities/" + availabilityId + "/block"))
                    .header("Authorization", "Bearer " + token)
                    .method("PATCH", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("No se pudo bloquear disponibilidad: " + response.body());
            }
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
                throw new IllegalStateException("No se pudo definir horario general: " + response.body());
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
        Long activeStateId = stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")
                .map(StateEntity::getIdEstado)
                .orElseThrow();

        String email = "hu15." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("HU15");
        user.setApellidosUsuario("Test");
        user.setCorreoUsuario(email);
        user.setHashContrasenaUsuario(passwordEncoder.encode(rawPassword));
        user.setRol(role);
        user.setIdEstado(activeStateId);
        user.setIntentosFallidosConsecutivos(0);
        user.setFechaFinRestriccionAcceso(null);

        userAccountRepository.save(user);
        return email;
    }

    private void insertReservation(Long availabilityId, Long clientUserId) {
        Long createdStateId = stateRepository.findByCategoryAndStateName("tbl_reserva", "CREADA")
                .map(StateEntity::getIdEstado)
                .orElseThrow();

        jdbcTemplate.update(
                "INSERT INTO tbl_reserva (id_disponibilidad_servicio, id_usuario_cliente, id_estado_reserva, fecha_creacion_reserva) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                availabilityId,
                clientUserId,
                createdStateId
        );
    }

    private Long queryUserId(String email) {
        return jdbcTemplate.query("SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?", rs ->
                rs.next() ? rs.getLong(1) : null, email);
    }

    private LocalDate nextDate(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now();
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }

    private boolean containsAvailabilityId(JsonNode data, Long availabilityId) {
        for (JsonNode node : data) {
            if (availabilityId.equals(node.path("availabilityId").asLong())) {
                return true;
            }
        }

        return false;
    }

    private record HttpResult(int statusCode, String body) {
    }
}
