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
class BookingLifecycleAndCustomerQueryE2ETest {

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
    void shouldFinalizeOwnBookingWhenSlotAlreadyEnded() throws Exception {
        Scenario scenario = setupScenario(2);
        forceAvailabilityToPast(scenario.firstAvailabilityId());

        HttpResult finalize = patch(
                "/api/v1/providers/me/bookings/" + scenario.firstBookingId() + "/finalization",
                scenario.providerToken());

        assertEquals(200, finalize.statusCode(), finalize.body());
        JsonNode payload = objectMapper.readTree(finalize.body());
        assertEquals("FINALIZADA", payload.path("data").path("bookingStatus").asText());

        String state = jdbcTemplate.queryForObject(
                "SELECT e.nombre_estado FROM tbl_reserva r JOIN tbl_estado e ON e.id_estado = r.id_estado_reserva WHERE r.id_reserva = ?",
                String.class,
                scenario.firstBookingId());
        assertEquals("FINALIZADA", state);
    }

    @Test
    void shouldCancelBookingAndRestoreAvailabilitySlots() throws Exception {
        Scenario scenario = setupScenario(1);

        HttpResult before = getAvailability(scenario.clientToken(), scenario.providerId(), scenario.serviceId(), scenario.bookingDate().toString());
        assertEquals(200, before.statusCode(), before.body());
        JsonNode beforeJson = objectMapper.readTree(before.body());
        assertEquals("No hay disponibilidad para reserva en la fecha seleccionada", beforeJson.path("message").asText());
        assertEquals(0, beforeJson.path("data").size());

        HttpResult cancel = patch(
                "/api/v1/bookings/" + scenario.firstBookingId() + "/cancellation",
                scenario.clientToken());
        assertEquals(200, cancel.statusCode(), cancel.body());

        HttpResult after = getAvailability(scenario.clientToken(), scenario.providerId(), scenario.serviceId(), scenario.bookingDate().toString());
        assertEquals(200, after.statusCode(), after.body());
        JsonNode afterJson = objectMapper.readTree(after.body());
        assertEquals("Consulta de horarios y cupos exitosa", afterJson.path("message").asText());
        assertEquals(1, afterJson.path("data").size());
    }

    @Test
    void shouldReturnOnlyClientOwnBookings() throws Exception {
        Scenario scenario = setupScenario(2);

        HttpResult cancel = patch(
                "/api/v1/bookings/" + scenario.firstBookingId() + "/cancellation",
                scenario.clientToken());
        assertEquals(200, cancel.statusCode(), cancel.body());

        HttpResult query = get("/api/v1/bookings/me", scenario.clientToken());
        assertEquals(200, query.statusCode(), query.body());

        JsonNode json = objectMapper.readTree(query.body());
        assertEquals("Consulta de reservas del cliente exitosa", json.path("message").asText());
        assertEquals(2, json.path("data").size());
        assertTrue(containsStatus(json.path("data"), "CANCELADA"));
        assertTrue(containsStatus(json.path("data"), "CREADA"));
    }

    private Scenario setupScenario(int capacity) throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);

        Long serviceId = createService(providerToken, "Servicio HU13-17-19", capacity);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        LocalDate bookingDate = nextDate(DayOfWeek.MONDAY);
        Long availabilityOne = createAvailability(providerToken, serviceId, bookingDate, "09:00:00", "10:00:00");
        Long availabilityTwo = createAvailability(providerToken, serviceId, bookingDate.plusDays(1), "10:00:00", "11:00:00");

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        Long bookingOne = createReservation(clientToken, providerId, serviceId, availabilityOne);
        Long bookingTwo = createReservation(clientToken, providerId, serviceId, availabilityTwo);

        return new Scenario(
                providerToken,
                clientToken,
                providerId,
                serviceId,
                bookingDate,
                availabilityOne,
                bookingOne,
                bookingTwo);
    }

    private void forceAvailabilityToPast(Long availabilityId) {
        jdbcTemplate.update(
                "UPDATE tbl_disponibilidad_servicio SET fecha_disponibilidad = CURRENT_DATE - INTERVAL '1 DAY', hora_inicio = '08:00:00', hora_fin = '09:00:00' WHERE id_disponibilidad_servicio = ?",
                availabilityId);
    }

    private boolean containsStatus(JsonNode bookings, String expectedStatus) {
        for (JsonNode item : bookings) {
            if (expectedStatus.equals(item.path("bookingStatus").asText())) {
                return true;
            }
        }
        return false;
    }

    private HttpResult getAvailability(String token, Long providerId, Long serviceId, String date) {
        return get(
                "/api/v1/providers/" + providerId + "/services/" + serviceId + "/availabilities?date=" + date,
                token);
    }

    private HttpResult get(String path, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult patch(String path, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Long createReservation(String token, Long providerId, Long serviceId, Long availabilityId) {
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
                throw new IllegalStateException("No se pudo crear reserva de prueba: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("data").path("bookingId").asLong();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Long createService(String token, String name, int capacity) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "nombre", name,
                    "descripcion", "Servicio para pruebas HU13/HU17/HU19",
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
                throw new IllegalStateException("No se pudo crear disponibilidad de prueba: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.path("data").path("idDisponibilidadServicio").asLong();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String authenticate(String email, String password) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "email", email,
                    "password", password
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
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
        RoleEntity role = roleRepository.findByNombreRol(roleName)
                .orElseThrow(() -> new IllegalStateException("Rol requerido no encontrado: " + roleName));

        StateEntity activeUserState = stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")
                .orElseThrow(() -> new IllegalStateException("Estado ACTIVA no encontrado para usuarios"));

        String email = roleName.toLowerCase() + ".hu131719." + UUID.randomUUID() + "@test.local";

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("Nombre " + roleName);
        user.setApellidosUsuario("Apellido " + roleName);
        user.setCorreoUsuario(email);
        user.setHashContrasenaUsuario(passwordEncoder.encode(rawPassword));
        user.setIntentosFallidosConsecutivos(0);
        user.setRol(role);
        user.setIdEstado(activeUserState.getIdEstado());

        userAccountRepository.save(user);
        createdEmails.add(email);
        return email;
    }

    private Long queryUserId(String email) {
        Optional<UserAccountEntity> user = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        return user.map(UserAccountEntity::getIdUsuario)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado para email: " + email));
    }

    private void defineGeneralSchedule(String token, String day, String start, String end) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "diaSemana", day,
                    "horaInicio", start,
                    "horaFin", end
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/schedules"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException("No se pudo crear horario general: " + response.body());
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private LocalDate nextDate(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now();
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }

    private record HttpResult(int statusCode, String body) {
    }

    private record Scenario(
            String providerToken,
            String clientToken,
            Long providerId,
            Long serviceId,
            LocalDate bookingDate,
            Long firstAvailabilityId,
            Long firstBookingId,
            Long secondBookingId
    ) {
    }
}
