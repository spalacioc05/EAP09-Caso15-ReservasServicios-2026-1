package com.eap09.reservas.customerbooking.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class ProviderBookingQueryE2ETest {

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
            Long userId = jdbcTemplate.query("SELECT id_usuario FROM tbl_usuario WHERE correo_usuario = ?", rs ->
                    rs.next() ? rs.getLong(1) : null, email);

            if (userId != null) {
                jdbcTemplate.update("DELETE FROM tbl_reserva WHERE id_usuario_cliente = ?", userId);
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
    void shouldReturnOnlyOwnBookingsWithoutFilters() throws Exception {
        TestScenario scenario = setupScenario();

        HttpResult response = getOwnBookings(scenario.providerToken(), null, null, null);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("Consulta operativa de reservas exitosa", json.path("message").asText());
        assertEquals(2, json.path("data").size());
        assertTrue(allBookingsBelongToService(json.path("data"), scenario.ownServiceId()));
    }

    @Test
    void shouldFilterByDate() throws Exception {
        TestScenario scenario = setupScenario();

        HttpResult response = getOwnBookings(scenario.providerToken(), scenario.bookingDate().toString(), null, null);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals(1, json.path("data").size());
        assertEquals(scenario.bookingDate().toString(), json.path("data").get(0).path("slotDate").asText());
    }

    @Test
    void shouldFilterByStatus() throws Exception {
        TestScenario scenario = setupScenario();

        HttpResult response = getOwnBookings(scenario.providerToken(), null, "CREADA", null);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertTrue(json.path("data").size() >= 1);
        assertTrue(allWithStatus(json.path("data"), "CREADA"));
    }

    @Test
    void shouldFilterByOwnService() throws Exception {
        TestScenario scenario = setupScenario();

        HttpResult response = getOwnBookings(scenario.providerToken(), null, null, scenario.ownServiceId().toString());
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertTrue(json.path("data").size() >= 1);
        assertTrue(allBookingsBelongToService(json.path("data"), scenario.ownServiceId()));
    }

    @Test
    void shouldFilterByDateStatusAndServiceCombination() throws Exception {
        TestScenario scenario = setupScenario();

        HttpResult response = getOwnBookings(
                scenario.providerToken(),
                scenario.bookingDate().toString(),
                "CREADA",
                scenario.ownServiceId().toString());
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals(1, json.path("data").size());
        JsonNode row = json.path("data").get(0);
        assertEquals(scenario.ownServiceId(), row.path("serviceId").asLong());
        assertEquals("CREADA", row.path("bookingStatus").asText());
        assertEquals(scenario.bookingDate().toString(), row.path("slotDate").asText());
    }

    @Test
    void shouldRejectForeignServiceFilter() throws Exception {
        TestScenario scenario = setupScenario();

        HttpResult response = getOwnBookings(scenario.providerToken(), null, null, scenario.foreignServiceId().toString());
        assertEquals(403, response.statusCode(), response.body());
        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("FORBIDDEN", json.path("errorCode").asText());
    }

    @Test
    void shouldReturnFilteredNoResultsMessage() throws Exception {
        TestScenario scenario = setupScenario();

        HttpResult response = getOwnBookings(scenario.providerToken(), "2099-01-01", null, null);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("No existen reservas que cumplan con los filtros aplicados", json.path("message").asText());
        assertEquals(0, json.path("data").size());
    }

    @Test
    void shouldReturnNoReservationsRegisteredMessage() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");

        HttpResult response = getOwnBookings(providerToken, null, null, null);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("No existen reservas registradas para sus servicios", json.path("message").asText());
        assertEquals(0, json.path("data").size());
    }

    @Test
    void shouldRejectWithoutToken() {
        HttpResult response = getOwnBookings(null, null, null, null);
        assertEquals(401, response.statusCode(), response.body());
    }

    @Test
    void shouldNotModifyReservationDataAsSideEffect() throws Exception {
        TestScenario scenario = setupScenario();

        Integer before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tbl_reserva", Integer.class);
        HttpResult response = getOwnBookings(scenario.providerToken(), null, null, null);
        assertEquals(200, response.statusCode(), response.body());
        Integer after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tbl_reserva", Integer.class);
        assertEquals(before, after);
    }

    private TestScenario setupScenario() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);

        Long ownServiceId = createService(providerToken, "Servicio HU12 Propio", 2);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        LocalDate bookingDate = nextDate(DayOfWeek.MONDAY);
        Long ownAvailabilityId = createAvailability(providerToken, ownServiceId, bookingDate, "09:00:00", "10:00:00");
        Long ownAvailabilityIdTwo = createAvailability(providerToken, ownServiceId, bookingDate.plusDays(1), "09:00:00", "10:00:00");

        String foreignProviderEmail = createUser("PROVEEDOR", "Password1!");
        String foreignProviderToken = authenticate(foreignProviderEmail, "Password1!");
        Long foreignServiceId = createService(foreignProviderToken, "Servicio HU12 Ajeno", 1);
        defineGeneralSchedule(foreignProviderToken, "LUNES", "08:00:00", "18:00:00");
        Long foreignAvailabilityId = createAvailability(foreignProviderToken, foreignServiceId, bookingDate, "11:00:00", "12:00:00");

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        createReservation(clientToken, providerId, ownServiceId, ownAvailabilityId);
        createReservation(clientToken, providerId, ownServiceId, ownAvailabilityIdTwo);
        createReservation(clientToken, queryUserId(foreignProviderEmail), foreignServiceId, foreignAvailabilityId);

        return new TestScenario(providerToken, ownServiceId, foreignServiceId, bookingDate);
    }

    private HttpResult getOwnBookings(String token, String date, String status, String serviceId) {
        try {
            StringBuilder uri = new StringBuilder("http://localhost:" + port + "/api/v1/providers/me/bookings");
            String sep = "?";

            if (date != null) {
                uri.append(sep).append("date=").append(date);
                sep = "&";
            }

            if (status != null) {
                uri.append(sep).append("status=").append(status);
                sep = "&";
            }

            if (serviceId != null) {
                uri.append(sep).append("serviceId=").append(serviceId);
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(uri.toString()));
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> response = httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void createReservation(String token, Long providerId, Long serviceId, Long availabilityId) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "providerId", providerId,
                    "serviceId", serviceId,
                    "availabilityId", availabilityId
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/bookings"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException("No se pudo crear reserva de prueba HU-12: " + response.body());
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Long createService(String token, String name, int capacity) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "nombre", name,
                    "descripcion", "Servicio para pruebas HU-12",
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
        Long activeStateId = stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")
                .map(StateEntity::getIdEstado)
                .orElseThrow();

        String email = "hu12." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("HU12");
        user.setApellidosUsuario("Prueba");
        user.setCorreoUsuario(email);
        user.setHashContrasenaUsuario(passwordEncoder.encode(rawPassword));
        user.setRol(role);
        user.setIdEstado(activeStateId);
        user.setIntentosFallidosConsecutivos(0);
        user.setFechaFinRestriccionAcceso(null);

        userAccountRepository.save(user);
        return email;
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

    private boolean allBookingsBelongToService(JsonNode rows, Long serviceId) {
        for (JsonNode row : rows) {
            if (row.path("serviceId").asLong() != serviceId) {
                return false;
            }
        }
        return true;
    }

    private boolean allWithStatus(JsonNode rows, String status) {
        for (JsonNode row : rows) {
            if (!status.equals(row.path("bookingStatus").asText())) {
                return false;
            }
        }
        return true;
    }

    private record HttpResult(int statusCode, String body) {
    }

    private record TestScenario(String providerToken, Long ownServiceId, Long foreignServiceId, LocalDate bookingDate) {
    }
}