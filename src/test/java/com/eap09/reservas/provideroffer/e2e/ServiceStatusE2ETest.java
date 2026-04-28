package com.eap09.reservas.provideroffer.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    void shouldInactivateOwnServiceKeepCreatedReservationsAndExcludeItFromOffer() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = userAccountRepository.findByCorreoUsuarioIgnoreCase(providerEmail).orElseThrow().getIdUsuario();

        Long serviceId = createService(providerToken, "Servicio HU10 Inactivar", 2);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        Long availabilityId = createAvailability(providerToken, serviceId, nextDate(DayOfWeek.MONDAY), "09:00:00", "10:00:00");

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");
        Long clientId = userAccountRepository.findByCorreoUsuarioIgnoreCase(clientEmail).orElseThrow().getIdUsuario();

        HttpResult bookingResponse = createReservation(clientToken, providerId, serviceId, availabilityId);
        assertEquals(201, bookingResponse.statusCode(), bookingResponse.body());

        Integer reservationCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tbl_reserva WHERE id_usuario_cliente = ?",
                Integer.class,
                clientId);
        assertEquals(1, reservationCountBefore);

        HttpResult patchResponse = patchServiceStatus(providerToken, serviceId, "INACTIVO");
        assertEquals(200, patchResponse.statusCode(), patchResponse.body());

        JsonNode patchJson = objectMapper.readTree(patchResponse.body());
        assertEquals("Servicio inactivado correctamente", patchJson.path("message").asText());
        assertEquals("INACTIVO", patchJson.path("data").path("estadoServicio").asText());

        String persistedState = jdbcTemplate.queryForObject("""
                SELECT e.nombre_estado
                FROM tbl_servicio s
                JOIN tbl_estado e ON e.id_estado = s.id_estado_servicio
                WHERE s.id_servicio = ?
                """, String.class, serviceId);
        assertEquals("INACTIVO", persistedState);

        Integer reservationCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tbl_reserva WHERE id_usuario_cliente = ?",
                Integer.class,
                clientId);
        assertEquals(reservationCountBefore, reservationCountAfter);

        String reservationState = jdbcTemplate.queryForObject("""
                SELECT e.nombre_estado
                FROM tbl_reserva r
                JOIN tbl_estado e ON e.id_estado = r.id_estado_reserva
                WHERE r.id_usuario_cliente = ?
                """, String.class, clientId);
        assertEquals("CREADA", reservationState);

        HttpResult offersResponse = getOffers(clientToken);
        assertEquals(200, offersResponse.statusCode(), offersResponse.body());
        JsonNode offersJson = objectMapper.readTree(offersResponse.body());
        assertFalse(containsServiceName(offersJson.path("data"), "Servicio HU10 Inactivar"));

        HttpResult newBookingAttempt = createReservation(clientToken, providerId, serviceId, availabilityId);
        assertEquals(409, newBookingAttempt.statusCode(), newBookingAttempt.body());
        JsonNode newBookingJson = objectMapper.readTree(newBookingAttempt.body());
        assertEquals("SERVICE_NOT_AVAILABLE", newBookingJson.path("errorCode").asText());
    }

    @Test
    void shouldActivateOwnInactiveServiceSuccessfully() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long serviceId = createService(providerToken, "Servicio HU10 Activar", 1);
        Long inactiveStateId = stateRepository.findByCategoryAndStateName("tbl_servicio", "INACTIVO")
                .map(StateEntity::getIdEstado)
                .orElseThrow();

        jdbcTemplate.update("UPDATE tbl_servicio SET id_estado_servicio = ? WHERE id_servicio = ?", inactiveStateId, serviceId);

        HttpResult patchResponse = patchServiceStatus(providerToken, serviceId, "ACTIVO");
        assertEquals(200, patchResponse.statusCode(), patchResponse.body());

        JsonNode patchJson = objectMapper.readTree(patchResponse.body());
        assertEquals("Servicio activado correctamente", patchJson.path("message").asText());
        assertEquals("ACTIVO", patchJson.path("data").path("estadoServicio").asText());

        String persistedState = jdbcTemplate.queryForObject("""
                SELECT e.nombre_estado
                FROM tbl_servicio s
                JOIN tbl_estado e ON e.id_estado = s.id_estado_servicio
                WHERE s.id_servicio = ?
                """, String.class, serviceId);
        assertEquals("ACTIVO", persistedState);
    }

    @Test
    void shouldRejectForeignServiceStatusChange() throws Exception {
        String ownerEmail = createUser("PROVEEDOR", "Password1!");
        String ownerToken = authenticate(ownerEmail, "Password1!");
        Long foreignServiceId = createService(ownerToken, "Servicio Ajeno HU10", 1);

        String attackerEmail = createUser("PROVEEDOR", "Password1!");
        String attackerToken = authenticate(attackerEmail, "Password1!");

        HttpResult patchResponse = patchServiceStatus(attackerToken, foreignServiceId, "INACTIVO");
        assertEquals(403, patchResponse.statusCode(), patchResponse.body());

        JsonNode json = objectMapper.readTree(patchResponse.body());
        assertEquals("FORBIDDEN", json.path("errorCode").asText());
    }

    @Test
    void shouldRejectWithoutToken() {
        HttpResult patchResponse = patchServiceStatus(null, 999999L, "ACTIVO");
        assertEquals(401, patchResponse.statusCode(), patchResponse.body());
    }

    @Test
    void shouldReturnConflictWhenStateAlreadyRequested() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long serviceId = createService(providerToken, "Servicio Redundante HU10", 1);

        HttpResult patchResponse = patchServiceStatus(providerToken, serviceId, "ACTIVO");
        assertEquals(409, patchResponse.statusCode(), patchResponse.body());

        JsonNode json = objectMapper.readTree(patchResponse.body());
        assertEquals("SERVICE_STATUS_ALREADY_SET", json.path("errorCode").asText());
    }

    private HttpResult patchServiceStatus(String token, Long serviceId, String targetStatus) {
        try {
            String body = objectMapper.writeValueAsString(Map.of("targetStatus", targetStatus));

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/providers/me/services/" + serviceId + "/status"))
                    .header("Content-Type", "application/json");

            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpRequest request = builder.method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult createReservation(String token, Long providerId, Long serviceId, Long availabilityId) {
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
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult getOffers(String token) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/offers"));

            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> response = httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
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

    private Long createService(String token, String name, Integer capacity) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "nombre", name,
                    "descripcion", "Servicio para pruebas HU-10",
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

        String email = "hu10." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("HU10");
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

    private LocalDate nextDate(DayOfWeek dayOfWeek) {
        LocalDate date = LocalDate.now();
        while (date.getDayOfWeek() != dayOfWeek) {
            date = date.plusDays(1);
        }
        return date;
    }

    private boolean containsServiceName(JsonNode offers, String expectedServiceName) {
        for (JsonNode offer : offers) {
            if (expectedServiceName.equals(offer.path("serviceName").asText())) {
                return true;
            }
        }
        return false;
    }

    private record HttpResult(int statusCode, String body) {
    }
}