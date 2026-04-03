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
class CustomerBookingOfferE2ETest {

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
                jdbcTemplate.update("DELETE FROM tbl_reserva WHERE id_disponibilidad_servicio IN (SELECT ds.id_disponibilidad_servicio FROM tbl_disponibilidad_servicio ds JOIN tbl_servicio s ON s.id_servicio = ds.id_servicio WHERE s.id_usuario_proveedor = ?)", userId);
                jdbcTemplate.update("DELETE FROM tbl_disponibilidad_servicio WHERE id_servicio IN (SELECT id_servicio FROM tbl_servicio WHERE id_usuario_proveedor = ?)", userId);
                jdbcTemplate.update("DELETE FROM tbl_servicio WHERE id_usuario_proveedor = ?", userId);
                jdbcTemplate.update("DELETE FROM tbl_horario_general_proveedor WHERE id_usuario_proveedor = ?", userId);
                jdbcTemplate.update("DELETE FROM tbl_evento WHERE id_usuario_responsable = ? OR id_registro_afectado = ?", userId, userId);
            }

            jdbcTemplate.update("DELETE FROM tbl_usuario WHERE correo_usuario = ?", email);
        }

        createdEmails.clear();
    }

    @Test
    void shouldReturnAvailableOffersForClient() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long serviceId = createService(providerToken, "Servicio HU14");
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        createAvailability(providerToken, serviceId, nextDate(DayOfWeek.MONDAY), "09:00:00", "10:00:00");

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        HttpResult response = getOffers(clientToken);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("Consulta de oferta exitosa", json.path("message").asText());
        assertTrue(containsServiceName(json.path("data"), "Servicio HU14"));
    }

    @Test
    void shouldReturnNoOffersMessageWhenNothingReservable() throws Exception {
        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        HttpResult response = getOffers(clientToken);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertTrue(json.path("message").asText().equals("Consulta de oferta exitosa")
            || json.path("message").asText().equals("No hay oferta disponible para mostrar"));
        assertTrue(json.path("data").isArray());
    }

    @Test
    void shouldExcludeBlockedAvailabilityFromOffer() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");
        Long serviceId = createService(providerToken, "Servicio Bloqueado HU14");
        defineGeneralSchedule(providerToken, "LUNES", "08:00:00", "18:00:00");
        Long availabilityId = createAvailability(providerToken, serviceId, nextDate(DayOfWeek.MONDAY), "11:00:00", "12:00:00");
        blockAvailability(providerToken, serviceId, availabilityId);

        String clientEmail = createUser("CLIENTE", "Password1!");
        String clientToken = authenticate(clientEmail, "Password1!");

        HttpResult response = getOffers(clientToken);
        assertEquals(200, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertTrue(!containsServiceName(json.path("data"), "Servicio Bloqueado HU14"));
    }

    @Test
    void shouldRejectWhenNotAuthenticated() {
        HttpResult response = getOffers(null);
        assertTrue(response.statusCode() == 401 || response.statusCode() == 403);
    }

    @Test
    void shouldRejectWhenUserIsNotClient() throws Exception {
        String providerEmail = createUser("PROVEEDOR", "Password1!");
        String providerToken = authenticate(providerEmail, "Password1!");

        HttpResult response = getOffers(providerToken);
        assertEquals(403, response.statusCode(), response.body());

        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("CLIENT_ROLE_REQUIRED", json.path("errorCode").asText());
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

    private Long createService(String token, String name) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "nombre", name,
                    "descripcion", "Servicio para pruebas HU-14",
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
        Long activeStateId = stateRepository.findByCategoryAndStateName("tbl_usuario", "ACTIVA")
                .map(StateEntity::getIdEstado)
                .orElseThrow();

        String email = "hu14." + roleName.toLowerCase() + "." + UUID.randomUUID().toString().replace("-", "") + "@test.local";
        createdEmails.add(email);

        Optional<UserAccountEntity> existing = userAccountRepository.findByCorreoUsuarioIgnoreCase(email);
        existing.ifPresent(userAccountRepository::delete);

        UserAccountEntity user = new UserAccountEntity();
        user.setNombresUsuario("Oferta");
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

    private record HttpResult(int statusCode, String body) {
    }

    private boolean containsServiceName(JsonNode offers, String expectedServiceName) {
        for (JsonNode offer : offers) {
            if (expectedServiceName.equals(offer.path("serviceName").asText())) {
                return true;
            }
        }

        return false;
    }
}
