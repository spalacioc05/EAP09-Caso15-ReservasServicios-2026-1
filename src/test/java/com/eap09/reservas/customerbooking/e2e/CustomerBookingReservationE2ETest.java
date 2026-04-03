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
class CustomerBookingReservationE2ETest {

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

            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE correo_usuario = ?", email);
        }

        createdEmails.clear();
    }

    @Test
    void shouldCreateReservationSuccessfully() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);

        Long serviceId = createService(providerToken, "Servicio HU16 Exito", 2);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        Long availabilityId = createAvailability(providerToken, serviceId, nextDate(DayOfWeek.MONDAY), "09:00:00", "10:00:00");

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");
        Long clientId = queryUserId(clientEmail);

        HttpResult response = createBooking(clientToken, providerId, serviceId, availabilityId);
        assertEquals(201, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("Reserva creada correctamente", json.path("message").asText());
        assertEquals("CREADA", json.path("data").path("bookingStatus").asText());
        assertEquals(providerId, json.path("data").path("providerId").asLong());
        assertEquals(serviceId, json.path("data").path("serviceId").asLong());
        assertEquals(availabilityId, json.path("data").path("availabilityId").asLong());
        assertEquals(clientId, json.path("data").path("customerId").asLong());

        Long createdStateId = stateId("tbl_reserva", "CREADA");
        Integer persisted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tbl_reserva WHERE id_disponibilidad_servicio = ? AND id_usuario_cliente = ? AND id_estado_reserva = ?",
                Integer.class,
                availabilityId,
                clientId,
                createdStateId);
        assertEquals(1, persisted);
    }

    @Test
    void shouldRejectWhenServiceIsInactive() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);

        Long serviceId = createService(providerToken, "Servicio HU16 Inactivo", 1);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        Long availabilityId = createAvailability(providerToken, serviceId, nextDate(DayOfWeek.MONDAY), "10:00:00", "11:00:00");
        setServiceInactive(serviceId);

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        HttpResult response = createBooking(clientToken, providerId, serviceId, availabilityId);
        assertEquals(409, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("SERVICE_NOT_AVAILABLE", json.path("errorCode").asText());
    }

    @Test
    void shouldRejectWhenProviderIsInactive() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);

        Long serviceId = createService(providerToken, "Servicio HU16 Proveedor", 1);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        Long availabilityId = createAvailability(providerToken, serviceId, nextDate(DayOfWeek.MONDAY), "12:00:00", "13:00:00");
        setProviderInactive(providerId);

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        HttpResult response = createBooking(clientToken, providerId, serviceId, availabilityId);
        assertEquals(409, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("PROVIDER_NOT_AVAILABLE", json.path("errorCode").asText());
    }

    @Test
    void shouldRejectWhenRequiredFieldsAreMissing() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long serviceId = createService(providerToken, "Servicio HU16 Requeridos", 1);

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        HttpResult response = createBookingRaw(clientToken, objectMapper.writeValueAsString(Map.of(
                "serviceId", serviceId,
                "availabilityId", 999999L
        )));

        assertEquals(400, response.statusCode(), response.body());
        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("REQUIRED_FIELDS_MISSING", json.path("errorCode").asText());
        assertEquals("Proveedor, servicio y franja son requeridos", json.path("message").asText());
    }

    @Test
    void shouldRejectWhenAvailabilityIsBlocked() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);

        Long serviceId = createService(providerToken, "Servicio HU16 Bloqueada", 1);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        Long availabilityId = createAvailability(providerToken, serviceId, nextDate(DayOfWeek.MONDAY), "14:00:00", "15:00:00");
        blockAvailability(providerToken, serviceId, availabilityId);

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        HttpResult response = createBooking(clientToken, providerId, serviceId, availabilityId);
        assertEquals(409, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("AVAILABILITY_NOT_RESERVABLE", json.path("errorCode").asText());
    }

    @Test
    void shouldRejectWhenCapacityIsExhausted() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long providerId = queryUserId(providerEmail);

        Long serviceId = createService(providerToken, "Servicio HU16 Cupo", 1);
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        Long availabilityId = createAvailability(providerToken, serviceId, nextDate(DayOfWeek.MONDAY), "16:00:00", "17:00:00");

        String firstClientEmail = createUser("CLIENTE", "Password1!");
        Long firstClientId = queryUserId(firstClientEmail);
        insertCreatedReservation(availabilityId, firstClientId);

        String secondClientEmail = createUser("CLIENTE", "Password1!");
        String secondClientToken = authenticate(secondClientEmail, "Password1!");

        HttpResult response = createBooking(secondClientToken, providerId, serviceId, availabilityId);
        assertEquals(409, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("AVAILABILITY_CAPACITY_EXHAUSTED", json.path("errorCode").asText());
    }

    @Test
    void shouldRejectWhenNotAuthenticated() {
        HttpResult response = createBooking(null, 1L, 1L, 1L);
        assertTrue(response.statusCode() == 401 || response.statusCode() == 403);
    }

    @Test
    void shouldRejectWhenAuthenticatedUserIsProvider() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");

        HttpResult response = createBooking(providerToken, 1L, 1L, 1L);
        assertEquals(403, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("CLIENT_ROLE_REQUIRED", json.path("errorCode").asText());
    }

    private HttpResult createBooking(String token, Long providerId, Long serviceId, Long availabilityId) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "providerId", providerId,
                    "serviceId", serviceId,
                    "availabilityId", availabilityId
            ));
            return createBookingRaw(token, body);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HttpResult createBookingRaw(String token, String body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/bookings"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Long createService(String token, String name, int capacity) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "nombre", name,
                    "descripcion", "Servicio para pruebas HU-16",
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
        Long activeStateId = stateId("tbl_usuario", "ACTIVA");

        String email = "hu16." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("HU16");
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

    private void setServiceInactive(Long serviceId) {
        Long inactiveStateId = stateId("tbl_servicio", "INACTIVO");
        jdbcTemplate.update("UPDATE tbl_servicio SET id_estado_servicio = ? WHERE id_servicio = ?", inactiveStateId, serviceId);
    }

    private void setProviderInactive(Long providerId) {
        Long inactiveStateId = stateId("tbl_usuario", "INACTIVA");
        jdbcTemplate.update("UPDATE tbl_usuario SET id_estado_usuario = ? WHERE id_usuario = ?", inactiveStateId, providerId);
    }

    private void insertCreatedReservation(Long availabilityId, Long clientUserId) {
        Long createdStateId = stateId("tbl_reserva", "CREADA");
        jdbcTemplate.update(
                "INSERT INTO tbl_reserva (id_disponibilidad_servicio, id_usuario_cliente, id_estado_reserva, fecha_creacion_reserva) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                availabilityId,
                clientUserId,
                createdStateId
        );
    }

    private Long stateId(String category, String stateName) {
        return stateRepository.findByCategoryAndStateName(category, stateName)
                .map(StateEntity::getIdEstado)
                .orElseThrow();
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

    private record HttpResult(int statusCode, String body) {
    }
}
